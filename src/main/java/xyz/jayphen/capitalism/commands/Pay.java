package xyz.jayphen.capitalism.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;
import xyz.jayphen.capitalism.lang.Token;

public class Pay implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage(new MessageBuilder("Economy").append(Token.TokenType.CAPTION, "Only players can use this command").build());
            return true;
        }
        if(args.length != 2) {
            return false;
        }

        // correct for player stupidity


        Player p = (Player) commandSender;
        p.sendMessage(
                new MessageBuilder("Economy")
                .append(Token.TokenType.CAPTION, "You have")
                .append(Token.TokenType.VARIABLE, "$" + NumberFormatter.addCommas(DatabasePlayer.from(p).getMoney())).build()
        );



        return true;
    }
}
