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

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class Login extends BukkitListener {

	private static boolean usingPEX = false;

	static {
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("PermissionsEx");

		if (plugin != null) {
			if (plugin.isEnabled())
				usingPEX = true;
		}
	}

	public Login(NiftyChat plugin) {
		super(plugin);
	}

	@EventHandler
	public void onBungeeLoaded(BungeeLoadedEvent event) {
		new BungeeHelper(this.getPlugin(), new MessageReceived(this.getPlugin()), true);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.setJoinMessage("");
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerPostLogin(PlayerPostLoginEvent event) {
		final UserChatData userData = new UserChatData(this.getPlugin(), event.getPlayer());

		try {
			Cache.MySQL.update(StringUtil.format("INSERT IGNORE INTO `{0}` (`uuid`) VALUES (?);", Config.USER_TABLE), userData.getProfile().getUniqueId());
			userData.updateDisplayName();
			userData.updateTabListName();

			if (!usingPEX) {
				if (userData.hasPermissions("chat", "bypass", "move"))
					userData.setMoved();

				if (!userData.hasPermissions("socialspy"))
					userData.resetFlagData("spying", "");

				if (!userData.hasPermissions("vanish"))
					userData.resetFlagData("vanished", "");
			} else
				userData.setMoved();

			userData.applyFlagData("vanished");
		} catch (Exception ex) {
			this.getLog().console(ex);
		}
	}

}