package org.dreamcat.round.lex;

import java.util.List;
import lombok.Getter;
import org.dreamcat.common.Pair;
import org.dreamcat.common.text.NumberSearcher;
import org.dreamcat.common.text.StringSearcher;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;

/**
 * @author Jerry Will
 * @version 2022-10-01
 */
@Getter
class LazyTokenStream implements TokenStream {

    final Lexer lexer;
    final LexConfig config;
    final String expression;
    final SimpleTokenStream stream;
    final int len; // expression length
    int i = -1; // expression offset

    protected LazyTokenStream(Lexer lexer, SimpleTokenStream stream) {
        this.lexer = lexer;
        this.config = lexer.config;
        this.expression = stream.getExpression();
        this.stream = stream;
        this.len = expression.length();
    }

    public LazyTokenStream(Lexer lexer, String expression) {
        this(lexer, new SimpleTokenStream(expression, lexer.config));
    }

    @Override
    public boolean hasNext() {
        return stream.hasNext() ||
                readNextToken();
    }

    @Override
    public Token next() {
        return stream.next();
    }

    @Override
    public boolean hasPrevious() {
        return stream.hasPrevious();
    }

    @Override
    public Token previous() {
        return stream.previous();
    }

    @Override
    public Token get() {
        return stream.get();
    }

    @Override
    public void mark() {
        stream.mark();
    }

    @Override
    public void reset() {
        stream.reset();
    }

    @Override
    public <T> T throwWrongSyntax() {
        return stream.throwWrongSyntax();
    }

    @Override
    public TokenStream copy() {
        LazyTokenStream copy = new LazyTokenStream(lexer, expression);
        copy.i = i;
        return copy;
    }

    private void addToken(Token token, int start, int end) {
        stream.add(token, start, end);
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
                        addToken(token, i, j);
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
                                addToken(token, i, j);
                                i = j;
                                return true;
                            }
                        }
                        throw config.getLexExceptionProducer().apply(expression, i);
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
                addToken(token, i, i + v.length());
                i += v.length() - 1;
                return true;
            }

            // number
            if (StringUtil.isNumberChar(c)) {
                Pair<Integer, Boolean> pair = NumberSearcher.search(expression, i);
                if (pair == null) {
                    throw config.getLexExceptionProducer().apply(expression, len - 1);
                }
                String value = expression.substring(i, pair.first());
                Number num = config.parseNumber(value, pair.second());

                NumberToken token = lexer.numberCache.computeIfAbsent(
                        value, it -> new NumberToken(num, value));
                addToken(token, i, pair.first());
                i += value.length() - 1;
                return true;
            }

            // string
            if (c == '\'' || c == '"' || c == '`') {
                String value = StringSearcher.searchLiteral(expression, i);
                if (value == null) {
                    throw config.getLexExceptionProducer().apply(expression, len - 1);
                }
                StringToken token;
                if (c == '\'') {
                    token = StringToken.ofSingle(value);
                } else if (c == '"') {
                    token = StringToken.ofDouble(value);
                } else {
                    token = StringToken.ofBacktick(value);
                }
                addToken(token, i, i + value.length() + 2);
                i += value.length() + 1;
                return true;
            }

            // punctuation
            PunctuationToken punctuationToken = PunctuationToken.search(c);
            if (punctuationToken != null) {
                addToken(punctuationToken, i, i + 1);
                return true;
            }

            // operator
            Pair<OperatorToken, Integer> pair = OperatorToken.search(expression, i);
            if (pair != null) {
                addToken(pair.first(), i, pair.second());
                i = pair.second() - 1;
                return true;
            }

            throw config.getLexExceptionProducer().apply(expression, i);
        }
        return false;
    }
}
