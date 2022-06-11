package xyz.jayphen.capitalism.events.register;

import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.events.PlayerJoin;

public class EventRegister {
    public static void registerAll() {
        Capitalism.plugin.getServer().getPluginManager().registerEvents(new PlayerJoin(), Capitalism.plugin);
    }
}
