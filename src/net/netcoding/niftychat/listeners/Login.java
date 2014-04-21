package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.UserChatData;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;

public class Login extends BukkitListener {

	public Login(NiftyChat plugin) {
		super(plugin);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		UserChatData userData = new UserChatData(this.getPlugin(), event.getPlayer());

		try {
			userData.updateDisplayName();
			userData.updateTabListName();

			if (userData.hasPermissions("chat", "bypass", "move"))
				userData.setMoved();
		} catch (Exception ex) {
			this.getLog().console(ex);
		}

		//if (this.hasPermissions(player, "vanish", "see"))
		//	Cache.ghosts.add(player);
	}

}