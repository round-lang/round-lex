package org.dreamcat.round.lex;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.dreamcat.common.util.StreamUtil;

/**
 * @author Jerry Will
 * @version 2021-09-07
 */
@RequiredArgsConstructor
class SimpleTokenStream implements TokenStream {

    @Getter
    final String expression;
    final LexConfig config;
    final List<TokenInfo> tokenInfos = new ArrayList<>();
    int size;
    int offset; // index of next token to return
    int mark; // marked offset

    @Setter
    int firstLineNo = 1;
    @Setter
    int firstCol = 1;

    // only invoke it in a lexer
    void add(TokenInfo tokenInfo) {
        if (config.isEnableTokenInfo()) {
            computeTokenInfo(tokenInfo);
        }
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

    @Override
    public <T> T throwWrongSyntax() {
        if (tokenInfos.isEmpty()) {
            throw config.getSyntaxExceptionProducer().apply(null, this);
        }
        throw config.getSyntaxExceptionProducer().apply(getAdjusted(), copy());
    }

    @Override
    public TokenStream copy() {
        SimpleTokenStream copy = new SimpleTokenStream(expression, config);
        copy.tokenInfos.addAll(tokenInfos);
        copy.size = size;
        copy.offset = offset;
        copy.mark = mark;
        copy.firstLineNo = firstLineNo;
        copy.firstCol = firstCol;
        return copy;
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

    private TokenInfo getAdjusted() {
        int adjustedOffset = StreamUtil.limitRange(offset, 0, tokenInfos.size() - 1);
        return tokenInfos.get(adjustedOffset);
    }
}
