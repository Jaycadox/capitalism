package xyz.jayphen.capitalism.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import xyz.jayphen.capitalism.Capitalism;

import java.util.ArrayList;
import java.util.List;

public class MessageBuilder {
	protected ArrayList<Token> message = new ArrayList<>();
	private String prefix = "";
	
	public MessageBuilder(String prefix) {
		this.prefix = prefix;
	}
	
	protected MessageBuilder(String prefix, ArrayList<Token> message) {
		this.prefix = prefix;
		this.message = message;
	}
	
	public MessageBuilder appendComponent(Component msg) {
		var tok = new Token("", Token.TokenType.COMPONENT);
		tok.setComponent(msg);
		message.add(tok);
		return new MessageBuilder(prefix, message);
	}
	
	public MessageBuilder appendCaption(String msg) {
		message.add(new Token(msg, Token.TokenType.CAPTION));
		return new MessageBuilder(prefix, message);
	}
	
	public MessageBuilder appendVariable(String msg) {
		message.add(new Token(msg, Token.TokenType.VARIABLE));
		return new MessageBuilder(prefix, message);
	}
	
	public MessageBuilder append(Token.TokenType type, String msg) {
		message.add(new Token(msg, type));
		return new MessageBuilder(prefix, message);
	}
	
	public MessageBuilder appendData(Token.TokenType type, String msg, String data) {
		var tok = new Token(msg, type);
		tok.setExtraData(data);
		message.add(tok);
		return new MessageBuilder(prefix, message);
	}
	
	public MessageBuilder appendList(List<String> list) {
		message.add(new Token("[", Token.TokenType.VARIABLE));
		for (int i = 0; i < list.size() - 1; i++) {
			message.add(new Token(list.get(i) + ", ", Token.TokenType.VARIABLE));
		}
		message.add(new Token(list.get(list.size() - 1) + "]", Token.TokenType.VARIABLE));
		return new MessageBuilder(prefix, message);
	}
	
	public BaseComponent[] buildWithCommand(String cmd, boolean deprecated) {
		ComponentBuilder message = new ComponentBuilder(build(false));
		
		return null;
	}
	
	public String build(boolean deprecated) {
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.BLUE).append(prefix).append(" > ");
		for (Token tok : message) {
			boolean endsWithNormalChar = !( sb.toString().endsWith(".") || sb.toString().endsWith(",") || sb.toString().endsWith("[") );
			switch (tok.token) {
				case CAPTION -> {
					sb.append(ChatColor.GRAY).append(tok.content);
					if (endsWithNormalChar) {
						sb.append(' ');
					}
				}
				case VARIABLE -> {
					sb.append(ChatColor.YELLOW).append(tok.content);
					if (endsWithNormalChar) {
						sb.append(' ');
					}
				}
				case BRACKET -> {
					sb.append(ChatColor.GRAY).append('(').append(ChatColor.YELLOW).append(tok.content).append(ChatColor.GRAY).append(')');
					if (endsWithNormalChar) {
						sb.append(' ');
					}
				}
			}
		}
		return sb.toString().trim().replace(" ,", "").replace(" .", ".").replace("[ ", "[").replace(",  ", ", ");
	}
	
	public Component make() {
		Component textComponent = Component.text(prefix + " > ", NamedTextColor.BLUE);
		for (Token tok : message) {
			var defaultColor = NamedTextColor.GRAY;
			if (tok.token != Token.TokenType.CAPTION) {
				defaultColor = NamedTextColor.YELLOW;
			}
			if (tok.token == Token.TokenType.BRACKET) {
				tok.content = "(" + tok.content + ")";
			}
			Component raw = Component.text(tok.content + " ", defaultColor);
			if (tok.token == Token.TokenType.CHAT || tok.token == Token.TokenType.COMMAND) {
				raw = raw.clickEvent(ClickEvent.runCommand(tok.getExtraData())).hoverEvent(HoverEvent.showText(Component.text("Clickable action...")));
			}
			if (tok.token == Token.TokenType.COMPONENT) {
				raw = tok.getComponent().append(Component.text(" "));
			}
			textComponent = textComponent.append(raw);
			
		}
		textComponent = textComponent.replaceText(TextReplacementConfig.builder().matchLiteral(" .").replacement(". ").build());
		textComponent = textComponent.replaceText(TextReplacementConfig.builder().matchLiteral(" ,").replacement(", ").build());
		textComponent = textComponent.replaceText(TextReplacementConfig.builder().matchLiteral("[ ").replacement("[").build());
		return textComponent;
	}
	
	public void broadcast() {
		Capitalism.ADVENTURE.all().sendMessage(make());
	}
	
	public void send(CommandSender p) {
		Capitalism.ADVENTURE.sender(p).sendMessage(make());
	}
	
	public void sendActionBar(CommandSender p) {
		Capitalism.ADVENTURE.sender(p).sendActionBar(make());
	}
}
