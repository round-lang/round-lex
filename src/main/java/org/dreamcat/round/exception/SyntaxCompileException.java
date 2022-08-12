package org.dreamcat.round.exception;

import lombok.Getter;
import org.dreamcat.round.lex.TokenInfo;
import org.dreamcat.round.lex.TokenStream;

/**
 * @author Jerry Will
 * @version 2022-08-12
 */
@Getter
public class SyntaxCompileException extends RoundException {

    /**
     * {@code stream.get()}
     * {@link TokenStream#get()}
     */
    private final TokenInfo tokenInfo;
    private final TokenStream stream;

    public SyntaxCompileException(TokenInfo tokenInfo, TokenStream stream, String message) {
        super(message);
        this.tokenInfo = tokenInfo;
        this.stream = stream;
    }
}
