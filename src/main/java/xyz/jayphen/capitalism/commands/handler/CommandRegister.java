package xyz.jayphen.capitalism.commands.handler;

import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.commands.*;
import xyz.jayphen.capitalism.commands.admin.Admin;

public class CommandRegister {
	public static void registerAllCommands(Capitalism p) {
		p.getCommand("balance").setExecutor(new Balance());
		p.getCommand("pay").setExecutor(new Pay());
		p.getCommand("profile").setExecutor(new Profile());
		p.getCommand("admin").setExecutor(new Admin());
		p.getCommand("admin").setTabCompleter(new Admin());
		p.getCommand("cheapest").setExecutor(new Cheapest());
		p.getCommand("land").setExecutor(new Land());
		p.getCommand("land").setTabCompleter(new Land());
		p.getCommand("hud").setExecutor(new Hud());
		
	}
	
}
