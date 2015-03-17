package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.cache.Config;
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
		String server = Config.getServerNameFromArgs(args, (args.length > 0));
		String playerName = args.length == 0 ? sender.getName() : args[0];
		playerName = NiftyBukkit.getBungeeHelper().isDetected() ? !NiftyBukkit.getBungeeHelper().getServerName().equals(server) ? args.length == 1 ? sender.getName() : playerName : playerName : playerName;

		if (isConsole(sender) && args.length == 0) {
			this.getLog().error(sender, "You must provide a player name when vanishing a player from console!");
			return;
		}

		if (isConsole(playerName)) {
			this.getLog().error(sender, "You cannot vanish the console!");
			return;
		}

		try {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName);
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(sender, "Unable to locate the profile of {{0}}!", playerName);
			return;
		}

		if (!sender.getName().equals(profile.getName()) && !this.hasPermissions(sender, "vanish", "others")) {
			this.getLog().error(sender, "You are not allowed to vanish other players!");
			return;
		}

		UserChatData userData = UserChatData.getCache(profile);

		if (Config.isGlobalCommand(alias, server)) {
			server = "*";
			userData.resetFlagData("vanished", "");
		}

		boolean isVanished = userData.getFlagData("vanished", server).getValue();
		if (Config.isForcedCommand(alias)) isVanished = true;
		userData.updateFlagData("vanished", !isVanished, server, 0);
		String serverMsg = server.equals("*") ? "" : StringUtil.format(" in the {{0}} server", server);
		String receiveMsg = "You are {{0}}{1}vanished{2}.";
		String sendMsg = "{{0}} is {{1}}{2}vanished{3}.";

		if (!sender.getName().equalsIgnoreCase(profile.getName()))
			this.getLog().message(sender, sendMsg, profile.getName(), (!server.equals("*") ? "" : "globally "), (!isVanished ? "" : "un"), serverMsg);

		if (!NiftyBukkit.getBungeeHelper().isDetected()) {
			if (userData.isOnline())
				this.getLog().message(userData.getOfflinePlayer().getPlayer(), receiveMsg, "", (!isVanished ? "" : "un"), "");
		} else {
			if (isConsole(sender) && NiftyBukkit.getBungeeHelper().getServer().getPlayerCount() == 0) return;
			NiftyBukkit.getBungeeHelper().message(profile, ChatColor.GRAY + StringUtil.format(receiveMsg, (!server.equals("*") ? "" : "globally "), (!isVanished ? "" : "un"), serverMsg));
		}
	}

}