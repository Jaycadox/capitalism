package xyz.jayphen.capitalism.commands.admin;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimManager;
import xyz.jayphen.capitalism.claims.region.RegionManager;
import xyz.jayphen.capitalism.database.player.BanManager;
import xyz.jayphen.capitalism.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.injection.EconomyInjector;
import xyz.jayphen.capitalism.economy.transaction.TaxTransaction;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.events.tax.TaxedTransaction;
import xyz.jayphen.capitalism.helpers.TimeHelper;
import xyz.jayphen.capitalism.helpers.WorldEditHelper;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class Admin implements CommandExecutor, TabCompleter {
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
		if (!commandSender.isOp()) return true;
		if (args[0].equals("draftclaim")) {
			LocalSession localSession = WorldEditHelper.getLocalSession((Player) commandSender);
			Region       region;
			try {
				World selectionWorld = localSession.getSelectionWorld();
				if (selectionWorld == null) {
					new MessageBuilder("Admin").appendCaption("You don't have an active WorldEdit selection").send(commandSender);
					return true;
				}
				region = localSession.getSelection(selectionWorld);
			} catch (IncompleteRegionException e) {
				new MessageBuilder("Admin").appendCaption("You don't have an active WorldEdit selection").send(commandSender);
				return true;
			}
			
			ClaimManager.adminDrafts.put(( (Player) commandSender ).getUniqueId(), new Claim(
					new Location(( (Player) commandSender ).getWorld(), region.getBoundingBox().getMinimumPoint().getBlockX(), 0,
					             region.getBoundingBox().getMinimumPoint().getBlockZ()
					), new Location(( (Player) commandSender ).getWorld(), region.getBoundingBox().getMaximumPoint().getBlockX(), 0,
					                region.getBoundingBox().getMaximumPoint().getBlockZ()
			), ( (Player) commandSender ).getUniqueId()));
			Claim claim = ClaimManager.adminDrafts.get(( (Player) commandSender ).getUniqueId());
			
			for (Location loc : claim.getBorderBlocks()) {
				for (int i = 0; i < 255; i++) {
					( (Player) commandSender ).spawnParticle(Particle.REDSTONE, loc.getBlockX() + 0.5, i + 0.1, loc.getBlockZ() + 0.5, 1,
					                                         new Particle.DustOptions(Color.fromBGR(0, 0, 255), 1)
					);
				}
			}
			commandSender.sendMessage("Claim should cost around " + ChatColor.GREEN + "$" + claim.getEstWorth() + ChatColor.WHITE +
			                          " assuming a base cost of $200 per block with $1 added every 30 blocks away it is from spawn.");
			Location loc = new Location(Bukkit.getWorld(claim.location.world), claim.getMidpointX(), 0, claim.getMidpointZ());
			commandSender.sendMessage("Claim is in region: " + RegionManager.getRegion(loc));
		} else if (args[0].equals("removedraft")) {
			ClaimManager.adminDrafts.remove(( (Player) commandSender ).getUniqueId());
			commandSender.sendMessage("Draft removed");
		} else if (args[0].equals("delete") && args.length == 2) {
			OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);
			if (!p.hasPlayedBefore()) {
				commandSender.sendMessage(ChatColor.RED + "Invalid player");
				return true;
			}
			
			DatabasePlayer.from(p.getUniqueId()).delete(false);
			commandSender.sendMessage(ChatColor.GREEN + "Erased account data belonging to " + p.getName());
		} else if (args[0].equals("ban") && args.length > 3) {
			OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);
			if (!p.hasPlayedBefore()) {
				commandSender.sendMessage(ChatColor.RED + "Invalid player");
				return true;
			}
			StringBuilder fArgs = new StringBuilder();
			for (int i = 2; i < args.length; i++) {
				fArgs.append(args[i])
						.append(" ");
			}
			var times = TimeHelper.splitTime(fArgs.toString().trim());
			if (times.size() != 2) {
				commandSender.sendMessage(ChatColor.RED + "Invalid format. It's /admin ban %player% %time% %reason%");
				return true;
			}
			BanManager.applyBan(p, times.get(1), TimeHelper.toTime(times.get(0)));
		} else if (args[0].equals("unban") && args.length == 2) {
			OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);
			if (!p.hasPlayedBefore()) {
				commandSender.sendMessage(ChatColor.RED + "Invalid player");
				return true;
			}
			if (BanManager.removeBan(p)) {
				commandSender.sendMessage(ChatColor.GREEN + "Removed account ban from " + p.getName() + ".");
			} else {
				commandSender.sendMessage(ChatColor.GREEN + "The player " + p.getName() + " isn't banned.");
			}
		} else if (args[0].equals("sell") && args.length == 3) {
			if (!ClaimManager.adminDrafts.containsKey(( (Player) commandSender ).getUniqueId())) {
				commandSender.sendMessage(ChatColor.RED + "No active draft");
				return true;
			}
			
			Claim c = ClaimManager.adminDrafts.get(( (Player) commandSender ).getUniqueId());
			
			Player p = Bukkit.getPlayer(args[1]);
			if (p == null) {
				commandSender.sendMessage(ChatColor.RED + "Invalid player");
				return true;
			}
			int amount;
			try {
				amount = Integer.parseInt(args[2]);
			} catch (Exception ignored) {
				commandSender.sendMessage(ChatColor.RED + "Invalid amount of money");
				return true;
			}
			TaxTransaction    trans = new TaxTransaction(p.getUniqueId(), DatabasePlayer.nonPlayer(EconomyInjector.SERVER).getUuid(), amount);
			TransactionResult res   = trans.transact(TaxedTransaction.INSTANCE, true);
			if (res.getType() != TransactionResult.TransactionResultType.SUCCESS) {
				commandSender.sendMessage(ChatColor.RED + "Transaction failed. Reason given: " + ChatColor.YELLOW + res.getErrorReason());
				return true;
			}
			String area = "(" + c.location.startX + ", " + c.location.startZ + " -> " + c.location.endX + ", " + c.location.endZ + ")";
			commandSender.sendMessage(ChatColor.GREEN + "Sold area " + area + " to " + p.getName() + " for $" + ChatColor.YELLOW + amount);
			
			DatabasePlayer.from(p).getJsonPlayer().getData().claims.add(c);
			DatabasePlayer.from(p).getJsonPlayer().save();
			
			new MessageBuilder("Land").appendCaption("You now own the land at").appendVariable(area + ".")
					.appendVariable("$" + NumberFormatter.addCommas(trans.getTotalAmount()))
					.appendCaption("has been deducted from your account.").send(p);
		} else if (args[0].equals("destroyclaim")) {
			Optional<Claim> optClaim = ClaimManager.getCachedClaim(( (Player) commandSender ).getLocation());
			if (optClaim.isEmpty()) {
				commandSender.sendMessage(ChatColor.RED + "Not currently inside a claim");
				return true;
			}
			Claim claim = optClaim.get();
			commandSender.sendMessage(
					ChatColor.GREEN + "Claim owned by " + ChatColor.YELLOW + Bukkit.getOfflinePlayer(UUID.fromString(claim.owner)).getName() +
					ChatColor.GREEN + " has been destroyed. Please wait up to 8 seconds for claim cache to update.");
			claim.destroy();
			
		} else if (args[0].equals("stats")) {
			if (args.length != 2) {
				commandSender.sendMessage(ChatColor.RED + "Missing 'player' field");
				return true;
			}
			DatabasePlayer dbp;
			String         name = "Server";
			if (args[1].equals("#server")) {
				dbp = DatabasePlayer.nonPlayer(EconomyInjector.SERVER);
			} else {
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
				if (!offlinePlayer.hasPlayedBefore()) {
					commandSender.sendMessage(ChatColor.RED + "Invalid player");
					return true;
				}
				dbp  = DatabasePlayer.from(offlinePlayer);
				name = offlinePlayer.getName();
			}
			
			
			commandSender.sendMessage(ChatColor.GREEN + "--- Stats for " + name + " ---");
			if (BanManager.isNotBanned(dbp)) {
				commandSender.sendMessage(ChatColor.YELLOW + "Ban status: " + ChatColor.GREEN + "Not banned");
				
			} else {
				String timeUntil = TimeHelper.timeToString(dbp.getJsonPlayer().getBannedUntil() - System.currentTimeMillis());
				String reason    = dbp.getJsonPlayer().getBanReason();
				commandSender.sendMessage(
						ChatColor.YELLOW + "Ban status: " + ChatColor.RED + "Banned. Expires in " + timeUntil + ". Reason: " + reason);
				
			}
			int banIndex = 0;
			commandSender.sendMessage(ChatColor.YELLOW + "Ban record:");
			
			for (String ban : dbp.getJsonPlayer().getBanRecord()) {
				
				try {
					String banLength = TimeHelper.timeToString(Long.parseUnsignedLong(ban.split("###")[0]));
					String banReason = ban.split("###")[1];
					if (banReason.contains("s")) continue;
					String banIssued = TimeHelper.timeToString(System.currentTimeMillis() - Long.parseUnsignedLong(ban.split("###")[2]));
					commandSender.sendMessage(ChatColor.GOLD + "  " + ++banIndex + ChatColor.WHITE + ": " + banReason);
					commandSender.sendMessage("    " + ChatColor.WHITE + "Ban length: " + ChatColor.YELLOW + banLength);
					commandSender.sendMessage("    " + ChatColor.WHITE + "Time since issued: " + ChatColor.YELLOW + banIssued);
					commandSender.sendMessage(
							"    " + ChatColor.WHITE + "AntiCheat issued: " + ChatColor.YELLOW + ( banReason.equals("[ac]") ? "Yes" : "No" ));
				} catch (Exception ignored) {
				}
				
			}
			
			commandSender.sendMessage(ChatColor.YELLOW + "Account balance: " + ChatColor.GREEN + "$" + NumberFormatter.addCommas(dbp.getMoneySafe()));
			commandSender.sendMessage(ChatColor.YELLOW + "Amount sent: " + ChatColor.GREEN + "$" +
			                          NumberFormatter.addCommas(dbp.getJsonPlayer().getData().stats.moneySent));
			commandSender.sendMessage(ChatColor.YELLOW + "Amount received: " + ChatColor.GREEN + "$" +
			                          NumberFormatter.addCommas(dbp.getJsonPlayer().getData().stats.moneyRecieved));
			commandSender.sendMessage(ChatColor.YELLOW + "Amount taxed: " + ChatColor.GREEN + "$" +
			                          NumberFormatter.addCommas(dbp.getJsonPlayer().getData().stats.amountTaxed));
			commandSender.sendMessage(ChatColor.YELLOW + "Claims:");
			int id = 0;
			for (Claim c : dbp.getJsonPlayer().getData().claims) {
				commandSender.sendMessage(
						ChatColor.GOLD + "  " + ++id + ChatColor.WHITE + ": Location: " + c.getMidpointX() + ", " + c.getMidpointZ());
				commandSender.sendMessage("    " + ChatColor.WHITE + "Area: " + ChatColor.YELLOW + c.getArea());
				commandSender.sendMessage(
						"    " + ChatColor.WHITE + "Est. Worth: " + ChatColor.GREEN + "$" + NumberFormatter.addCommas(c.getEstWorth()));
				if (commandSender instanceof Player p) {
					p.spigot().sendMessage(new ComponentBuilder(ChatColor.YELLOW + "    [Teleport] ").event(
									new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + c.getMidpointX() + " ~ " + c.getMidpointZ()))
							                       .append(new ComponentBuilder(ChatColor.RED + "[Destroy]").event(
									                       new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/admin destroyclaim")).create()).create());
					
				}
			}
		}
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (!sender.isOp()) return new ArrayList<>();
		if (args.length == 1) {
			return List.of("draftclaim", "stats", "removedraft", "sell", "destroyclaim", "delete", "ban", "unban");
		}
		if (args.length == 2 && ( args[0].equals("stats") || args[0].equals("sell") || args[0].equals("ban") || args[0].equals("unban") )) {
			return Bukkit.getServer().getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toList());
		}
		if (args.length == 3 && args[0].equals("ban")) {
			return List.of("");
		}
		if (args.length == 3 && args[0].equals("sell")) {
			if (!( ClaimManager.adminDrafts.containsKey(( (Player) sender ).getUniqueId()) )) {
				return List.of("No_active_draft");
			}
			Claim claim = ClaimManager.adminDrafts.get(( (Player) sender ).getUniqueId());
			return List.of("" + claim.getEstWorth());
		}
		return null;
	}
}
