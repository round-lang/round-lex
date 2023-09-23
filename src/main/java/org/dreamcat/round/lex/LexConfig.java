package org.dreamcat.round.lex;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.Pair;
import org.dreamcat.common.util.NumberUtil;
import org.dreamcat.round.exception.LexCompileException;
import org.dreamcat.round.exception.SyntaxCompileException;

/**
 * @author Jerry Will
 * @since 2021-07-08
 */
@Getter
@Setter
@Accessors(fluent = true)
public class LexConfig {

    /**
     * keyword to cache
     */
    private final Map<String, IdentifierToken> keywords = new ConcurrentHashMap<>();
    /**
     * which strategy to determine the value type of number tokens
     */
    private BigNumberStrategy bigNumberStrategy = BigNumberStrategy.NONE;
    /**
     * single line comment
     */
    private List<String> singleComments = Collections.singletonList("//");
    /**
     * multiple line comment
     */
    private List<Pair<String, String>> multipleComments = Collections.singletonList(Pair.of("/*", "*/"));

    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    /**
     * supply a syntax exception
     */
    private BiFunction<Token, TokenStream, SyntaxCompileException> syntaxExceptionProducer =
            this::produceSyntaxCompileException;
    /**
     * supply a lex exception
     */
    private BiFunction<String, Integer, LexCompileException>
            lexExceptionProducer = this::produceLexCompileException;


    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    /**
     * use in help information
     */
    private String name = "expression";
    /**
     * sample char count when throw a wrong syntax exception
     */
    private int sampleCharCount = 1 << 8; // set to <=0 to disable it
    private boolean enableLazy; // lazy parse
    private boolean enableTokenInfo; // use TokenInfo instead of Token

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public LexConfig addKeyword(IdentifierToken... keywordTokens) {
        for (IdentifierToken keywordToken : keywordTokens) {
            keywords.put(keywordToken.getIdentifier(), keywordToken);
        }
        return this;
    }

    public LexConfig addKeyword(Collection<? extends IdentifierToken> keywordTokens) {
        for (IdentifierToken keywordToken : keywordTokens) {
            keywords.put(keywordToken.getIdentifier(), keywordToken);
        }
        return this;
    }

    public Number parseNumber(String value, boolean floating) {
        if (bigNumberStrategy == BigNumberStrategy.NONE) {
            return NumberUtil.parseNumber(value, floating);
        } else {
            if (floating) {
                BigDecimal bigNum = new BigDecimal(value);
                if (bigNumberStrategy == BigNumberStrategy.RANGE &&
                        NumberUtil.isDoubleRange(bigNum)) {
                    return bigNum.doubleValue();
                } else return bigNum;
            } else {
                BigInteger bigNum = new BigInteger(value);
                if (bigNumberStrategy == BigNumberStrategy.RANGE &&
                        NumberUtil.isLongRange(bigNum)) {
                    if (NumberUtil.isIntRange(bigNum)) {
                        return bigNum.intValue();
                    } else {
                        return bigNum.longValue();
                    }
                } else return bigNum;
            }
        }
    }
    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public enum BigNumberStrategy {
        /**
         * use {@link Integer}, {@link Long} and {@link Double} only
         */
        NONE,
        /**
         * always use {@link java.math.BigDecimal} and {@link java.math.BigInteger}
         */
        ALWAYS,
        /**
         * use {@link java.math.BigDecimal} and {@link java.math.BigInteger} only if out of range
         */
        RANGE
    }

    private LexCompileException produceLexCompileException(
            String expression, int offset) {
        int line = 1, col = 1;
        for (int i = 0; i <= offset; i++) {
            char c = expression.charAt(i);
            if (c != '\n') col++;
            else {
                line++;
                col = 1;
            }
        }
        String message = String.format(
                "You has invalid character in your %s, near at line %d col %d",
                name, line, col);
        return new LexCompileException(expression, offset, line, col, message);
    }

    private SyntaxCompileException produceSyntaxCompileException(
            Token token, TokenStream stream) {
        if (token == null) {
            return new SyntaxCompileException(null, stream,
                    "You has wrong syntax in your empty " + name);
        } else if (!(token instanceof TokenInfo)) {
            return new SyntaxCompileException(null, stream, String.format(
                    "You has wrong syntax in your %s, invalid token %s", name, token));
        }
        TokenInfo tokenInfo = (TokenInfo) token;
        if (sampleCharCount <= 0) {
            // no sample
            String message = String.format(
                    "You has wrong syntax in your %s, at line %d col %d",
                    name, tokenInfo.getStartLine(), tokenInfo.getStartCol());
            return new SyntaxCompileException(tokenInfo, stream, message);
        }

        String nearBy = computeNearBy(tokenInfo, stream);
        String message = String.format(
                "You has wrong syntax in your %s, at line %d col %d, near by: %s",
                name, tokenInfo.getStartLine(), tokenInfo.getStartCol(), nearBy);
        return new SyntaxCompileException(tokenInfo, stream, message);
    }

    private String computeNearBy(TokenInfo tokenInfo, TokenStream stream) {
        String expression = stream.getExpression();

        int start = tokenInfo.getStart(), end = tokenInfo.getEnd();
        int width = end - start;
        if (width > sampleCharCount) {
            return expression.substring(start, start + sampleCharCount);
        }

        int margin = (sampleCharCount - width) >> 1, length = expression.length();
        int leftMargin = start - margin, rightMargin = end + margin;

        TokenInfo left = tokenInfo, right = tokenInfo;

        // find left
        stream.mark();
        while (stream.hasPrevious()) {
            stream.previous();
            TokenInfo prev = (TokenInfo) stream.get();
            if (prev.getStart() > leftMargin) {
                left = prev;
            } else break;
        }

        // find right
        stream.reset();
        stream.next(); // consume the current token
        while (stream.hasNext()) {
            TokenInfo next = (TokenInfo) stream.get();
            if (next.getEnd() < rightMargin) {
                right = next;
            } else break;
            stream.next();
        }

        return expression.substring(left.getStart(), right.getEnd());
    }
}
