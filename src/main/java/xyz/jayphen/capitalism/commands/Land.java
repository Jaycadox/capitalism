package xyz.jayphen.capitalism.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jayphen.capitalism.Capitalism;
import xyz.jayphen.capitalism.claims.Claim;
import xyz.jayphen.capitalism.claims.ClaimManager;
import xyz.jayphen.capitalism.commands.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.lang.MessageBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class Land implements CommandExecutor, TabCompleter {
	public static HashMap<String, BooleanRunnable> generatePreferenceTable(Claim c) {
		HashMap<String, BooleanRunnable> table = new HashMap<>();
		table.put("allowWoodenDoorAccess", new BooleanRunnable() {
			@Override
			public void run (boolean toggle) {
				c.getPermissions().accessWoodDoorsAndTrapdoors = toggle;
			}
		});
		return table;
	}


	@Override
	public boolean onCommand (CommandSender commandSender, Command command, String s, String[] args)
	{
		new BukkitRunnable() {
			@Override
			public void run () {
				asyncCommandHandler(commandSender, args);
			}
		}.runTaskAsynchronously(Capitalism.plugin);
		return true;
	}

	private boolean asyncCommandHandler (CommandSender commandSender, String[] args) {
		if(!(commandSender instanceof Player p)) return true;

		Claim c = ClaimManager.getCachedClaim(p.getLocation()).orElse(null);
		if(c == null) {
			p.sendMessage(new MessageBuilder("Land").appendCaption("You are currently not inside a claimed area of land").build());
			return true;
		}
		if(!c.hasPermission(p, Claim.ClaimInteractionType.GENERAL)) {
			p.sendMessage(new MessageBuilder("Land").appendCaption("You do not have permission to view the Landlord menu for this claimed area").build());
			return true;
		}

		if(args.length >= 1 && args[0].equals("settings")) {
			if(args.length == 1) {
				p.sendMessage(new MessageBuilder("Land").appendCaption("Name of preference has not been provided").build());
				return true;
			}
			if(args.length == 2) {
				p.sendMessage(new MessageBuilder("Land").appendCaption("Value of preference not provided. This can be 'true/yes/on/enable' or 'false/no/off/disable'").build());
				return true;
			}
			String preferenceName = args[1];
			boolean preferenceToggle = args[2].equalsIgnoreCase("true")
					|| args[2].equalsIgnoreCase("yes")
					|| args[2].equalsIgnoreCase("enable")
					|| args[2].equalsIgnoreCase("on");
			HashMap<String, BooleanRunnable> table = generatePreferenceTable(c);
			if(!table.containsKey(preferenceName)) {
				p.sendMessage(new MessageBuilder("Land").appendCaption("Could not find preference with name:").appendVariable(preferenceName).build());
				return true;
			}
			table.get(preferenceName).run(preferenceToggle);
			p.sendMessage(new MessageBuilder("Land").appendVariable(preferenceName)
					              .appendCaption("has been").appendVariable(preferenceToggle ? "enabled" : "disabled").build());
			DatabasePlayer.from(p).getJsonPlayer().save();
		}
		if(args.length >= 1 && args[0].equals("trusted")) {
			if(args.length == 1) {
				p.sendMessage(new MessageBuilder("Land").appendCaption("Sub argument to 'trusted' not provided").build());
				return true;
			}
			if(args.length == 2 && args[1].equals("list")) {
				if(c.getTrusted().isEmpty()) {
					p.sendMessage(
							new MessageBuilder("Land").appendCaption("This claim has no trusted players").build()
					);
					return true;
				} else {
					p.sendMessage(
							new MessageBuilder("Land").appendCaption("These players have general trust permissions on this claim:")
									.appendList(c.getTrusted().stream().map(x -> Bukkit.getOfflinePlayer(UUID.fromString(x)).getName()).collect(Collectors.toList())).build()
					);
					return true;
				}
			}
			if(args[1].equals("add")) {
				if(args.length == 2) {
					p.sendMessage(new MessageBuilder("Land").appendCaption("Player to be trusted is not specified").build());
					return true;
				}
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);
				if(!offlinePlayer.hasPlayedBefore()) {
					p.sendMessage(new MessageBuilder("Land").appendCaption("Could not find player").appendVariable(args[2]).build());
					return true;
				}
				List<OfflinePlayer> trusted = c.getTrusted().stream().map(x -> Bukkit.getOfflinePlayer(UUID.fromString(x))).toList();
				if(trusted.contains(offlinePlayer)) {
					p.sendMessage(new MessageBuilder("Land").appendVariable(offlinePlayer.getName()).appendCaption("is on the trust list").build());
					return true;
				}
				DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted().add(offlinePlayer.getUniqueId().toString());
				DatabasePlayer.from(p).getJsonPlayer().save();
				p.sendMessage(new MessageBuilder("Land").appendVariable(offlinePlayer.getName()).appendCaption("has been added to the trust list").build());
			}
			if(args[1].equals("remove")) {
				if(args.length == 2) {
					p.sendMessage(new MessageBuilder("Land").appendCaption("Player to be removed from the trust list is not specified").build());
					return true;
				}
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);
				if(!offlinePlayer.hasPlayedBefore()) {
					p.sendMessage(new MessageBuilder("Land").appendCaption("Could not find player").appendVariable(args[2]).build());
					return true;
				}
				List<OfflinePlayer> trusted = c.getTrusted().stream().map(x -> Bukkit.getOfflinePlayer(UUID.fromString(x))).toList();
				if(!trusted.contains(offlinePlayer)) {
					p.sendMessage(new MessageBuilder("Land").appendVariable(offlinePlayer.getName()).appendCaption("is not on the trust list").build());
					return true;
				}
				DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted().removeIf(x -> x.equals(offlinePlayer.getUniqueId().toString()));
				DatabasePlayer.from(p).getJsonPlayer().save();
				p.sendMessage(new MessageBuilder("Land").appendVariable(offlinePlayer.getName()).appendCaption("has been removed from the trust list").build());
			}

		}
		return true;
	}

	@Override
	public List<String> onTabComplete (CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 1) {
			return Arrays.asList("settings", "trusted");
		}
		if(args.length == 2 && args[0].equals("settings")) {
			ArrayList<String> prefs = new ArrayList<>();
			for(String s : generatePreferenceTable(null).keySet()) {
				prefs.add(s);
			}
			return prefs;
		} else if(args.length == 2 && args[0].equals("trusted")) {
			return Arrays.asList("list", "add", "remove");
		}
		if(args.length == 3 && args[0].equals("settings")) {
			return Arrays.asList("enable", "disable");
		}
		if(args.length == 3 && args[0].equals("trusted") && args[1].equals("remove")) {
			try {
				return ClaimManager.getCachedClaim(((Player)sender).getLocation()).orElse(null).getTrusted().stream().map(
						x -> Bukkit.getOfflinePlayer(UUID.fromString(x)).getName()
				).collect(Collectors.toList());
			} catch(Exception ignored) {}
			return List.of("");
		}
		return null;
	}
}
abstract class BooleanRunnable {
	public abstract void run(boolean toggle);
}