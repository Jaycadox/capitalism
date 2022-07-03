package xyz.jayphen.capitalism.commands;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.claims.ClaimManager;
import xyz.jayphen.capitalism.claims.region.RegionManager;
import xyz.jayphen.capitalism.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;

import java.util.HashMap;
import java.util.UUID;

public class Hud implements CommandExecutor {
	
	private static final HashMap<UUID, ScoreState> states     = new HashMap<>();
	private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.builder().build();
	private static final Scoreboard                empty      = Bukkit.getScoreboardManager().getNewScoreboard();
	
	public Hud() {
		Capitalism.plugin.getServer().getScheduler().runTaskTimer(Capitalism.plugin, this::tick, 20, 20);
		
		
	}
	
	private void tick() {
		states.forEach((u, s) -> {
			var player = Bukkit.getPlayer(u);
			if (player == null) return;
			update(player, s.o);
		});
	}
	
	private void update(Player p, Objective o) {
		states.get(p.getUniqueId()).board.getObjectives().forEach(Objective::unregister);
		o = states.get(p.getUniqueId()).board.registerNewObjective(p.getUniqueId() + "-obj", "dummy",
		                                                           Component.text("Capitalism (mc.jayphen.xyz)", TextColor.color(0xeeeeee)).decorate(TextDecoration.BOLD)
		);
		String line1 = serializer.serialize(Component.text("Region > ", TextColor.color(0xddddff)).decorate(TextDecoration.BOLD)
				                                    .append(Component.text(
						                                    RegionManager.getRegion(p.getLocation()).toString().toUpperCase(),
						                                    NamedTextColor.YELLOW
				                                    )));
		o.getScore(line1).setScore(2);
		
		String line2 = serializer.serialize(Component.text("Balance > ", TextColor.color(0xddddff)).decorate(TextDecoration.BOLD)
				                                    .append(Component.text(
						                                    "$" + NumberFormatter.addCommas(DatabasePlayer.from(p).getMoneySafe()),
						                                    NamedTextColor.YELLOW
				                                    )));
		o.getScore(line2).setScore(1);
		var claimOwner = "Not inside claim";
		var claim      = ClaimManager.getCachedClaim(p.getLocation()).orElse(null);
		if (claim != null) {
			claimOwner = Bukkit.getOfflinePlayer(UUID.fromString(claim.owner)).getName();
		}
		String line3 = serializer.serialize(Component.text("Claim > ", TextColor.color(0xddddff)).decorate(TextDecoration.BOLD)
				                                    .append(Component.text(claimOwner, NamedTextColor.YELLOW)));
		o.getScore(line3).setScore(3);
		
		String line4 = serializer.serialize(Component.text("Server running Capitalism v" + Capitalism.VERSION, NamedTextColor.DARK_GRAY));
		o.getScore(line4).setScore(0);
		
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		
		p.setScoreboard(!states.get(p.getUniqueId()).open[0] ? empty : states.get(p.getUniqueId()).board);
	}
	
	private void generateScoreboard(Player p) {
		var board = Bukkit.getScoreboardManager().getNewScoreboard();
		var objective = board.registerNewObjective(p.getUniqueId() + "-obj", "dummy",
		                                           Component.text("Capitalism HUD", TextColor.color(0xeeeeee))
		);
		states.put(p.getUniqueId(), new ScoreState(board, objective, new Boolean[] { false }));
		update(p, objective);
	}
	
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!( sender instanceof Player p )) return true;
		if (!states.containsKey(p.getUniqueId())) {
			p.sendMessage(Component.text("This feature is currently ", NamedTextColor.GRAY)
					              .append(Component.text("EXPERIMENTAL", NamedTextColor.RED).decorate(TextDecoration.BOLD))
					              .append(Component.text(". Please report any issues found.", NamedTextColor.GRAY))
			);
			
			
			generateScoreboard(p);
		}
		states.get(p.getUniqueId()).open[0] = !states.get(p.getUniqueId()).open[0];
		new MessageBuilder("Capitalism").appendCaption("Your HUD has been")
				.appendVariable(states.get(p.getUniqueId()).open[0] ? "enabled" : "disabled").send(p);
		return true;
	}
	
	private record ScoreState(Scoreboard board, Objective o, Boolean[] open) {}
}
