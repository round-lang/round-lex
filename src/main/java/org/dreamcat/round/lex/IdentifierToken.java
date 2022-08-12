package org.dreamcat.round.lex;

import java.util.Objects;

/**
 * @author Jerry Will
 * @since 2021-07-03
 */
public class IdentifierToken extends AbstractToken {

    public IdentifierToken(String rawToken) {
        super(Type.IDENTIFIER, rawToken);
    }

    @Override
    public boolean isIdentifier() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof IdentifierToken) {
            return Objects.equals(getIdentifier(), ((IdentifierToken) o).getIdentifier());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getIdentifier());
    }
    
    public boolean is(String identifier) {
        return Objects.equals(getIdentifier(), identifier);
    }
}
