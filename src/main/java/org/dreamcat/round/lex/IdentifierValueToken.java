package org.dreamcat.round.lex;

/**
 * @author Jerry Will
 * @version 2022-08-12
 */
public class IdentifierValueToken<T> extends ValueToken<T> {

    public IdentifierValueToken(T value, String rawToken) {
        super(value, Type.IDENTIFIER_VALUE, rawToken);
    }
}
