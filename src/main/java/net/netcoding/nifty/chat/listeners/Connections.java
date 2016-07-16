package net.netcoding.nifty.chat.listeners;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.Event;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.event.bungee.BungeeLoadedEvent;
import net.netcoding.nifty.common.minecraft.event.player.PlayerJoinEvent;
import net.netcoding.nifty.common.minecraft.event.player.PlayerQuitEvent;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.chat.NiftyChat;
import net.netcoding.nifty.chat.cache.Config;
import net.netcoding.nifty.chat.cache.UserChatData;
import net.netcoding.nifty.chat.commands.Vanish;

public class Connections extends MinecraftListener {

	public Connections(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Event
	public void onBungeeLoaded(BungeeLoadedEvent event) {
		Nifty.getBungeeHelper().register(this.getPlugin(), new MessageReceived(this.getPlugin()));
	}

	@Event
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.setJoinMessage(null);
	}

	@Event
	public void onProfileQuit(PlayerQuitEvent event) {
		event.setQuitMessage(null);
		UserChatData.removeCache(event.getProfile());
	}

	@Event
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