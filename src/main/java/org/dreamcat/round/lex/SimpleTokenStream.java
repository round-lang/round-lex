package org.dreamcat.round.lex;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.dreamcat.common.util.ObjectUtil;

/**
 * @author Jerry Will
 * @version 2021-09-07
 */
@RequiredArgsConstructor
public class SimpleTokenStream implements TokenStream {

    @Getter
    private final String expression;
    private final LexConfig config;
    private final List<TokenInfo> tokenInfos = new ArrayList<>();
    private int size;
    private int offset; // index of next token to return
    private int mark; // marked offset

    @Setter
    private int firstLineNo = 1;
    @Setter
    private int firstCol = 1;

    // only invoke it in a lexer
    void add(TokenInfo tokenInfo) {
        computeTokenInfo(tokenInfo);
        tokenInfos.add(tokenInfo);
        size++;
    }

    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    @Override
    public boolean hasNext() {
        return offset < size;
    }

    @Override
    public Token next() {
        if (!hasNext()) return throwWrongSyntax();
        return tokenInfos.get(offset++).getToken();
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }

    @Override
    public Token previous() {
        if (!hasPrevious()) return throwWrongSyntax();
        return tokenInfos.get(--offset).getToken();
    }

    @Override
    public TokenInfo get() {
        if (offset < 0 || offset >= size) return throwWrongSyntax();
        return tokenInfos.get(offset);
    }

    @Override
    public void mark() {
        mark = offset;
    }

    @Override
    public void reset() {
        offset = mark;
        mark = 0; // clear mark
    }

    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    @Override
    public <T> T throwWrongSyntax() {
        offset = ObjectUtil.limitRange(offset, 0, size - 1);
        throw config.getSyntaxExceptionProducer().apply(this);
    }

    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    private void computeTokenInfo(TokenInfo tokenInfo) {
        TokenInfo prevTokenInfo = null;
        if (size > 0) {
            prevTokenInfo = tokenInfos.get(size - 1);
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
