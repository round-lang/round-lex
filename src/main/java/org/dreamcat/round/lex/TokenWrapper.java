package org.dreamcat.round.lex;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * @author Jerry Will
 * @version 2021-09-07
 */
@NoArgsConstructor
@AllArgsConstructor
public class TokenWrapper implements Token {

    protected Token token;

    public Type getType() {
        return token.getType();
    }

    @Override
    public String getRaw() {
        return token.getRaw();
    }

    @Override
    public boolean isIdentifier() {
        return token.isIdentifier();
    }

    @Override
    public String getIdentifier() {
        return token.getIdentifier();
    }

    @Override
    public boolean isValue() {
        return token.isValue();
    }

    @Override
    public Object getValue() {
        return token.getValue();
    }

    @Override
    public boolean isOperator() {
        return token.isOperator();
    }

    @Override
    public OperatorToken getOperator() {
        return token.getOperator();
    }

    @Override
    public boolean isLeftParenthesis() {
        return token.isLeftParenthesis();
    }

    @Override
    public boolean isRightParenthesis() {
        return token.isRightParenthesis();
    }

    @Override
    public boolean isLeftBracket() {
        return token.isLeftBracket();
    }

    @Override
    public boolean isRightBracket() {
        return token.isRightBracket();
    }

    @Override
    public boolean isLeftBrace() {
        return token.isLeftBrace();
    }

    @Override
    public boolean isRightBrace() {
        return token.isRightBrace();
    }

    @Override
    public boolean isSemicolon() {
        return token.isSemicolon();
    }

    @Override
    public boolean isComma() {
        return token.isComma();
    }

    @Override
    public boolean isColon() {
        return token.isColon();
    }

    @Override
    public boolean isAt() {
        return token.isAt();
    }

    @Override
    public boolean isSharp() {
        return token.isSharp();
    }

    @Override
    public boolean isDollar() {
        return token.isDollar();
    }

    @Override
    public boolean isDot() {
        return token.isDot();
    }
}
