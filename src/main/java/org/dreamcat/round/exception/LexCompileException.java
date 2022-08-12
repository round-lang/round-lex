package org.dreamcat.round.exception;

import lombok.Getter;

/**
 * @author Jerry Will
 * @version 2022-08-12
 */
@Getter
public class LexCompileException extends RoundException {

    private final String expression;
    private final int offset;
    private final int line;
    private final int col;

    public LexCompileException(String expression, int offset, int line, int col, String message) {
        super(message);
        this.expression = expression;
        this.offset = offset;
        this.line = line;
        this.col = col;
    }
}
