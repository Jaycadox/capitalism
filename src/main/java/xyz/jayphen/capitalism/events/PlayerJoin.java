package xyz.jayphen.capitalism.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;

public class PlayerJoin implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        //if(!Capitalism.eco.db.containsKey(event.getPlayer().getUniqueId()))
        //    Capitalism.eco.db.put(event.getPlayer().getUniqueId(), 1000000.0);
        DatabasePlayer.from(event.getPlayer());
    }
}
