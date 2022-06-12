package xyz.jayphen.capitalism.lang;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
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
    public BaseComponent[] buildWithCommand(String cmd) {
        ComponentBuilder message = new ComponentBuilder(build());
        BaseComponent[] msg = message.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd)).create();
        return msg;
    }

    public String build()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.BLUE).append(prefix).append(" > ");
        for(Token tok : message) {
            boolean endsWithNormalChar = !(sb.toString().endsWith(".") || sb.toString().endsWith(","));
            switch(tok.token) {
                case CAPTION:
                    sb.append(ChatColor.GRAY).append(tok.content);
                    if(endsWithNormalChar)
                    {
                        sb.append(' ');
                    }
                    break;
                case VARIABLE:
                    sb.append(ChatColor.YELLOW).append(tok.content);
                    if(endsWithNormalChar)
                    {
                        sb.append(' ');
                    }
                    break;
                case BRACKET:
                    sb.append(ChatColor.GRAY).append('(').append(ChatColor.YELLOW).append(tok.content).append(ChatColor.GRAY).append(')');
                    if(endsWithNormalChar)
                    {
                        sb.append(' ');
                    }
                    break;
            }
        }
        return sb.toString().trim().replace(" ,", "").replace(" .", ".");
    }

}
