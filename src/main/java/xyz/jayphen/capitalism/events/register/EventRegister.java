package xyz.jayphen.capitalism.events.register;

import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.events.PlayerJoin;
import xyz.jayphen.capitalism.events.PlayerLeave;
import xyz.jayphen.capitalism.events.PlaytimeRewards;

public class EventRegister {
    public static void registerAll() {
        Capitalism.plugin.getServer().getPluginManager().registerEvents(new PlayerJoin(), Capitalism.plugin);
        Capitalism.plugin.getServer().getPluginManager().registerEvents(new PlayerLeave(), Capitalism.plugin);
        Capitalism.plugin.getServer().getPluginManager().registerEvents(new PlaytimeRewards(), Capitalism.plugin);
        PlaytimeRewards.register();

    }
}
