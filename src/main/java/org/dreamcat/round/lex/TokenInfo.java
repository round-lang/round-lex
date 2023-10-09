package org.dreamcat.round.lex;

import lombok.Getter;

/**
 * @author Jerry Will
 * @version 2021-09-07
 */
@Getter
public class TokenInfo extends TokenWrapper {

    final Token rawToken; // token in the expression
    final int start; // start offset in the expression
    final int end; // end (exclusive) offset in the expression

    // lazy compute
    int startLine; // 1-based index
    int startCol; // 1-based
    int endLine;
    int endCol;

    public TokenInfo(Token token, int start, int end) {
        super(token);
        this.rawToken = token;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return String.format("%s (%d-%d) (%d:%d, %d:%d)",
                rawToken, start, end, startLine, startCol, endLine, endCol);
    }

    @Override
    public Token replace(Token token) {
        if (!(token instanceof TokenWrapper)) {
            this.token = token;
            return this;
        }
        return super.replace(token);
    }
}
