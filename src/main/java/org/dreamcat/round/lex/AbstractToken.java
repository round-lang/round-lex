package org.dreamcat.round.lex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Jerry Will
 * @since 2021-07-03
 */
@Getter
@RequiredArgsConstructor
class AbstractToken implements Token {

    final Type type;
    final String raw;

    @Override
    public String toString() {
        return this.getRaw();
    }
}
