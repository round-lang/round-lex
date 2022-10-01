package org.dreamcat.round.lex;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dreamcat.common.util.StreamUtil;

/**
 * @author Jerry Will
 * @version 2021-09-07
 */
@RequiredArgsConstructor
public class SimpleTokenStream implements TokenStream {

    @Getter
    final String expression;
    final LexConfig config;
    final List<Token> tokens = new ArrayList<>();
    int size;
    int offset; // index of next token to return
    int mark; // marked offset

    public void add(Token token) {
        add(token, -1, -1);
    }

    // only invoke it in a lexer
    public void add(Token token, int start, int end) {
        tokens.add(token);
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
        return tokens.get(offset++);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }

    @Override
    public Token previous() {
        if (!hasPrevious()) return throwWrongSyntax();
        return tokens.get(--offset);
    }

    @Override
    public Token get() {
        if (offset < 0 || offset >= size) return throwWrongSyntax();
        return tokens.get(offset);
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
        if (tokens.isEmpty()) {
            throw config.getSyntaxExceptionProducer().apply(null, this);
        }
        int adjustedOffset = StreamUtil.limitRange(offset, 0, tokens.size() - 1);
        Token token = tokens.get(adjustedOffset);
        throw config.getSyntaxExceptionProducer().apply(token, copy());
    }

    @Override
    public TokenStream copy() {
        SimpleTokenStream copy = new SimpleTokenStream(expression, config);
        copy.tokens.addAll(tokens);
        copy.size = size;
        copy.offset = offset;
        copy.mark = mark;
        return copy;
    }
}
