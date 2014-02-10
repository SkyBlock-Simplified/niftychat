package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.managers.Cache;
import net.netcoding.niftychat.managers.UserData;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

public class Move extends BukkitListener {

	public Move(NiftyChat plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent event) {
		UserData userData = Cache.userData.get(event.getPlayer().getName());
		if (userData != null) userData.setMoved(); // TODO: Run timings on active server
	}

}