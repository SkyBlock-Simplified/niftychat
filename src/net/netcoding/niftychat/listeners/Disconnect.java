package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.UserChatData;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Disconnect extends BukkitListener {

	public Disconnect(NiftyChat plugin) {
		super(plugin);
	}

	private void playerDisconnect(Player player) {
		UserChatData.removeCache(NiftyBukkit.getMojangRepository().searchByExactUUID(player.getUniqueId()));
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		event.setLeaveMessage("");
		this.playerDisconnect(event.getPlayer());
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		event.setQuitMessage("");
		this.playerDisconnect(event.getPlayer());
	}

}