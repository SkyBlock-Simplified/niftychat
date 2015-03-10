package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftychat.NiftyChat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Disconnect extends BukkitListener {

	public Disconnect(NiftyChat plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		event.setLeaveMessage("");
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		event.setQuitMessage("");
	}

}