package net.netcoding.niftychat.listeners;

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
		MojangProfile profile = NiftyBukkit.getMojangRepository().searchByPlayer(event.getPlayer());
		UserChatData userData = UserChatData.getCache(profile.getUniqueId());

		if (userData.isOnline())
			userData.setMoved();
		else
			this.getLog().console(profile.getName() + " moved without being here!");
	}

	@EventHandler
	public void onRankChangeEvent(RankChangeEvent event) {
		UserChatData userData = UserChatData.getCache(event.getUniqueId());

		if (userData.isOnline()) {
			userData.updateDisplayName();
			userData.updateTabListName();
			userData.applyFlagData("vanished", false);
		}
	}

}