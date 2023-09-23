package org.dreamcat.round.lex;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.dreamcat.common.Pair;
import org.dreamcat.common.text.NumberSearcher;
import org.dreamcat.common.text.StringSearcher;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;

/**
 * @author Jerry Will
 * @since 2021-07-04
 */
@RequiredArgsConstructor
public class Lexer {

    final LexConfig config;
    final Map<String, IdentifierToken> identifierCache = new ConcurrentHashMap<>();
    final Map<String, NumberToken> numberCache = new ConcurrentHashMap<>();

    public Lexer() {
        this(new LexConfig());
    }

    public void clear() {
        identifierCache.clear();
        numberCache.clear();
    }

    public TokenStream lex(String expression) {
        if (config.enableLazy()) {
            if (config.enableTokenInfo()) {
                return new LazyTokenInfoStream(this, expression);
            } else {
                return new LazyTokenStream(this, expression);
            }
        }
        // no lazy
        SimpleTokenStream stream;
        if (config.enableTokenInfo()) {
            stream = new SimpleTokenInfoStream(expression, config);
        } else {
            stream = new SimpleTokenStream(expression, config);
        }
        lex(expression, stream);
        return stream;
    }

    public void lex(String expression, SimpleTokenStream stream) {
        int len = expression.length(), j;
        outer:
        for (int i = 0; i < len; i++) {
            char c = expression.charAt(i);
            if (c <= ' ') continue;

            // comment
            List<String> singleComments = config.singleComments();
            if (ObjectUtil.isNotEmpty(singleComments)) {
                for (String singleComment : singleComments) {
                    char first = singleComment.charAt(0);
                    int width = singleComment.length();
                    if (c == first && i < len - width && expression.substring(i, i + width).equals(singleComment)) {
                        for (j = i + width; j < len && (c = expression.charAt(j)) != '\n'; j++) ;
                        CommentToken token = CommentToken.of(expression.substring(i, j), singleComment);
                        stream.add(token, i, j);
                        i = j;
                        if (c == '\n') continue outer; // found \n
                        else break outer; // found EOF
                    }
                }
            }

            List<Pair<String, String>> multipleComments = config.multipleComments();
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
                                stream.add(token, i, j);
                                i = j;
                                continue outer;
                            }
                        }
                        throw config.lexExceptionProducer().apply(expression, i);
                    }
                }
            }

            // identifier
            if (StringUtil.isFirstVariableChar(c)) {
                String v = StringSearcher.searchVar(expression, i);
                Token token = config.keywords().get(v);
                if (token == null) {
                    token = identifierCache.computeIfAbsent(v, IdentifierToken::new);
                }
                stream.add(token, i, i + v.length());
                i += v.length() - 1;
                continue;
            }

            // number
            if (StringUtil.isNumberChar(c)) {
                Pair<Integer, Boolean> pair = NumberSearcher.search(expression, i);
                if (pair == null) {
                    throw config.lexExceptionProducer().apply(expression, len - 1);
                }
                String value = expression.substring(i, pair.first());
                Number num = config.parseNumber(value, pair.second());

                NumberToken token = numberCache.computeIfAbsent(
                        value, it -> new NumberToken(num, value));
                stream.add(token, i, pair.first());
                i += value.length() - 1;
                continue;
            }

            // string
            if (c == '\'' || c == '"' || c == '`') {
                String value = StringSearcher.searchLiteral(expression, i);
                if (value == null) {
                    throw config.lexExceptionProducer().apply(expression, len - 1);
                }
                StringToken token;
                if (c == '\'') {
                    token = StringToken.ofSingle(value);
                } else if (c == '"') {
                    token = StringToken.ofDouble(value);
                } else {
                    token = StringToken.ofBacktick(value);
                }
                stream.add(token, i, i + value.length() + 2);
                i += value.length() + 1;
                continue;
            }

            // punctuation
            PunctuationToken punctuationToken = PunctuationToken.search(c);
            if (punctuationToken != null) {
                stream.add(punctuationToken, i, i + 1);
                continue;
            }

            // operator
            Pair<OperatorToken, Integer> pair = OperatorToken.search(expression, i);
            if (pair != null) {
                stream.add(pair.first(), i, pair.second());
                i = pair.second() - 1;
                continue;
            }

            throw config.lexExceptionProducer().apply(expression, i);
        }
    }
}
