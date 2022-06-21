package xyz.jayphen.capitalism.commands.admin;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimManager;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.economy.injection.EconomyInjector;
import xyz.jayphen.capitalism.economy.transaction.TaxTransaction;
import xyz.jayphen.capitalism.economy.transaction.TransactionResult;
import xyz.jayphen.capitalism.events.tax.TaxedTransaction;
import xyz.jayphen.capitalism.helpers.WorldEditHelper;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;
import xyz.jayphen.capitalism.lang.Token;

import java.util.*;
import java.util.stream.Collectors;

public class Admin implements CommandExecutor, TabCompleter {
	@Override
	public boolean onCommand (CommandSender commandSender, Command command, String s, String[] args) {
		if(!commandSender.isOp()) return true;
		if(args[0].equals("draftclaim")) {
			LocalSession localSession = WorldEditHelper.getLocalSession((Player)commandSender);
			Region region = null;
			try {
				World selectionWorld = localSession.getSelectionWorld();
				if (selectionWorld == null) {
					commandSender.sendMessage(new MessageBuilder("Admin").append(Token.TokenType.CAPTION, "You don't have an active WorldEdit selection").build());
					return true;
				}
				region = localSession.getSelection(selectionWorld);
			} catch (IncompleteRegionException e) {
				commandSender.sendMessage(new MessageBuilder("Admin").append(Token.TokenType.CAPTION, "You don't have an active WorldEdit selection").build());
				return true;
			}

			ClaimManager.adminDrafts.put(((Player) commandSender).getUniqueId(), new Claim(new Location(
					((Player)commandSender).getWorld(),
					region.getBoundingBox().getMinimumPoint().getBlockX(),
					0,
					region.getBoundingBox().getMinimumPoint().getBlockZ()
			), new Location(
					((Player)commandSender).getWorld(),
					region.getBoundingBox().getMaximumPoint().getBlockX(),
					0,
					region.getBoundingBox().getMaximumPoint().getBlockZ()
			), ((Player)commandSender).getUniqueId()));
			Claim claim = ClaimManager.adminDrafts.get(((Player) commandSender).getUniqueId());

			for(Location loc : claim.getBorderBlocks()) {
				((Player)commandSender).spawnParticle(Particle.REDSTONE, loc.getBlockX() + 0.5, loc.getBlockY() + 0.1, loc.getBlockZ() + 0.5, 1, new Particle.DustOptions(Color.fromBGR(0, 0, 255), 1));
			}
			commandSender.sendMessage("Claim should cost around " + ChatColor.GREEN + "$" + (claim.getArea() * (200 + (claim.getDistanceFromSpawn() / 30)))
					                          + ChatColor.WHITE + " assuming a base cost of $200 per block with $1 added every 30 blocks away it is from spawn.");
		} else if(args[0].equals("removedraft")) {
			ClaimManager.adminDrafts.remove(((Player)commandSender).getUniqueId());
			commandSender.sendMessage("Draft removed");
		} else if(args[0].equals("sell") && args.length == 3) {
			if(!ClaimManager.adminDrafts.containsKey(((Player)commandSender).getUniqueId())) {
				commandSender.sendMessage(ChatColor.RED + "No active draft");
				return true;
			}

			Claim c = ClaimManager.adminDrafts.get(((Player)commandSender).getUniqueId());

			Player p = Bukkit.getPlayer(args[1]);
			if(p == null) {
				commandSender.sendMessage(ChatColor.RED + "Invalid player");
				return true;
			}
			int amount = 0;
			try {
				amount = Integer.parseInt(args[2]);
			} catch(Exception ignored) {
				commandSender.sendMessage(ChatColor.RED + "Invalid amount of money");
				return true;
			}
			TaxTransaction trans = new TaxTransaction(p.getUniqueId(), DatabasePlayer.nonPlayer(EconomyInjector.SERVER).getUuid(), amount);
			TransactionResult res = trans.transact(TaxedTransaction.INSTANCE, true);
			if(res.getType() != TransactionResult.TransactionResultType.SUCCESS) {
				commandSender.sendMessage(ChatColor.RED + "Transaction failed. Reason given: " + ChatColor.YELLOW + res.getErrorReason());
				return true;
			}
			String area = "(" + c.location.startX + ", " + c.location.startZ + " -> " + c.location.endX + ", " + c.location.endZ + ")";
			commandSender.sendMessage(ChatColor.GREEN + "Sold area " + area + " to " + p.getName() + " for $" + ChatColor.YELLOW + amount);

			DatabasePlayer.from(p).getJsonPlayer().getData().claims.add(c);
			DatabasePlayer.from(p).getJsonPlayer().save();

			p.sendMessage(new MessageBuilder("Land").appendCaption("You now own the land at")
					              .appendVariable(area + ".")
					              .appendVariable("$" + NumberFormatter.addCommas(trans.getTotalAmount()))
					              .appendCaption("has been deducted from your account.")
					              .build());
		} else if(args[0].equals("destroyclaim")) {
			Optional<Claim> optClaim = ClaimManager.getCachedClaim(((Player)commandSender).getLocation());
			if(optClaim.isEmpty()) {
				commandSender.sendMessage(ChatColor.RED + "Not currently inside a claim");
				return true;
			}
			Claim claim = optClaim.get();
			commandSender.sendMessage(ChatColor.GREEN + "Claim owned by "
					                          + ChatColor.YELLOW
					                          + Bukkit.getOfflinePlayer(UUID.fromString(claim.owner)).getName()
					                          + ChatColor.GREEN
					                          + " has been destroyed. Please wait up to 8 seconds for claim cache to update.");
			claim.destroy();

		}

			return true;
	}

	@Override
	public List<String> onTabComplete (CommandSender sender, Command command, String label, String[] args) {
		if(!sender.isOp()) return new ArrayList<>();
		if(args.length == 1) {
			return Arrays.asList("draftclaim", "stats", "removedraft", "sell", "destroyclaim");
		}
		if(args.length == 2 && (args[0].equals("stats") || args[0].equals("sell"))) {
			return Bukkit.getServer().getOnlinePlayers().stream().map(x -> x.getName()).collect(Collectors.toList());
		}
		if(args.length == 3 && args[0].equals("sell")) {
			if(!(ClaimManager.adminDrafts.containsKey(((Player)sender).getUniqueId()))) {
				return Arrays.asList("No_active_draft");
			}
			Claim claim = ClaimManager.adminDrafts.get(((Player)sender).getUniqueId());
			return Arrays.asList("" + (claim.getArea() * (200 + (claim.getDistanceFromSpawn() / 30))));
		}
		return new ArrayList<>();
	}
}
