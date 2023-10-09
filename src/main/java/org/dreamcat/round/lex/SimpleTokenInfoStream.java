package org.dreamcat.round.lex;

import lombok.Setter;

/**
 * @author Jerry Will
 * @version 2021-09-07
 */
public class SimpleTokenInfoStream extends SimpleTokenStream {

    @Setter
    int firstLineNo = 1;
    @Setter
    int firstCol = 1;

    public SimpleTokenInfoStream(String expression, LexConfig config) {
        super(expression, config);
    }

    // only invoke it in a lexer
    public void add(Token token, int start, int end) {
        TokenInfo tokenInfo = new TokenInfo(token, start, end);
        computeTokenInfo(tokenInfo);
        tokens.add(tokenInfo);
        size++;
    }

    @Override
    public TokenStream copy() {
        SimpleTokenInfoStream copy = new SimpleTokenInfoStream(expression, config);
        copy.tokens.addAll(tokens);
        copy.size = size;
        copy.offset = offset;
        copy.mark = mark;
        copy.firstLineNo = firstLineNo;
        copy.firstCol = firstCol;
        return copy;
    }

    private void computeTokenInfo(TokenInfo tokenInfo) {
        TokenInfo prevTokenInfo = null;
        if (size > 0) {
            prevTokenInfo = (TokenInfo) tokens.get(size - 1);
        }

        int startLine, startCol;
        int prev = 0;
        // already compute, then use previous token cache
        if (prevTokenInfo != null && prevTokenInfo.startLine > 0) {
            prev = prevTokenInfo.getEnd();
            startLine = prevTokenInfo.endLine;
            startCol = prevTokenInfo.endCol;
        } else {
            startLine = firstLineNo;
            startCol = firstCol;
        }

        // compute start
        int start = tokenInfo.getStart();
        for (int i = prev; i < start; i++) {
            char c = expression.charAt(i);
            if (c != '\n') {
                startCol++;
            } else {
                startLine++;
                startCol = 1;
            }
        }

        // compute end
        int endLine = startLine, endCol = startCol;
        int end = tokenInfo.getEnd();
        for (int i = start; i < end; i++) {
            char c = expression.charAt(i);
            if (c != '\n') {
                endCol++;
            } else {
                endLine++;
                endCol = 1;
            }
        }

        tokenInfo.startLine = startLine;
        tokenInfo.startCol = startCol;
        tokenInfo.endLine = endLine;
        tokenInfo.endCol = endCol;
    }
}
