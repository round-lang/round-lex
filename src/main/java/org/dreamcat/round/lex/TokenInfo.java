package org.dreamcat.round.lex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Jerry Will
 * @version 2021-09-07
 */
@Getter
@RequiredArgsConstructor
public class TokenInfo {

    final Token token; // token in the expression
    final int start; // start offset in the expression
    final int end; // end (exclusive) offset in the expression

    // lazy compute
    int startLine; // 1-based index
    int startCol; // 1-based
    int endLine;
    int endCol;

    public static TokenInfo of(
            Token token, int start, int end) {
        return new TokenInfo(token, start, end);
    }

    @Override
    public String toString() {
        return String.format("%s (%d-%d) (%d:%d, %d:%d)",
                token, start, end, startLine, startCol, endLine, endCol);
    }
}
