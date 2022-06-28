package xyz.jayphen.capitalism.database.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.ServerOperator;
import org.jetbrains.annotations.NotNull;
import xyz.jayphen.capitalism.helpers.TimeHelper;

import java.util.Objects;

public class BanManager {
	public static void applyBan(OfflinePlayer p, String reason, long length) {
		var dbp = DatabasePlayer.from(p).getJsonPlayer();
		reason = reason.replace(" [wipe]", "[wipe]").replace("[wipe] ", "[wipe]");
		
		DatabasePlayer.from(p).getJsonPlayer().getBanRecord().add(length + "###" + reason + "###" + System.currentTimeMillis());
		DatabasePlayer.from(p).getJsonPlayer().save();
		boolean shouldWipe = reason.startsWith("[wipe]");
		
		if (shouldWipe) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "admin delete " + p.getName());
		}
		dbp.getData().bannedUntil = System.currentTimeMillis() + length;
		dbp.getData().banReason   = reason.replace("[wipe]", "");
		dbp.save();
		
		Player onlinePlayer = p.getPlayer();
		if (onlinePlayer != null && onlinePlayer.isOnline()) {
			onlinePlayer.kick(getBanMessage(DatabasePlayer.from(p), length));
		}
		var message = Component.text("Applied account ban to ", NamedTextColor.GREEN)
				.append(Component.text(Objects.requireNonNull(p.getName()), NamedTextColor.YELLOW))
				.append(Component.text(". Reason given: ", NamedTextColor.GREEN))
				.append(Component.text(reason, NamedTextColor.YELLOW))
				.append(Component.text(". Time until unban: ", NamedTextColor.GREEN))
				.append(Component.text(TimeHelper.timeToString(length), NamedTextColor.YELLOW));
		
		Bukkit.getOnlinePlayers().stream().filter(ServerOperator::isOp).forEach(x -> x.sendMessage(message));
		Bukkit.getConsoleSender().sendMessage(message);
		
	}
	
	public static boolean removeBan(OfflinePlayer p) {
		if (isNotBanned(DatabasePlayer.from(p))) return false;
		DatabasePlayer.from(p).getJsonPlayer().getData().bannedUntil = (long) -1;
		DatabasePlayer.from(p).getJsonPlayer().getData().banReason   = "";
		DatabasePlayer.from(p).getJsonPlayer().save();
		return true;
	}
	
	public static boolean isNotBanned(DatabasePlayer p) {
		return p.getJsonPlayer().getData().bannedUntil == -1 || ( p.getJsonPlayer().getData().bannedUntil - System.currentTimeMillis() ) < 0;
	}
	
	public static String getFormalBanReason(DatabasePlayer dbp) {
		String reason = dbp.getJsonPlayer().getBanReason().replace("[wipe]", "");
		if (!( reason.equals("[ac]") )) return reason;
		return "Unfair advantage";
	}
	public static int getAnticheatInfractions(DatabasePlayer p) {
		return p.getJsonPlayer().getBanRecord().stream().filter(x -> x.contains("[ac]")).toList().size();
	}
	public static long getAutobanTime(OfflinePlayer p) {
		var infractionCount = getAnticheatInfractions(DatabasePlayer.from(p));
		if(infractionCount == 0) {
			return TimeHelper.toTime("1d");
		}
		if(infractionCount == 1) {
			return TimeHelper.toTime("3d");
		}
		if(infractionCount == 2) {
			return TimeHelper.toTime("7d");
		}
		if(infractionCount == 3) {
			return TimeHelper.toTime("14d");
		}
		if(infractionCount == 4) {
			return TimeHelper.toTime("30d");
		}
		if(infractionCount == 5) {
			return TimeHelper.toTime("99999d");
		}
		return 0;
	}
	@NotNull
	public static Component getBanMessage(DatabasePlayer dbp, long timeLeft) {
		String formattedTimeLeft = TimeHelper.timeToString(timeLeft);
		return MiniMessage.miniMessage().deserialize(
				"[CAPITALISM SMP]<newline><color:red><bold>INFRACTION NOTICE</bold></color><newline><newline><yellow>\"" + getFormalBanReason(dbp) +
				"\"<newline><newline><reset>Expires in <yellow>" + formattedTimeLeft + "<newline><newline>" +
				( dbp.getJsonPlayer().getBanReason().contains("[wipe]")
				  ? "<grey>In addition, your player data has been reset<newline><newline><reset>" : "" ) +
				( dbp.getJsonPlayer().getBanReason().equals("[ac]")
				  ? "<grey>This action was performed automatically, with no human involvement<newline><newline><reset>" : "" ) +
				"<gray>If you wish to appeal this infraction, please message <yellow>jayphen#6666<gray> on Discord");
	}
}

