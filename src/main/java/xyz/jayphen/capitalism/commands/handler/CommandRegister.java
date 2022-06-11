package xyz.jayphen.capitalism.commands.handler;

import org.bukkit.plugin.Plugin;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.commands.Balance;

public class CommandRegister {
    public static void registerAllCommands(Capitalism p)
    {
        p.getCommand("balance").setExecutor(new Balance());

    }

}
