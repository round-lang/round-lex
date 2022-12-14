package org.dreamcat.round.lex;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.dreamcat.common.Pair;
import org.dreamcat.common.text.NumberSearcher;
import org.dreamcat.common.text.StringSearcher;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.NumberUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.round.lex.LexConfig.BigNumberStrategy;

/**
 * @author Jerry Will
 * @since 2021-07-04
 */
@RequiredArgsConstructor
public final class Lexer {

    final LexConfig config;
    final Map<String, IdentifierToken> identifierCache = new ConcurrentHashMap<>();
    final Map<String, NumberToken> numberCache = new ConcurrentHashMap<>();

    public void clear() {
        identifierCache.clear();
        numberCache.clear();
    }

    public TokenStream lex(String expression) {
        SimpleTokenStream stream = new SimpleTokenStream(expression, config);

        int size = expression.length(), j;
        outer:
        for (int i = 0; i < size; i++) {
            char c = expression.charAt(i);
            if (c <= ' ') continue;

            // comment
            List<String> singleComments = config.getSingleComments();
            if (CollectionUtil.isNotEmpty(singleComments)) {
                for (String singleComment : singleComments) {
                    char first = singleComment.charAt(0);
                    int width = singleComment.length();
                    if (c == first && i < size - width && expression.substring(i, i + width).equals(singleComment)) {
                        for (j = i + width; j < size && (c = expression.charAt(j)) != '\n'; j++) ;
                        CommentToken token = CommentToken.of(expression.substring(i, j), singleComment);
                        stream.add(TokenInfo.of(token, i, j));
                        i = j;
                        if (c == '\n') continue outer; // found \n
                        else break outer; // found EOF
                    }
                }
            }

            List<Pair<String, String>> multipleComments = config.getMultipleComments();
            if (CollectionUtil.isNotEmpty(multipleComments)) {
                for (Pair<String, String> multipleComment : multipleComments) {
                    String start = multipleComment.first(), end = multipleComment.second();
                    char first = start.charAt(0), last = end.charAt(0);
                    int startWidth = start.length(), endWidth = end.length(), lastEnd = size - endWidth;
                    if (c == first && i < size - startWidth && expression.substring(i, i + startWidth).equals(start)) {
                        for (j = i + startWidth; j <= lastEnd; j++) {
                            if (expression.charAt(j) == last && expression.substring(j, j + endWidth).equals(end)) {
                                CommentToken token = CommentToken.of(
                                        expression.substring(i, j += endWidth), start, end);
                                stream.add(TokenInfo.of(token, i, j));
                                i = j;
                                continue outer;
                            }
                        }
                        return throwInvalidToken(expression, i);
                    }
                }
            }

            // identifier or identifier value
            if (StringUtil.isFirstVariableChar(c)) {
                String v = StringSearcher.searchVar(expression, i);
                Token token = config.getKeywords().get(v);
                if (token == null) {
                    token = config.getIdentifierValues().get(v);
                    if (token == null) {
                        token = identifierCache.computeIfAbsent(v, IdentifierToken::new);
                    }
                }
                stream.add(TokenInfo.of(token, i, i + v.length()));
                i += v.length() - 1;
                continue;
            }

            // number
            if (StringUtil.isNumberChar(c)) {
                Pair<Integer, Boolean> pair = NumberSearcher.search(expression, i);
                if (pair == null) {
                    return throwInvalidToken(expression, size - 1);
                }
                String value = expression.substring(i, pair.first());
                Number num = parseNumber(value, pair.second());

                NumberToken token = numberCache.computeIfAbsent(
                        value, it -> new NumberToken(num, value));
                stream.add(TokenInfo.of(token, i, pair.first()));
                i += value.length() - 1;
                continue;
            }

            // string
            if (c == '\'' || c == '"' || c == '`') {
                String value = StringSearcher.searchLiteral(expression, i);
                if (value == null) {
                    return throwInvalidToken(expression, size - 1);
                }
                StringToken token;
                int k;
                if (c == '\'') {
                    token = StringToken.ofSingle(value);
                } else if (c == '"') {
                    token = StringToken.ofDouble(value);
                } else {
                    token = StringToken.ofBacktick(value);
                }
                stream.add(TokenInfo.of(token, i, i + value.length() + 2));
                i += value.length() + 1;
                continue;
            }

            // punctuation
            PunctuationToken punctuationToken = PunctuationToken.search(c);
            if (punctuationToken != null) {
                stream.add(TokenInfo.of(punctuationToken, i, i + 1));
                continue;
            }

            // operator
            Pair<OperatorToken, Integer> pair = OperatorToken.search(expression, i);
            if (pair != null) {
                stream.add(TokenInfo.of(pair.first(), i, pair.second()));
                i = pair.second() - 1;
                continue;
            }

            throwInvalidToken(expression, i);
        }

        return stream;
    }

    private Number parseNumber(String value, boolean floating) {
        BigNumberStrategy bns = config.getBigNumberStrategy();
        if (bns == BigNumberStrategy.NONE) {
            return NumberUtil.parseNumber(value, floating);
        } else {
            if (floating) {
                BigDecimal bigNum = new BigDecimal(value);
                if (bns == BigNumberStrategy.RANGE &&
                        NumberUtil.isDoubleRange(bigNum)) {
                    return bigNum.doubleValue();
                } else return bigNum;
            } else {
                BigInteger bigNum = new BigInteger(value);
                if (bns == BigNumberStrategy.RANGE && NumberUtil.isLongRange(bigNum)) {
                    if (NumberUtil.isIntRange(bigNum)) {
                        return bigNum.intValue();
                    } else {
                        return bigNum.longValue();
                    }
                } else return bigNum;
            }
        }
    }

    private <T> T throwInvalidToken(String expression, int offset) {
        int line = 1, col = 1;
        for (int i = 0; i <= offset; i++) {
            char c = expression.charAt(i);
            if (c != '\n') col++;
            else {
                line++;
                col = 1;
            }
        }
        throw config.getLexExceptionProducer().apply(expression, offset, line, col);
    }
}
