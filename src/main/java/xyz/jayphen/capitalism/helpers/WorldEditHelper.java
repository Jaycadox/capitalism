package xyz.jayphen.capitalism.helpers;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.entity.Player;

public class WorldEditHelper {
	public static LocalSession getLocalSession(Player player) {
		com.sk89q.worldedit.entity.Player actor   = BukkitAdapter.adapt(player);
		SessionManager                    manager = WorldEdit.getInstance().getSessionManager();
		return manager.get(actor);
	}
}
