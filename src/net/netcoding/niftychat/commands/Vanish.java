package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.UserChatData;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Vanish extends BukkitCommand {

	public Vanish(JavaPlugin plugin) {
		super(plugin, "vanish");
		this.setMinimumArgsLength(0);
		this.setMaximumArgsLength(2);
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		MojangProfile profile;
		String server = "*";
		String playerName = args.length == 0 ? sender.getName() : args[0];

		if (isConsole(sender) && args.length != 1) {
			this.getLog().error(sender, "You must provide a player name when vanishing a player from console!");
			return;
		}

		if (Cache.chatHelper.isOnline()) {
			server = Cache.chatHelper.getServerName();

			if (args.length > 0) {
				if (Cache.chatHelper.getServer(args[args.length - 1]) != null) {
					server = args[args.length - 1];
					if (args.length == 1) playerName = sender.getName();
				}
			}
		}

		try {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName)[0];
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(sender, "Unable to locate the uuid of {{0}}!", playerName);
			return;
		}

		UserChatData userData = UserChatData.getCache(profile.getUniqueId());
		userData = userData == null ? new UserChatData(this.getPlugin(), profile) : userData;
		server = userData.resetNonGlobalFlagData("vanished", alias, server);
		boolean isVanished = userData.getFlagData("vanished").getValue();
		if (alias.matches("^globalunvanish|gunvanish|unvanish$")) isVanished = true;
		userData.updateFlagData("vanished", isVanished, server, 0);
		String serverMsg = server.equals("*") ? "" : StringUtil.format(" in {{0}}", server);
		String receiveMsg = "You are {{0}}{1}vanished{2}.";
		String sendMsg = "{{0}} {{1}}{2}vanished{3}.";

		if (!sender.getName().equalsIgnoreCase(profile.getName()))
			this.getLog().message(sender, sendMsg, profile.getName(), (!server.equals("*") ? "" : "globally "), (!isVanished ? "" : "un"), serverMsg);

		if (!Cache.chatHelper.isOnline()) {
			if (userData.getPlayer() != null)
				this.getLog().message(userData.getPlayer(), receiveMsg, "", (!isVanished ? "" : "un"), "");
		} else {
			if (isConsole(sender) && Cache.chatHelper.getServer().getPlayerCount() == 0) return;
			Cache.chatHelper.message(profile, ChatColor.GRAY + StringUtil.format(receiveMsg, (!server.equals("*") ? "" : "globally "), (!isVanished ? "" : "un"), serverMsg));
		}
	}

}