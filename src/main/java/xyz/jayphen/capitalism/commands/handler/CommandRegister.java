package xyz.jayphen.capitalism.commands.handler;

import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.commands.Balance;
import xyz.jayphen.capitalism.commands.Land;
import xyz.jayphen.capitalism.commands.Pay;
import xyz.jayphen.capitalism.commands.admin.Admin;

public class CommandRegister {
	public static void registerAllCommands (Capitalism p) {
		p.getCommand("balance").setExecutor(new Balance());
		p.getCommand("pay").setExecutor(new Pay());
		p.getCommand("admin").setExecutor(new Admin());
		p.getCommand("admin").setTabCompleter(new Admin());

		p.getCommand("land").setExecutor(new Land());
		p.getCommand("land").setTabCompleter(new Land());

	}

}
