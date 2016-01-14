package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.minecraft.events.BungeeLoadedEvent;
import net.netcoding.niftybukkit.minecraft.events.PlayerDisconnectEvent;
import net.netcoding.niftybukkit.minecraft.events.PlayerPostLoginEvent;
import net.netcoding.niftybukkit.minecraft.messages.BungeeHelper;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.commands.Vanish;
import net.netcoding.niftycore.util.StringUtil;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Connections extends BukkitListener {

	public Connections(JavaPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onBungeeLoaded(BungeeLoadedEvent event) {
		new BungeeHelper(this.getPlugin(), new MessageReceived(this.getPlugin()), true);
	}

	@EventHandler
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		UserChatData.removeCache(event.getProfile());
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.setJoinMessage(null);
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		event.setLeaveMessage(null);
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		event.setQuitMessage(null);
	}

	@EventHandler
	public void onPlayerPostLogin(PlayerPostLoginEvent event) {
		final UserChatData userData = new UserChatData(this.getPlugin(), event.getProfile());

		try {
			NiftyChat.getSQL().updateAsync(StringUtil.format("INSERT IGNORE INTO {0} (uuid) VALUES (?);", Config.USER_TABLE), userData.getProfile().getUniqueId());
			userData.updateDisplayName();
			userData.updateTabListName();

			if (userData.hasPermissions("chat", "bypass", "move"))
				userData.setMoved();

			userData.applyFlagData(Vanish.FLAG);
		} catch (Exception ex) {
			this.getLog().console(ex);
		}
	}

}