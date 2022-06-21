package xyz.jayphen.capitalism.lang;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class MessageBuilder {
	protected ArrayList<Token> message = new ArrayList<>();
	private String prefix = "";

	public MessageBuilder (String prefix) {
		this.prefix = prefix;
	}

	protected MessageBuilder (String prefix, ArrayList<Token> message) {
		this.prefix = prefix;
		this.message = message;
	}
	public MessageBuilder appendCaption (String msg) {
		message.add(new Token(msg, Token.TokenType.CAPTION));
		return new MessageBuilder(prefix, message);
	}
	public MessageBuilder appendVariable (String msg) {
		message.add(new Token(msg, Token.TokenType.VARIABLE));
		return new MessageBuilder(prefix, message);
	}
	public MessageBuilder append (Token.TokenType type, String msg) {
		message.add(new Token(msg, type));
		return new MessageBuilder(prefix, message);
	}
	public MessageBuilder appendList(List<String> list) {
		message.add(new Token("[", Token.TokenType.VARIABLE));
		for(int i = 0; i < list.size() - 1; i++) {
			message.add(new Token(list.get(i) + ", ", Token.TokenType.VARIABLE));
		}
		message.add(new Token(list.get(list.size() - 1) + "]", Token.TokenType.VARIABLE));
		return new MessageBuilder(prefix, message);
	}
	public BaseComponent[] buildWithCommand (String cmd) {
		ComponentBuilder message = new ComponentBuilder(build());
		BaseComponent[] msg = message.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd)).create();
		return msg;
	}

	public String build () {
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.BLUE).append(prefix).append(" > ");
		for (Token tok : message) {
			boolean endsWithNormalChar = !(sb.toString().endsWith(".") || sb.toString().endsWith(",") || sb.toString().endsWith("["));
			switch (tok.token) {
				case CAPTION:
					sb.append(ChatColor.GRAY).append(tok.content);
					if (endsWithNormalChar) {
						sb.append(' ');
					}
					break;
				case VARIABLE:
					sb.append(ChatColor.YELLOW).append(tok.content);
					if (endsWithNormalChar) {
						sb.append(' ');
					}
					break;
				case BRACKET:
					sb.append(ChatColor.GRAY).append('(').append(ChatColor.YELLOW).append(tok.content).append(ChatColor.GRAY).append(')');
					if (endsWithNormalChar) {
						sb.append(' ');
					}
					break;
			}
		}
		return sb.toString().trim().replace(" ,", "").replace(" .", ".").replace("[ ", "[").replace(",  ", ", ");
	}

}
