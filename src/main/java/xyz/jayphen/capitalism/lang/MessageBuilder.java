package xyz.jayphen.capitalism.lang;

import org.bukkit.ChatColor;

import java.util.ArrayList;

public class MessageBuilder {
    private String prefix = "";
    protected ArrayList<Token> message = new ArrayList<>();
    public MessageBuilder(String prefix)
    {
        this.prefix = prefix;
    }
    protected MessageBuilder(String prefix, ArrayList<Token> message)
    {
        this.prefix = prefix;
        this.message = message;
    }
    public MessageBuilder append(Token.TokenType type, String msg)
    {
        message.add(new Token(msg, type));
        return new MessageBuilder(prefix, message);
    }
    public String build()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.BLUE).append(prefix).append(" > ");
        for(Token tok : message) {
            switch(tok.token) {
                case CAPTION:
                    sb.append(ChatColor.GRAY).append(tok.content).append(' ');
                    break;
                case VARIABLE:
                    sb.append(ChatColor.YELLOW).append(tok.content).append(' ');
                    break;
            }
        }
        return sb.toString().trim();
    }

}
