package xyz.jayphen.capitalism.commands;

import net.md_5.bungee.api.ChatColor;
import org.apache.logging.log4j.message.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import xyz.jayphen.capitalism.helpers.ChatInput;
import xyz.jayphen.capitalism.helpers.ChatQueryRunnable;
import xyz.jayphen.capitalism.helpers.InventoryHelper;
import xyz.jayphen.capitalism.helpers.InventoryScroll;
import xyz.jayphen.capitalism.lang.MessageBuilder;
import xyz.jayphen.capitalism.lang.NumberFormatter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Land implements CommandExecutor, TabCompleter {
	public static HashMap<String, BooleanRunnable> generatePreferenceTable(Claim c) {
		HashMap<String, BooleanRunnable> table = new HashMap<>();
		table.put("allowWoodenDoorAccess", new BooleanRunnable() {
			@Override
			public boolean run (Player p, Boolean toggle) {
				if(toggle == null) {
					return DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getPermissions().accessWoodDoorsAndTrapdoors;
				}
				DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getPermissions().accessWoodDoorsAndTrapdoors = toggle;
				DatabasePlayer.from(p).getJsonPlayer().save();
				return false;
			}
		});
		table.put("showVisualization", new BooleanRunnable() {
			@Override
			public boolean run (Player p, Boolean toggle) {
				if(toggle == null) {
					return DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getPermissions().visualize;
				}
				DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getPermissions().visualize = toggle;
				DatabasePlayer.from(p).getJsonPlayer().save();
				return false;
			}
		});
		return table;
	}
	public static String getDescription(String s) {
		if(s.equals("allowWoodenDoorAccess")) {
			return "If un-trusted players are allowed to open wooden doors/trapdoors.";
		}
		if(s.equals("showVisualization")) {
			return "Allows you to see your claims boundary.";
		}
		return "";
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

	private void asyncCommandHandler (CommandSender commandSender, String[] args) {
		if(!(commandSender instanceof Player p)) return;

		Claim c = ClaimManager.getDatabaseClaim(ClaimManager.getCachedClaim(p.getLocation()).orElse(null));
		if(c == null) {
			p.sendMessage(new MessageBuilder("Land").appendCaption("You are currently not inside a claimed area of land").build());
			return;
		}
		if(!c.hasPermission(p, Claim.ClaimInteractionType.OWNER)) {
			p.sendMessage(new MessageBuilder("Land").appendCaption("You do not have permission to view the Landlord menu for this claimed area").build());
			return;
		}

		if(args.length == 0) {
			InventoryHelper inventoryHelper = new InventoryHelper("Landlord", 1, (inv, ctx) -> {
				if(ctx.equals("root")) {
					inv.setMargin(3, 0);
					inv.setItem(0, 1, ChatColor.YELLOW + "Settings", Material.LODESTONE, () -> {
						inv.push("settings", 1);
					});
					inv.setItem(0, 0, ChatColor.YELLOW + "Claim Information", Material.PAPER, () -> {
						inv.push("info", 1);
					});
					inv.setItem(0, 2, ChatColor.YELLOW + "Trusted Players", Material.GOLDEN_PICKAXE, () -> {
						inv.push("trusted", 1);
					});
				}
				if(ctx.equals("destroy")) {
					inv.setMargin(3, 0);
					inv.setItem(0, 0, ChatColor.GREEN + "" + ChatColor.BOLD + "CONFIRM", Material.GREEN_WOOL, () -> {
						ClaimManager.getDatabaseClaim(c).destroy();
						p.sendMessage(
								new MessageBuilder("Land").appendCaption("Land claim has been destroyed. Please allow up to")
										.appendVariable("8 seconds")
										.appendCaption("for changes to take effect")
										.build()
						);
						p.closeInventory();
					});
					inv.addMargin(0, 1);
				}
				if(ctx.equals("trusted")) {
					inv.setMargin(1, 0);
					InventoryScroll scroll = InventoryScroll.get(0, 4, inv, ClaimManager.getDatabaseClaim(c).getTrusted().stream().map(x -> {
						OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(x));
						return new InventoryScroll.ItemRunnable(
							InventoryHelper.getHead(player, ChatColor.YELLOW + player.getName(), List.of(
									"&7Click to: &c&lUN-TRUST PLAYER"
							))
							, () -> {
								DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted().removeIf(pl -> x.equals(player.getUniqueId().toString()));
								DatabasePlayer.from(p).getJsonPlayer().save();
								p.sendMessage(new MessageBuilder("Land").appendVariable(player.getName()).appendCaption("has been removed from the trust list").build());
							});
						}).collect(Collectors.toList())
					);
					scroll.render();
					inv.setItem(0, 5, ChatColor.GREEN + "" + ChatColor.BOLD + "ADD TRUSTED USER", Material.GREEN_WOOL, () -> {
						p.closeInventory();
						ChatInput.createQuery("player name to be trusted", response -> {
							OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(response);
							if(!offlinePlayer.hasPlayedBefore()) {
								p.sendMessage(new MessageBuilder("Land").appendCaption("Could not find that player").build());
								return;
							}
							if(DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted().contains(offlinePlayer.getUniqueId().toString())) {
								p.sendMessage(new MessageBuilder("Land").appendCaption("That player is already trusted").build());
								return;
							}
							if(offlinePlayer.getUniqueId() == p.getUniqueId()) {
								p.sendMessage(new MessageBuilder("Land").appendCaption("You cannot trust yourself").build());
								return;
							}
							DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted().add(offlinePlayer.getUniqueId().toString());
							DatabasePlayer.from(p).getJsonPlayer().save();
							p.sendMessage(new MessageBuilder("Land").appendVariable(offlinePlayer.getName()).appendCaption("has been added to the trust list").build());
							inv.show(p, "trusted");
						}, p);
					});
				}
				if(ctx.equals("info")) {
					inv.setMargin(1, 0);
					inv.setItem(0, 0, ChatColor.YELLOW + "Claim Area", Material.GRASS_BLOCK, () -> {},
					            Stream.of("&7This claim's area is: &e" + c.getArea() + " blocks")
							            .map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList()));

					inv.setItem(0, 5, ChatColor.RED + "Destroy Claim", Material.TNT, () -> {
						inv.push("destroy", 1);
					}, Stream.of("&7This action is &c&lIRREVERSIBLE!", "&4&lYou will NOT recieve any money as a result of this")
							           .map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList()));

					inv.setItem(0, 1, ChatColor.YELLOW + "Claim Value", Material.GREEN_DYE, () -> {},
					            Stream.of("&7This claim's estimated value is: &a$" + NumberFormatter.addCommas(c.getEstWorth()))
							            .map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList()));
					inv.setItem(0, 2, InventoryHelper.getHead(Bukkit.getOfflinePlayer(UUID.fromString(c.owner)), "&eClaim Owner",
					                                          List.of("&7Username: &e" + Bukkit.getOfflinePlayer(UUID.fromString(c.owner)).getName())
					), () -> {});
				}
				if(ctx.equals("settings")) {
					inv.setMargin(1, 0);
					int index = 0;
					HashMap<String, BooleanRunnable> prefs = generatePreferenceTable(c);
					for(Map.Entry<String, BooleanRunnable> entry : prefs.entrySet()) {
						boolean value = entry.getValue().run(p, null);
						inv.setItem(0, index++, entry.getKey(), value ? Material.GREEN_WOOL : Material.RED_WOOL, () -> {
							entry.getValue().run(p, !value);
						}, Stream.of("&7Current value: " + (value ? "&a&lENABLED" : "&c&lDISABLED"),
						             "",
						             "&e" + getDescription(entry.getKey())
						).map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList()));

					}


				}
			});

			inventoryHelper.show(((Player)commandSender));
			return;
		}
		if(args.length >= 1 && args[0].equals("settings")) {
			if(args.length == 1) {
				p.sendMessage(new MessageBuilder("Land").appendCaption("Name of preference has not been provided").build());
				return;
			}
			if(args.length == 2) {
				p.sendMessage(new MessageBuilder("Land").appendCaption("Value of preference not provided. This can be 'true/yes/on/enable' or 'false/no/off/disable'").build());
				return;
			}
			String preferenceName = args[1];
			boolean preferenceToggle = args[2].equalsIgnoreCase("true")
					|| args[2].equalsIgnoreCase("yes")
					|| args[2].equalsIgnoreCase("enable")
					|| args[2].equalsIgnoreCase("on");
			HashMap<String, BooleanRunnable> table = generatePreferenceTable(c);
			if(!table.containsKey(preferenceName)) {
				p.sendMessage(new MessageBuilder("Land").appendCaption("Could not find preference with name:").appendVariable(preferenceName).build());
				return;
			}
			table.get(preferenceName).run(p, preferenceToggle);
			p.sendMessage(new MessageBuilder("Land").appendVariable(preferenceName)
					              .appendCaption("has been").appendVariable(preferenceToggle ? "enabled" : "disabled").build());
			DatabasePlayer.from(p).getJsonPlayer().save();
		}
		if(args.length >= 1 && args[0].equals("trusted")) {
			if(args.length == 1) {
				p.sendMessage(new MessageBuilder("Land").appendCaption("Sub argument to 'trusted' not provided").build());
				return;
			}
			if(args.length == 2 && args[1].equals("list")) {
				if(c.getTrusted().isEmpty()) {
					p.sendMessage(
							new MessageBuilder("Land").appendCaption("This claim has no trusted players").build()
					);
					return;
				} else {
					p.sendMessage(
							new MessageBuilder("Land").appendCaption("These players have general trust permissions on this claim:")
									.appendList(ClaimManager.getDatabaseClaim(c).getTrusted().stream().map(x -> Bukkit.getOfflinePlayer(UUID.fromString(x)).getName()).collect(Collectors.toList())).build()
					);
					return;
				}
			}
			if(args[1].equals("add")) {
				if(args.length == 2) {
					p.sendMessage(new MessageBuilder("Land").appendCaption("Player to be trusted is not specified").build());
					return;
				}
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);
				if(!offlinePlayer.hasPlayedBefore()) {
					p.sendMessage(new MessageBuilder("Land").appendCaption("Could not find player").appendVariable(args[2]).build());
					return;
				}
				List<OfflinePlayer> trusted = ClaimManager.getDatabaseClaim(c).getTrusted().stream().map(x -> Bukkit.getOfflinePlayer(UUID.fromString(x))).toList();
				if(trusted.contains(offlinePlayer) || offlinePlayer.getUniqueId() == p.getUniqueId()) {
					p.sendMessage(new MessageBuilder("Land").appendVariable(offlinePlayer.getName()).appendCaption("is on the trust list").build());
					return;
				}
				DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted().add(offlinePlayer.getUniqueId().toString());
				DatabasePlayer.from(p).getJsonPlayer().save();
				p.sendMessage(new MessageBuilder("Land").appendVariable(offlinePlayer.getName()).appendCaption("has been added to the trust list").build());
			}
			if(args[1].equals("remove")) {
				if(args.length == 2) {
					p.sendMessage(new MessageBuilder("Land").appendCaption("Player to be removed from the trust list is not specified").build());
					return;
				}
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);
				if(!offlinePlayer.hasPlayedBefore()) {
					p.sendMessage(new MessageBuilder("Land").appendCaption("Could not find player").appendVariable(args[2]).build());
					return;
				}
				List<OfflinePlayer> trusted = ClaimManager.getDatabaseClaim(c).getTrusted().stream().map(x -> Bukkit.getOfflinePlayer(UUID.fromString(x))).toList();
				if(!trusted.contains(offlinePlayer)) {
					p.sendMessage(new MessageBuilder("Land").appendVariable(offlinePlayer.getName()).appendCaption("is not on the trust list").build());
					return;
				}
				DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted().removeIf(x -> x.equals(offlinePlayer.getUniqueId().toString()));
				DatabasePlayer.from(p).getJsonPlayer().save();
				p.sendMessage(new MessageBuilder("Land").appendVariable(offlinePlayer.getName()).appendCaption("has been removed from the trust list").build());
			}

		}
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
