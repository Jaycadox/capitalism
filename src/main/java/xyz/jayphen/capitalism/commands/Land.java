package xyz.jayphen.capitalism.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import xyz.jayphen.capitalism.claims.ClaimLocation;
import xyz.jayphen.capitalism.claims.ClaimManager;
import xyz.jayphen.capitalism.claims.ClaimOffer;
import xyz.jayphen.capitalism.claims.region.RegionManager;
import xyz.jayphen.capitalism.database.player.DatabasePlayer;
import xyz.jayphen.capitalism.helpers.ChatInput;
import xyz.jayphen.capitalism.helpers.InventoryHelper;
import xyz.jayphen.capitalism.helpers.InventoryHelperRunnable;
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
			public boolean run(Player p, Boolean toggle) {
				if (toggle == null) {
					return DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getPermissions().accessWoodDoorsAndTrapdoors;
				}
				DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getPermissions().accessWoodDoorsAndTrapdoors = toggle;
				DatabasePlayer.from(p).getJsonPlayer().save();
				return false;
			}
		});
		table.put("showVisualization", new BooleanRunnable() {
			@Override
			public boolean run(Player p, Boolean toggle) {
				if (toggle == null) {
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
		if (s.equals("allowWoodenDoorAccess")) {
			return "If un-trusted players are allowed to open wooden doors/trapdoors.";
		}
		if (s.equals("showVisualization")) {
			return "Allows you to see your claims boundary.";
		}
		return "";
	}
	
	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
		Claim c = null;
		if (args.length > 0 && args[0].startsWith("id=")) {
			try {
				int              selectedID = Integer.parseInt(args[0].replace("id=", ""));
				ArrayList<Claim> claims     = DatabasePlayer.from(( (Player) commandSender )).getJsonPlayer().getData().claims;
				if (selectedID >= claims.size() || 0 > selectedID) {
					new MessageBuilder("Land").appendCaption("Invalid ID selection").send(commandSender);
					return true;
				}
				c = claims.get(selectedID);
				String[] newArgs = new String[args.length - 1];
				for (int i = 1; i < args.length; i++) {
					newArgs[i - 1] = args[i];
				}
				args = newArgs;
			} catch (Exception ignored) {
				ignored.printStackTrace();
			}
		}
		String[] finalArgs = args;
		Claim    finalC    = c;
		new BukkitRunnable() {
			@Override
			public void run() {
				asyncCommandHandler(commandSender, finalArgs, finalC);
			}
		}.runTaskAsynchronously(Capitalism.plugin);
		return true;
	}
	
	private void asyncCommandHandler(CommandSender commandSender, String[] args, Claim oClaim) {
		if (!( commandSender instanceof Player p )) return;
		Claim cache = oClaim != null ? oClaim : ClaimManager.getCachedClaim(p.getLocation()).orElse(null);
		if (cache == null) {
			new MessageBuilder("Land").appendCaption("You are currently not inside a claimed area of land").send(p);
			return;
		}
		if (!cache.hasPermission(p, Claim.ClaimInteractionType.OWNER)) {
			new MessageBuilder("Land").appendCaption("You do not have permission to view the Landlord menu for this claimed area").send(p);
			return;
		}
		Claim                   c      = ClaimManager.getDatabaseClaim(cache);
		final InventoryScroll[] scroll = { null };
		
		if (args.length == 0) {
			var runnable = new InventoryHelperRunnable() {
				@Override
				public void run(InventoryHelper inv, String ctx) {
					if (ctx.equals("root")) {
						inv.setMargin(0, 0);
						inv.addMargin(0, 0);
						inv.addMargin(0, 1);
						inv.addMargin(0, 2);
						inv.addMargin(0, 3);
						inv.addMargin(0, 6);
						inv.addMargin(0, 7);
						inv.setItem(0, 8, InventoryHelper.getHead(p, "&aView profile", null), () -> {
							InventoryHelper.close(p);
							p.performCommand("profile");
						});
						inv.setItem(0, 1 + 3, ChatColor.YELLOW + "Settings", Material.LODESTONE, () -> {
							inv.push("settings", 1);
						});
						inv.setItem(0, 0 + 3, ChatColor.YELLOW + "Claim Information", Material.PAPER, () -> {
							inv.push("info", 1);
						});
						inv.setItem(0, 2 + 3, ChatColor.YELLOW + "Trusted Players", Material.GOLDEN_PICKAXE, () -> {
							inv.push("trusted", 1);
						});
					}
					if (ctx.equals("destroy")) {
						inv.setMargin(3, 0);
						inv.setItem(0, 0, ChatColor.GREEN + "" + ChatColor.BOLD + "CONFIRM", Material.GREEN_WOOL, () -> {
							ClaimManager.getDatabaseClaim(c).destroy();
							new MessageBuilder("Land").appendCaption("Land claim has been destroyed. Please allow up to").appendVariable("8 seconds")
									.appendCaption("for changes to take effect").send(p);
							InventoryHelper.close(p);
						});
						inv.addMargin(0, 1);
					}
					if (ctx.equals("trusted")) {
						inv.setMargin(1, 0);
						var list = ClaimManager.getDatabaseClaim(c).getTrusted().stream().map(x -> {
							OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(x));
							return new InventoryScroll.ItemRunnable(
									InventoryHelper.getHead(player, ChatColor.YELLOW + player.getName(), List.of("&7Click to: &c&lUN-TRUST PLAYER")),
									() -> {
										DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted()
												.removeIf(pl -> x.equals(player.getUniqueId().toString()));
										DatabasePlayer.from(p).getJsonPlayer().save();
										new MessageBuilder("Land").appendVariable(player.getName())
												.appendCaption("has been removed from the trust list").send(p);
									}
							);
						}).collect(Collectors.toList());
						if (scroll[0] == null) {
							scroll[0] = new InventoryScroll(0, 4, inv, list, ctx, p.getUniqueId());
						} else {
							scroll[0].setView(list);
						}
						scroll[0].render();
						inv.setItem(0, 5, ChatColor.GREEN + "" + ChatColor.BOLD + "ADD TRUSTED USER", Material.GREEN_WOOL, () -> {
							InventoryHelper.close(p);
							ChatInput.createQuery("player name to be trusted", response -> {
								OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(response);
								if (offlinePlayer == null) {
									new MessageBuilder("Land").appendCaption("Could not find that player").send(p);
									return;
								}
								if (DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted()
										.contains(offlinePlayer.getUniqueId().toString()))
								{
									new MessageBuilder("Land").appendCaption("That player is already trusted").send(p);
									return;
								}
								if (offlinePlayer.getUniqueId() == p.getUniqueId()) {
									new MessageBuilder("Land").appendCaption("You cannot trust yourself").send(p);
									return;
								}
								DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted().add(offlinePlayer.getUniqueId().toString());
								DatabasePlayer.from(p).getJsonPlayer().save();
								new MessageBuilder("Land").appendVariable(offlinePlayer.getName())
										.appendCaption("has been added to the trust list").send(p);
								inv.show(p, "trusted");
							}, p);
						});
					}
					if (ctx.equals("selltransfer")) {
						inv.setMargin(1, 0);
						inv.setItem(0, 2, ChatColor.YELLOW + "Sell Claim", Material.GOLD_INGOT, () -> {
							InventoryHelper.close(p);
							ChatInput.createQuery("amount of money to sell for", response -> {
								InventoryHelper.close(p);
								int amount = 0;
								try {
									amount = Integer.parseInt(response);
								} catch (Exception e) {
									new MessageBuilder("Land").appendCaption("Has to be a valid number").send(p);
									return;
								}
								if (1 > amount) {
									new MessageBuilder("Land").appendCaption("Cannot sell for a free or negative price").send(p);
									return;
								}
								int finalAmount = amount;
								ChatInput.createQuery("player name to sell land to", name -> {
									InventoryHelper.close(p);
									OfflinePlayer tPlayer = Bukkit.getOfflinePlayerIfCached(name);
									if (tPlayer == null) {
										new MessageBuilder("Land Transfer").appendCaption("Player doesn't exist").send(p);
										return;
									}
									DatabasePlayer.from(tPlayer.getUniqueId()).getJsonPlayer().getClaimOffers().add(new ClaimOffer(
											new ClaimLocation(c.location.startX, c.location.startZ, c.location.endX, c.location.endZ,
											                  c.location.world
											), finalAmount));
									DatabasePlayer.from(tPlayer.getUniqueId()).getJsonPlayer().save();
									new MessageBuilder("Land").appendCaption("Created offer for")
											.appendVariable("$" + NumberFormatter.addCommas(finalAmount))
											.appendCaption("to").appendVariable(tPlayer.getName()).send(p);
									DatabasePlayer.from(tPlayer.getUniqueId()).getJsonPlayer().queueMessage(
											new MessageBuilder("Land").appendVariable(p.getName())
													.appendCaption("is offering to sell you a land claim, type").appendVariable("/profile")
													.appendCaption("for more info").make());
								}, p);
							}, p);
							inv.push("sell", 1);
						});
						inv.setItem(0, 4, ChatColor.RED + "Transfer Claim", Material.REDSTONE_BLOCK, () -> {
							InventoryHelper.close(p);
							ChatInput.createQuery("player name to transfer claim to", response -> {
								OfflinePlayer tPlayer = Bukkit.getOfflinePlayerIfCached(response);
								if (tPlayer == null) {
									new MessageBuilder("Land Transfer").appendCaption("Player doesn't exist").send(p);
									return;
								}
								String area = "(" + c.location.startX + ", " + c.location.startZ + " -> " + c.location.endX + ", " + c.location.endZ +
								              ")";
								ClaimManager.getDatabaseClaim(c).transfer(tPlayer.getUniqueId());
								new MessageBuilder("Land").appendCaption("You no longer own the land at").appendVariable(area + ".")
										.appendCaption("This is because ownership has been transferred to").appendVariable(tPlayer.getName()).send(p);
								DatabasePlayer.from(tPlayer.getUniqueId()).getJsonPlayer().queueMessage(
										new MessageBuilder("Land").appendCaption("You now own the land at").appendVariable(area + ".")
												.appendCaption("This was free as it has been transferred to you by").appendVariable(p.getName())
												.make());
								
							}, p);
						});
					}
					if (ctx.equals("info")) {
						inv.setMargin(1, 0);
						inv.setItem(0, 0, ChatColor.YELLOW + "Claim Area", Material.GRASS_BLOCK, () -> {},
						            Stream.of("&7This claim's area is: &e" + c.getArea() + " blocks",
						                      "&7Location: &e" + c.getMidpointX() + ", " + c.getMidpointZ(), "&7Type: &e" + RegionManager.getRegion(
												            new Location(Bukkit.getWorld(c.location.world), c.location.startX, 0, c.location.startZ))
										            .toString().toLowerCase()
						            ).map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList())
						);
						
						inv.setItem(0, 5, ChatColor.RED + "Destroy Claim", Material.TNT, () -> {
							inv.push("destroy", 1);
						}, Stream.of("&7This action is &c&lIRREVERSIBLE!", "&4&lYou will NOT receive any money as a result of this")
								            .map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList()));
						if (DatabasePlayer.from(p).getClaimOffersFromClaim(c).isEmpty()) {
							inv.setItem(0, 4, ChatColor.YELLOW + "Sell/Transfer Claim", Material.ENDER_PEARL, () -> {
								inv.push("selltransfer", 1);
							});
						} else {
							ClaimOffer offer = DatabasePlayer.from(p).getClaimOffersFromClaim(c).get(0);
							inv.setItem(0, 4, ChatColor.RED + "Cancel ongoing offer", Material.ENDER_EYE, () -> {
								DatabasePlayer.from(p).deleteAllOffersForClaim(c);
							}, List.of("&7Offer to player: &e" + Bukkit.getOfflinePlayer(DatabasePlayer.getRecipientOfClaimOffer(offer)).getName(),
							           "&7Offered for: &e$" + NumberFormatter.addCommas(offer.price)
							));
						}
						
						
						inv.setItem(0, 1, ChatColor.YELLOW + "Claim Value", Material.GREEN_DYE, () -> {},
						            Stream.of("&7This claim's estimated value is: &a$" + NumberFormatter.addCommas(c.getEstWorth()))
								            .map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList())
						);
						inv.setItem(0, 2, InventoryHelper.getHead(Bukkit.getOfflinePlayer(UUID.fromString(c.owner)), "&eClaim Owner",
						                                          List.of("&7Username: &e" +
						                                                  Bukkit.getOfflinePlayer(UUID.fromString(c.owner)).getName())
						), () -> {});
						inv.setItem(0, 3, ChatColor.YELLOW + "Rename claim", Material.NAME_TAG, () -> {
							InventoryHelper.close(p);
							
							int id = 0;
							for (Claim tC : DatabasePlayer.from(p).getJsonPlayer().getData().claims) {
								if (tC.location.hashCode() == c.location.hashCode()) break;
								id++;
							}
							
							int finalId = id;
							ChatInput.createQuery("new name for claim", response -> {
								
								if (response.length() > 20) {
									new MessageBuilder("Land").appendCaption("Name cannot be over 16 characters").send(p);
									return;
								}
								DatabasePlayer.from(p).getJsonPlayer().getClaim(c).name = response;
								DatabasePlayer.from(p).getJsonPlayer().save();
								p.performCommand("land id=" + finalId);
								
								new MessageBuilder("Land").appendCaption("Land claim's name has been set to:").appendVariable(response).send(p);
							}, p);
						}, List.of());
					}
					if (ctx.equals("settings")) {
						inv.setMargin(1, 0);
						int                              index = 0;
						HashMap<String, BooleanRunnable> prefs = generatePreferenceTable(c);
						for (Map.Entry<String, BooleanRunnable> entry : prefs.entrySet()) {
							boolean value = entry.getValue().run(p, null);
							inv.setItem(0, index++, entry.getKey(), value ? Material.GREEN_WOOL : Material.RED_WOOL, () -> {
								entry.getValue().run(p, !value);
							}, Stream.of("&7Current value: " + ( value ? "&a&lENABLED" : "&c&lDISABLED" ), "", "&e" + getDescription(entry.getKey()))
									            .map(x -> ChatColor.translateAlternateColorCodes('&', x)).collect(Collectors.toList()));
							
						}
						
						
					}
				}
			};
			InventoryHelper inventoryHelper = new InventoryHelper("Landlord > " + ClaimManager.getDatabaseClaim(c).getName(), 1, runnable);
			
			inventoryHelper.show(( (Player) commandSender ));
			return;
		}
		if (args.length >= 1 && args[0].equals("settings")) {
			if (args.length == 1) {
				new MessageBuilder("Land").appendCaption("Name of preference has not been provided").send(p);
				return;
			}
			if (args.length == 2) {
				new MessageBuilder("Land").appendCaption(
						"Value of preference not provided. This can be 'true/yes/on/enable' or 'false/no/off/disable'").send(p);
				return;
			}
			String preferenceName = args[1];
			boolean preferenceToggle = args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("yes") || args[2].equalsIgnoreCase("enable") ||
			                           args[2].equalsIgnoreCase("on");
			HashMap<String, BooleanRunnable> table = generatePreferenceTable(c);
			if (!table.containsKey(preferenceName)) {
				new MessageBuilder("Land").appendCaption("Could not find preference with name:").appendVariable(preferenceName).send(p);
				return;
			}
			table.get(preferenceName).run(p, preferenceToggle);
			new MessageBuilder("Land").appendVariable(preferenceName)
					.appendCaption("has been").appendVariable(preferenceToggle ? "enabled" : "disabled").send(p);
			DatabasePlayer.from(p).getJsonPlayer().save();
		}
		if (args.length >= 1 && args[0].equals("trusted")) {
			if (args.length == 1) {
				new MessageBuilder("Land").appendCaption("Sub argument to 'trusted' not provided").send(p);
				return;
			}
			if (args.length == 2 && args[1].equals("list")) {
				if (c.getTrusted().isEmpty()) {
					new MessageBuilder("Land").appendCaption("This claim has no trusted players").send(p);
					return;
				} else {
					new MessageBuilder("Land").appendCaption("These players have general trust permissions on this claim:").appendList(
							ClaimManager.getDatabaseClaim(c).getTrusted().stream().map(x -> Bukkit.getOfflinePlayer(UUID.fromString(x)).getName())
									.collect(Collectors.toList())).send(p);
					return;
				}
			}
			if (args[1].equals("add")) {
				if (args.length == 2) {
					new MessageBuilder("Land").appendCaption("Player to be trusted is not specified").send(p);
					return;
				}
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(args[2]);
				if (offlinePlayer == null) {
					new MessageBuilder("Land").appendCaption("Could not find player").appendVariable(args[2]).send(p);
					return;
				}
				List<OfflinePlayer> trusted = ClaimManager.getDatabaseClaim(c).getTrusted().stream()
						.map(x -> Bukkit.getOfflinePlayer(UUID.fromString(x))).toList();
				if (trusted.contains(offlinePlayer) || offlinePlayer.getUniqueId() == p.getUniqueId()) {
					new MessageBuilder("Land").appendVariable(offlinePlayer.getName())
							.appendCaption("is on the trust list").send(p);
					return;
				}
				DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted().add(offlinePlayer.getUniqueId().toString());
				DatabasePlayer.from(p).getJsonPlayer().save();
				new MessageBuilder("Land").appendVariable(offlinePlayer.getName())
						.appendCaption("has been added to the trust list").send(p);
			}
			if (args[1].equals("remove")) {
				if (args.length == 2) {
					new MessageBuilder("Land").appendCaption("Player to be removed from the trust list is not specified").send(p);
					return;
				}
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(args[2]);
				if (offlinePlayer == null) {
					new MessageBuilder("Land").appendCaption("Could not find player").appendVariable(args[2]).send(p);
					return;
				}
				List<OfflinePlayer> trusted = ClaimManager.getDatabaseClaim(c).getTrusted().stream()
						.map(x -> Bukkit.getOfflinePlayer(UUID.fromString(x))).toList();
				if (!trusted.contains(offlinePlayer)) {
					new MessageBuilder("Land").appendVariable(offlinePlayer.getName())
							.appendCaption("is not on the trust list").send(p);
					return;
				}
				DatabasePlayer.from(p).getJsonPlayer().getClaim(c).getTrusted().removeIf(x -> x.equals(offlinePlayer.getUniqueId().toString()));
				DatabasePlayer.from(p).getJsonPlayer().save();
				new MessageBuilder("Land").appendVariable(offlinePlayer.getName())
						.appendCaption("has been removed from the trust list").send(p);
			}
			
		}
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 1) {
			return Arrays.asList("settings", "trusted");
		}
		if (args.length == 2 && args[0].equals("settings")) {
			ArrayList<String> prefs = new ArrayList<>();
			for (String s : generatePreferenceTable(null).keySet()) {
				prefs.add(s);
			}
			return prefs;
		} else if (args.length == 2 && args[0].equals("trusted")) {
			return Arrays.asList("list", "add", "remove");
		}
		if (args.length == 3 && args[0].equals("settings")) {
			return Arrays.asList("enable", "disable");
		}
		if (args.length == 3 && args[0].equals("trusted") && args[1].equals("remove")) {
			try {
				return ClaimManager.getCachedClaim(( (Player) sender ).getLocation()).orElse(null).getTrusted().stream()
						.map(x -> Bukkit.getOfflinePlayer(UUID.fromString(x)).getName()).collect(Collectors.toList());
			} catch (Exception ignored) {
			}
			return List.of("");
		}
		return null;
	}
}
