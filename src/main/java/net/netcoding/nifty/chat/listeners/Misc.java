package net.netcoding.nifty.chat.listeners;

import net.netcoding.nifty.chat.commands.Mute;
import net.netcoding.nifty.chat.commands.Vanish;
import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.Event;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.event.block.BlockPlaceEvent;
import net.netcoding.nifty.common.minecraft.event.player.PlayerInteractEvent;
import net.netcoding.nifty.common.minecraft.event.player.PlayerMoveEvent;
import net.netcoding.nifty.common.minecraft.event.player.PlayerNameChangeEvent;
import net.netcoding.nifty.common.minecraft.material.Material;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.nifty.chat.cache.UserChatData;
import net.netcoding.nifty.ranks.events.PlayerRankChangeEvent;

import java.util.Arrays;
import java.util.List;

public class Misc extends MinecraftListener {

	private static final transient List<Material> BOOKS = Arrays.asList(Material.BOOK, Material.BOOK_AND_QUILL, Material.ENCHANTED_BOOK, Material.WRITTEN_BOOK);

	public Misc(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Event
	private void onBlockPlace(BlockPlaceEvent event) {
		Material type = event.getBlock().getType();

		if (Material.SIGN_POST == type || Material.WALL_SIGN == type) {
			MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByPlayer(event.getPlayer());
			UserChatData userData = UserChatData.getCache(profile);

			if (userData.getFlagData(Mute.FLAG).getValue())
				event.setCancelled(true);

			if (event.isCancelled())
				this.getLog().console("{0} was seen as muted, sign create blocked!", profile.getName());
		}
	}

	@Event
	private void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getItem() != null && BOOKS.contains(event.getItem().getType())) {
			MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByPlayer(event.getPlayer());
			UserChatData userData = UserChatData.getCache(profile);

			if (userData.getFlagData(Mute.FLAG).getValue())
				event.setCancelled(true);
		}
	}

	@Event
	private void onPlayerMove(PlayerMoveEvent event) {
		try {
			MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByPlayer(event.getPlayer());
			UserChatData userData = UserChatData.getCache(profile);

			if (userData.isOnlineLocally())
				userData.setMoved();
		} catch (ProfileNotFoundException ignore) { }
	}

	@Event
	private void onPlayerNameChange(PlayerNameChangeEvent event) {
		UserChatData userData = UserChatData.getCache(event.getProfile());

		if (userData.isOnlineLocally()) {
			userData.updateDisplayName();
			userData.updateTabListName();
			userData.applyFlagData(Vanish.FLAG);
		}
	}

	@Event
	private void onRankChangeEvent(PlayerRankChangeEvent event) {
		UserChatData userData = UserChatData.getCache(event.getProfile());

		if (userData.isOnlineLocally()) {
			userData.updateDisplayName();
			userData.updateTabListName();
			userData.applyFlagData(Vanish.FLAG);
		}
	}

}