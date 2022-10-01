package org.dreamcat.round.lex;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.Pair;
import org.dreamcat.common.function.QuaFunction;
import org.dreamcat.common.util.StreamUtil;
import org.dreamcat.round.exception.LexCompileException;
import org.dreamcat.round.exception.SyntaxCompileException;

/**
 * @author Jerry Will
 * @since 2021-07-08
 */
@Getter
@Setter
@Accessors(chain = true)
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
    private BiFunction<TokenInfo, TokenStream, SyntaxCompileException> syntaxExceptionProducer =
            this::produceSyntaxCompileException;
    /**
     * supply a lex exception
     */
    private QuaFunction<String, Integer, Integer, Integer, LexCompileException>
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
    private boolean enableTokenInfo = true; // to compute the tokenInfo

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public LexConfig addKeyword(IdentifierToken keywordToken) {
        keywords.put(keywordToken.getIdentifier(), keywordToken);
        return this;
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
            String expression, int offset, int line, int col) {
        String message = String.format(
                "You has invalid character in your %s, near at line %d col %d",
                name, line, col);
        return new LexCompileException(expression, offset, line, col, message);
    }

    private SyntaxCompileException produceSyntaxCompileException(
            TokenInfo tokenInfo, TokenStream stream) {
        if (tokenInfo == null) {
            return new SyntaxCompileException(null, stream,
                    "You has wrong syntax in your empty " + name);
        }
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
            TokenInfo prev = stream.get();
            if (prev.getStart() > leftMargin) {
                left = prev;
            } else break;
        }

        // find right
        stream.reset();
        stream.next(); // consume the current token
        while (stream.hasNext()) {
            TokenInfo next = stream.get();
            if (next.getEnd() < rightMargin) {
                right = next;
            } else break;
            stream.next();
        }

        return expression.substring(left.getStart(), right.getEnd());
    }
}
