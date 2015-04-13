package net.netcoding.niftychat.listeners;

import java.util.Arrays;
import java.util.List;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.minecraft.events.PlayerNameChangeEvent;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.commands.Mute;
import net.netcoding.niftychat.commands.Vanish;
import net.netcoding.niftyranks.events.RankChangeEvent;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Misc extends BukkitListener {

	private static final transient List<Material> BOOKS = Arrays.asList(Material.BOOK, Material.BOOK_AND_QUILL, Material.ENCHANTED_BOOK, Material.WRITTEN_BOOK);

	public Misc(JavaPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Material type = event.getBlock().getType();

		if (Material.SIGN_POST.equals(type) || Material.WALL_SIGN.equals(type)) {
			MojangProfile profile = NiftyBukkit.getMojangRepository().searchByPlayer(event.getPlayer());
			UserChatData userData = UserChatData.getCache(profile);
			event.setCancelled(userData.getFlagData(Mute.FLAG).getValue());
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getItem() != null && BOOKS.contains(event.getItem().getType())) {
			MojangProfile profile = NiftyBukkit.getMojangRepository().searchByPlayer(event.getPlayer());
			UserChatData userData = UserChatData.getCache(profile);
			event.setCancelled(userData.getFlagData(Mute.FLAG).getValue());
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		try {
			MojangProfile profile = NiftyBukkit.getMojangRepository().searchByPlayer(event.getPlayer());
			UserChatData userData = UserChatData.getCache(profile);
			if (userData.getOfflinePlayer().isOnline()) userData.setMoved();
		} catch (ProfileNotFoundException pfne) { }
	}

	@EventHandler
	public void onPlayerNameChange(PlayerNameChangeEvent event) {
		UserChatData userData = UserChatData.getCache(event.getProfile());

		if (userData.getOfflinePlayer().isOnline()) {
			userData.updateDisplayName();
			userData.updateTabListName();
			userData.applyFlagData(Vanish.FLAG);
		}
	}

	@EventHandler
	public void onRankChangeEvent(RankChangeEvent event) {
		UserChatData userData = UserChatData.getCache(event.getProfile());

		if (userData.getOfflinePlayer().isOnline()) {
			userData.updateDisplayName();
			userData.updateTabListName();
			userData.applyFlagData(Vanish.FLAG);
		}
	}

}