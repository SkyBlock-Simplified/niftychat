package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitListener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Disconnect extends BukkitListener {

	public Disconnect(JavaPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		event.setLeaveMessage(null);
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		event.setQuitMessage(null);
	}

}