package xyz.jayphen.capitalism.lang;

public class Token {
    public enum TokenType {
        CAPTION,
        VARIABLE,
        BRACKET
    }
    String content = "";
    TokenType token = null;
    public Token(String cnt, TokenType tok) {
        this.content = cnt;
        this.token = tok;
    }
}
