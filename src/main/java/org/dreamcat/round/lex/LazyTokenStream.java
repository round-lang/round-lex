package org.dreamcat.round.lex;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.dreamcat.common.Pair;
import org.dreamcat.common.text.NumberSearcher;
import org.dreamcat.common.text.StringSearcher;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StreamUtil;
import org.dreamcat.common.util.StringUtil;

/**
 * @author Jerry Will
 * @version 2022-10-01
 */
@Getter
class LazyTokenStream implements TokenStream {

    private final Lexer lexer;
    private final String expression;

    private final LexConfig config;
    private final int len; // expression length
    private int i = -1; // expression offset
    private final List<TokenInfo> tokenInfos = new ArrayList<>();
    private int offset; // index of next token to return
    private int mark; // marked offset

    public LazyTokenStream(Lexer lexer, String expression) {
        this.lexer = lexer;
        this.expression = expression;
        this.config = lexer.config;
        this.len = expression.length();
    }

    @Override
    public boolean hasNext() {
        return offset < tokenInfos.size() ||
                readNextToken();
    }

    @Override
    public Token next() {
        if (!hasNext()) return throwWrongSyntax();
        return tokenInfos.get(offset++).getToken();
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }

    @Override
    public Token previous() {
        if (!hasPrevious()) return throwWrongSyntax();
        return tokenInfos.get(--offset).getToken();
    }

    @Override
    public TokenInfo get() {
        if (offset < 0 || offset >= tokenInfos.size()) return throwWrongSyntax();
        return tokenInfos.get(offset);
    }

    @Override
    public void mark() {
        mark = offset;
    }

    @Override
    public void reset() {
        offset = mark;
        mark = 0; // clear mark
    }

    @Override
    public <T> T throwWrongSyntax() {
        if (tokenInfos.isEmpty()) {
            throw config.getSyntaxExceptionProducer().apply(null, this);
        }
        throw config.getSyntaxExceptionProducer().apply(getAdjusted(), copy());
    }

    @Override
    public TokenStream copy() {
        LazyTokenStream copy = new LazyTokenStream(lexer, expression);
        copy.i = i;
        copy.tokenInfos.addAll(tokenInfos);
        copy.offset = offset;
        copy.mark = mark;
        return copy;
    }

    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    private boolean readNextToken() {
        i++; // forward
        for (int j; i < len; i++) {
            char c = expression.charAt(i);
            if (c <= ' ') continue;

            // comment
            List<String> singleComments = config.getSingleComments();
            if (ObjectUtil.isNotEmpty(singleComments)) {
                for (String singleComment : singleComments) {
                    char first = singleComment.charAt(0);
                    int width = singleComment.length();
                    if (c == first && i < len - width && expression.substring(i, i + width).equals(singleComment)) {
                        for (j = i + width; j < len && (c = expression.charAt(j)) != '\n'; j++) ;
                        CommentToken token = CommentToken.of(expression.substring(i, j), singleComment);
                        tokenInfos.add(TokenInfo.of(token, i, j));
                        i = j;
                        return true; // found \n or EOF
                    }
                }
            }

            List<Pair<String, String>> multipleComments = config.getMultipleComments();
            if (ObjectUtil.isNotEmpty(multipleComments)) {
                for (Pair<String, String> multipleComment : multipleComments) {
                    String start = multipleComment.first(), end = multipleComment.second();
                    char first = start.charAt(0), last = end.charAt(0);
                    int startWidth = start.length(), endWidth = end.length(), lastEnd = len - endWidth;
                    if (c == first && i < len - startWidth && expression.substring(i, i + startWidth).equals(start)) {
                        for (j = i + startWidth; j <= lastEnd; j++) {
                            if (expression.charAt(j) == last && expression.substring(j, j + endWidth).equals(end)) {
                                CommentToken token = CommentToken.of(
                                        expression.substring(i, j += endWidth), start, end);
                                tokenInfos.add(TokenInfo.of(token, i, j));
                                i = j;
                                return true;
                            }
                        }
                        return lexer.throwInvalidToken(expression, i); // throws error
                    }
                }
            }

            // identifier or identifier value
            if (StringUtil.isFirstVariableChar(c)) {
                String v = StringSearcher.searchVar(expression, i);
                Token token = config.getKeywords().get(v);
                if (token == null) {
                    token = lexer.identifierCache.computeIfAbsent(v, IdentifierToken::new);
                }
                tokenInfos.add(TokenInfo.of(token, i, i + v.length()));
                i += v.length() - 1;
                return true;
            }

            // number
            if (StringUtil.isNumberChar(c)) {
                Pair<Integer, Boolean> pair = NumberSearcher.search(expression, i);
                if (pair == null) {
                    return lexer.throwInvalidToken(expression, len - 1);
                }
                String value = expression.substring(i, pair.first());
                Number num = lexer.parseNumber(value, pair.second());

                NumberToken token = lexer.numberCache.computeIfAbsent(
                        value, it -> new NumberToken(num, value));
                tokenInfos.add(TokenInfo.of(token, i, pair.first()));
                i += value.length() - 1;
                return true;
            }

            // string
            if (c == '\'' || c == '"' || c == '`') {
                String value = StringSearcher.searchLiteral(expression, i);
                if (value == null) {
                    return lexer.throwInvalidToken(expression, len - 1);
                }
                StringToken token;
                if (c == '\'') {
                    token = StringToken.ofSingle(value);
                } else if (c == '"') {
                    token = StringToken.ofDouble(value);
                } else {
                    token = StringToken.ofBacktick(value);
                }
                tokenInfos.add(TokenInfo.of(token, i, i + value.length() + 2));
                i += value.length() + 1;
                return true;
            }

            // punctuation
            PunctuationToken punctuationToken = PunctuationToken.search(c);
            if (punctuationToken != null) {
                tokenInfos.add(TokenInfo.of(punctuationToken, i, i + 1));
                return true;
            }

            // operator
            Pair<OperatorToken, Integer> pair = OperatorToken.search(expression, i);
            if (pair != null) {
                tokenInfos.add(TokenInfo.of(pair.first(), i, pair.second()));
                i = pair.second() - 1;
                return true;
            }

            return lexer.throwInvalidToken(expression, i);
        }
        return false;
    }

    private TokenInfo getAdjusted() {
        int adjustedOffset = StreamUtil.limitRange(offset, 0, tokenInfos.size() - 1);
        return tokenInfos.get(adjustedOffset);
    }
}
