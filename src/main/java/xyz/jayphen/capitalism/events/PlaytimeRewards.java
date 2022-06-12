package xyz.jayphen.capitalism.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.commands.database.Database;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.Token;

import java.util.ArrayList;
import java.util.UUID;

public class PlaytimeRewards implements Listener {
    public static ArrayList<UUID> eligiblePlayers = new ArrayList<>();
    public static ArrayList<UUID> redeemedPlayers = new ArrayList<>();


    public static void register()
    {
        new BukkitRunnable() {
            @Override
            public void run() {
                showReward();
            }
        }.runTaskTimer(Capitalism.plugin, 0, 60 * 25 * 20);
    }
    private static void showReward() {

        for(Player p : Bukkit.getOnlinePlayers()) {
            eligiblePlayers.add(p.getUniqueId());
            p.spigot().sendMessage(new MessageBuilder("Reward")
                    .append(Token.TokenType.CAPTION, "Click this message to redeem your playtime reward. This will expire in 30 seconds")
                    .buildWithCommand("__I_REDEEM_REWARD__")
            );
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player p : Bukkit.getOnlinePlayers()) {
                    if (!redeemedPlayers.contains(p.getUniqueId()) && eligiblePlayers.contains(p.getUniqueId())) {
                        p.sendMessage(new MessageBuilder("Reward").append(Token.TokenType.CAPTION, "Your playtime reward has expired").build());
                    }
                    redeemedPlayers.clear();
                    eligiblePlayers.clear();
                }
            }
        }.runTaskLater(Capitalism.plugin, 30 * 20);
    }
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if(e.getMessage().equals("__I_REDEEM_REWARD__")) {
            e.setCancelled(true);
            if(!redeemedPlayers.contains(e.getPlayer().getUniqueId()) && eligiblePlayers.contains(e.getPlayer().getUniqueId()))
            {
                eligiblePlayers.remove(e.getPlayer().getUniqueId());
                redeemedPlayers.add(e.getPlayer().getUniqueId());

                if(Database.injector.inject(e.getPlayer().getUniqueId(), 100000).getType() == TransactionResult.TransactionResultType.SUCCESS) {
                    e.getPlayer().sendMessage(
                            new MessageBuilder("Reward")
                                    .append(Token.TokenType.VARIABLE, "$100,000")
                                    .append(Token.TokenType.CAPTION, "has been added to your balance")
                                    .build());

                }
            }
        }
    }

}
