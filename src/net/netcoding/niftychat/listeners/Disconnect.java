package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.managers.Cache;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Disconnect extends BukkitListener {

	public Disconnect(NiftyChat plugin) {
		super(plugin);
	}

	private void playerDisconnect(Player player) {
		Cache.userData.remove(player.getName());

		if (Cache.ghosts.hasPlayer(player))
			Cache.ghosts.remove(player);
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		event.setLeaveMessage(null);
		this.playerDisconnect(event.getPlayer());
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		event.setQuitMessage(null);
		this.playerDisconnect(event.getPlayer());
	}

}