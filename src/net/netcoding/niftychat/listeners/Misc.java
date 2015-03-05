package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftyranks.events.RankChangeEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

public class Misc extends BukkitListener {

	public Misc(NiftyChat plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent event) {
		try {
			MojangProfile profile = NiftyBukkit.getMojangRepository().searchByPlayer(event.getPlayer());
			UserChatData userData = UserChatData.getCache(profile);
			if (userData.isOnline()) userData.setMoved();
		} catch (ProfileNotFoundException pfne) {
			// For whatever reason they could not be found
		}
	}

	@EventHandler
	public void onRankChangeEvent(RankChangeEvent event) {
		UserChatData userData = UserChatData.getCache(event.getProfile());

		if (userData.isOnline()) {
			userData.updateDisplayName();
			userData.updateTabListName();
			userData.applyFlagData("vanished");
		}
	}

}