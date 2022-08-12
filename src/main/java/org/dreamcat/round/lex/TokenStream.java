package org.dreamcat.round.lex;

/**
 * @author Jerry Will
 * @since 2021-07-04
 */
public interface TokenStream {

    /**
     * expression code
     */
    String getExpression();

    /**
     * has next
     */
    boolean hasNext();

    /**
     * get next token
     */
    Token next();

    /**
     * has previous
     */
    boolean hasPrevious();

    /**
     * get previous token
     */
    Token previous();

    /**
     * get next token, but not to move offset
     */
    TokenInfo get();

    /**
     * mark the current offset
     */
    void mark();

    /**
     * reset to the marked offset and clear old mark
     * the default marked offset is the head
     */
    void reset();

    /**
     * throws an exception at current offset
     * with {@link TokenInfo} message typically
     *
     * @return never reach here
     */
    <T> T throwWrongSyntax();
}
