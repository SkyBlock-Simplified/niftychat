package net.netcoding.niftychat.listeners;

import static net.netcoding.niftychat.managers.Cache.Log;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.managers.Cache;
import net.netcoding.niftychat.managers.UserData;

public class Login extends BukkitListener {

	public Login(NiftyChat plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.setJoinMessage(null);
		Player player = event.getPlayer();
		final String playerName = player.getName();

		Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), new Runnable() {
			@Override
			public void run() {
				UserData userData = Cache.userData.get(playerName);

				try {
					Cache.MySQL.update("INSERT IGNORE INTO `nc_users` (`user`) VALUES (?);", playerName);
					userData.updateRanks();
					userData.updateVaultRanks();
					userData.updateDisplayName();
					userData.updateTabListName();

					if (userData.hasPermissions("chat", "bypass", "move"))
						userData.setMoved();

					//Cache.userData.put(playerName, userData);
				} catch (Exception ex) {
					Log.console(ex);
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		UserData userData = new UserData(super.getPlugin(), playerName);
		Cache.userData.put(playerName, userData);

		if (super.hasPermissions(player, "vanish", "see"))
			Cache.ghosts.add(player);

		if (event.getResult() == Result.KICK_FULL && super.hasPermissions(player, "join", "fullserver"))
			event.setResult(Result.ALLOWED);
	}

}