package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.minecraft.events.bungee.BungeeLoadedEvent;
import net.netcoding.niftybukkit.minecraft.events.profile.ProfileJoinEvent;
import net.netcoding.niftybukkit.minecraft.events.profile.ProfileQuitEvent;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.commands.Vanish;
import net.netcoding.niftycore.util.StringUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Connections extends BukkitListener {

	public Connections(JavaPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onBungeeLoaded(BungeeLoadedEvent event) {
		NiftyBukkit.getBungeeHelper().register(this.getPlugin(), new MessageReceived(this.getPlugin()));
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.setJoinMessage(null);
	}

	@EventHandler
	public void onProfileQuit(ProfileQuitEvent event) {
		event.setQuitMessage(null);
		UserChatData.removeCache(event.getProfile());
	}

	@EventHandler
	public void onProfileJoin(ProfileJoinEvent event) {
		final UserChatData userData = new UserChatData(this.getPlugin(), event.getProfile());

		try {
			NiftyChat.getSQL().updateAsync(StringUtil.format("INSERT IGNORE INTO {0} (uuid) VALUES (?);", Config.USER_TABLE), userData.getProfile().getUniqueId());
			userData.updateDisplayName();
			userData.updateTabListName();

			if (userData.hasPermissions("chat", "bypass", "move"))
				userData.setMoved();

			userData.applyFlagData(Vanish.FLAG);
		} catch (Exception ex) {
			this.getLog().console(ex, userData.getProfile().toString());
		}
	}

}