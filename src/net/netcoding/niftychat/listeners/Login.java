package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.minecraft.BungeeHelper;
import net.netcoding.niftybukkit.minecraft.events.BungeeLoadedEvent;
import net.netcoding.niftybukkit.minecraft.events.PlayerPostLoginEvent;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.UserChatData;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

public class Login extends BukkitListener {

	public Login(NiftyChat plugin) {
		super(plugin);
	}

	@EventHandler
	public void onBungeeLoaded(BungeeLoadedEvent event) {
		Cache.chatHelper = new BungeeHelper(this.getPlugin(), new MessageReceived(this.getPlugin()), true);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.setJoinMessage("");
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerPostLogin(PlayerPostLoginEvent event) {
		final UserChatData userData = new UserChatData(this.getPlugin(), event.getPlayer());

		try {
			Cache.MySQL.update(StringUtil.format("INSERT IGNORE INTO `{0}` (`uuid`) VALUES (?);", Config.USER_TABLE), userData.getUniqueId());
			userData.updateDisplayName();
			userData.updateTabListName();

			if (userData.hasPermissions("chat", "bypass", "move"))
				userData.setMoved();

			userData.applyFlagData("vanished", true);
		} catch (Exception ex) {
			this.getLog().console(ex);
		}
	}

}