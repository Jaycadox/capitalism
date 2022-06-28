package xyz.jayphen.capitalism.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;

public class Balance implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
		if (!( commandSender instanceof Player )) {
			new MessageBuilder("Economy").appendCaption("Only players can use this command").send(commandSender);
			
			return true;
		}
		Player p = (Player) commandSender;
		new MessageBuilder("Economy").appendCaption("You have")
				.appendVariable("$" + NumberFormatter.addCommas(DatabasePlayer.from(p).getMoneySafe())).send(p);
		return true;
	}
}
