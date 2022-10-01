package org.dreamcat.round.lex;

/**
 * @author Jerry Will
 * @version 2022-10-01
 */
class LazyTokenInfoStream extends LazyTokenStream {

    public LazyTokenInfoStream(Lexer lexer, String expression) {
        super(lexer, new SimpleTokenInfoStream(expression, lexer.config));
    }

    @Override
    public TokenStream copy() {
        LazyTokenInfoStream copy = new LazyTokenInfoStream(lexer, expression);
        copy.i = i;
        return copy;
    }
}
