package net.netcoding.niftychat.listeners;

import java.sql.SQLException;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftyranks.events.RankChangeEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

public class Move extends BukkitListener {

	public Move(NiftyChat plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent event) {
		MojangProfile profile = NiftyBukkit.getMojangRepository().searchByExactPlayer(event.getPlayer());
		UserChatData userData = UserChatData.getCache(profile.getUniqueId());
		if (userData != null) userData.setMoved();
	}

	@EventHandler
	public void onRankChangeEvent(RankChangeEvent event) {
		MojangProfile profile = NiftyBukkit.getMojangRepository().searchByExactPlayer(event.getPlayer());
		UserChatData userData = UserChatData.getCache(profile.getUniqueId());

		if (userData != null) {
			try {
				userData.updateDisplayName();
				userData.updateTabListName();
			} catch (SQLException ex) {
				this.getLog().console(ex);
			}
		}
	}

}