package xyz.jayphen.capitalism.helpers;

import org.bukkit.entity.Player;
import xyz.jayphen.capitalism.lang.MessageBuilder;

import java.util.ArrayList;
import java.util.UUID;

public class ChatInput {
	public static final ArrayList<Query> OPEN_QUERIES = new ArrayList<>();
	
	;
	
	public static ArrayList<Query> getOpenQueries() {
		return OPEN_QUERIES;
	}
	
	public static void createQuery(String text, ChatQueryRunnable onQuery, Player p) {
		p.closeInventory();
		OPEN_QUERIES.add(new Query(p.getUniqueId(), onQuery, text));
		new MessageBuilder("Query").appendCaption("Please type in chat your query for:").appendVariable(text + ".")
				.appendCaption("If you wish to cancel, please sneak").send(p);
	}
	
	public record Query(UUID player, ChatQueryRunnable runnable, String query) {}
}
