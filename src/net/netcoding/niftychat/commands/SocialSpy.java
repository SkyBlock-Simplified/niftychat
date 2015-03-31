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

public class SocialSpy extends BukkitCommand {

	public static final String FLAG = "spying";

	public SocialSpy(JavaPlugin plugin) {
		super(plugin, "socialspy");
		this.setMinimumArgsLength(0);
		this.setMaximumArgsLength(2);
	}

	@Override
	protected void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		MojangProfile profile;
		String server = Config.getServerNameFromArgs(args, (args.length > 0));
		String playerName = args.length == 0 ? sender.getName() : args[0];
		playerName = NiftyBukkit.getBungeeHelper().isDetected() ? !NiftyBukkit.getBungeeHelper().getServerName().equals(server) ? args.length == 1 ? sender.getName() : playerName : playerName : playerName;

		if (isConsole(sender) && args.length == 0) {
			this.getLog().error(sender, "You must provide a player name when modifying a players socialspy from console!");
			return;
		}

		if (isConsole(playerName)) {
			this.getLog().error(sender, "You cannot spy on the console!");
			return;
		}

		try {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName);
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(sender, "Unable to locate the profile of {{0}}!", playerName);
			return;
		}

		if (!sender.getName().equals(profile.getName()) && !this.hasPermissions(sender, "socialspy", "others")) {
			this.getLog().error(sender, "You are not allowed to enabled socialspy for other players!");
			return;
		}

		UserChatData userData = UserChatData.getCache(profile);
		if (Config.isGlobalCommand(alias, server)) server = "*";

		if (server.equals("*") && !this.hasPermissions(sender, "socialspy", "global")) {
			this.getLog().error(sender, "You are not allowed to globally spy on players!");
			return;
		}

		if (Config.isGlobalCommand(alias, server)) userData.resetFlagData(FLAG, "");
		boolean isSpying = userData.getFlagData(FLAG, server).getValue();
		if (Config.isForcedCommand(alias)) isSpying = true;
		userData.updateFlagData(FLAG, !isSpying, server, 0);
		String serverMsg = server.equals("*") ? "" : StringUtil.format(" in the {{0}} server", server);
		String receiveMsg = "You are {{0}}{1}spying{2}.";
		String sendMsg = "{{0}} is {{1}}{2}spying{3}.";

		if (!sender.getName().equalsIgnoreCase(profile.getName()))
			this.getLog().message(sender, sendMsg, profile.getName(), (!server.equals("*") ? "" : "globally "), (!isSpying ? "" : "un"), serverMsg);

		if (!NiftyBukkit.getBungeeHelper().isDetected() && userData.isOnlineLocally())
			this.getLog().message(userData.getOfflinePlayer().getPlayer(), receiveMsg, "", (!isSpying ? "" : "un"), "");
		else {
			if (isConsole(sender) && NiftyBukkit.getBungeeHelper().getServer().getPlayerCount() == 0) return;
			NiftyBukkit.getBungeeHelper().message(profile, ChatColor.GRAY + StringUtil.format(receiveMsg, (!server.equals("*") ? "" : "globally "), (!isSpying ? "" : "un"), serverMsg));
		}
	}

}