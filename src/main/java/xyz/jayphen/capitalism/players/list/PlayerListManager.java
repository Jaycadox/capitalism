package xyz.jayphen.capitalism.players.list;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.players.display.PlayerDisplay;

public class PlayerListManager {
	public static void set(Player p) {
		var hover = PlayerDisplay.from(p).hoverEvent();
		if(hover == null) {
			p.playerListName(PlayerDisplay.from(p));
		} else {
			p.playerListName(((Component) hover.value()).append(Component.text(" ")).append(PlayerDisplay.from(p)));
		}

		final Component header = MiniMessage.miniMessage().deserialize(
				"<color:#d34aff><bold>CAPITALISM SMP</bold></color><newline>"
		);
		final Component footer = MiniMessage.miniMessage().deserialize(
				"<newline><color:blue>Version: 0.1-prealpha</color><newline><red><bold>REPORT BUGS TO: <reset><yellow>jayphen#6666"
		);
		Capitalism.ADVENTURE.player(p).sendPlayerListHeaderAndFooter(header, footer);

	}
}
