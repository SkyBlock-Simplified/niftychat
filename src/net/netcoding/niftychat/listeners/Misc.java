package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.minecraft.events.PlayerNameChangeEvent;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.commands.Vanish;
import net.netcoding.niftyranks.events.RankChangeEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Misc extends BukkitListener {

	public Misc(JavaPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent event) {
		try {
			MojangProfile profile = NiftyBukkit.getMojangRepository().searchByPlayer(event.getPlayer());
			UserChatData userData = UserChatData.getCache(profile);
			if (userData.getOfflinePlayer().isOnline()) userData.setMoved();
		} catch (ProfileNotFoundException pfne) { }
	}

	@EventHandler
	public void onRankChangeEvent(RankChangeEvent event) {
		UserChatData userData = UserChatData.getCache(event.getProfile());

		if (userData.getOfflinePlayer().isOnline()) {
			userData.updateDisplayName();
			userData.updateTabListName();
			userData.applyFlagData(Vanish.FLAG);
		}
	}

	@EventHandler
	public void onPlayerNameChange(PlayerNameChangeEvent event) {
		UserChatData userData = UserChatData.getCache(event.getProfile());

		if (userData.getOfflinePlayer().isOnline()) {
			userData.updateDisplayName();
			userData.updateTabListName();
			userData.applyFlagData(Vanish.FLAG);
		}
	}

}