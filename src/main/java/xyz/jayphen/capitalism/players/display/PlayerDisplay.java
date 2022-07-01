package xyz.jayphen.capitalism.players.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;

public class PlayerDisplay {
	public static Component from(OfflinePlayer p) {
		if (p.getUniqueId().toString().equals("3823f732-4e06-459b-89b3-e53c986d7b59")) { //jypn
			return MiniMessage.miniMessage()
					.deserialize("<gradient:#ff00eb:#5d51ff><hover:show_text:'<yellow>DEV'>" + p.getName() + "</hover></gradient>");
		}
		if (p.getUniqueId().toString().equals("c5d50e4e-1a57-4fde-b578-6145d8847d53")) { //blox
			return MiniMessage.miniMessage()
					.deserialize("<gradient:#5b3d37:#a89b95><hover:show_text:'<yellow>SILKSONG'>" + p.getName() + "</hover></gradient>");
		}
		if (p.getUniqueId().toString().equals("0a1ddfd5-4c81-432d-bc4e-c1b864b20ff6")) { //zlch
			return MiniMessage.miniMessage()
					.deserialize("<hover:show_text:'<rainbow>LGBT</rainbow>'><color:#8f6b07>" + p.getName() + "</color></hover>");
		}
		return MiniMessage.miniMessage().deserialize("<color:#e8e8e8>" + p.getName() + "</color>");
	}
}
