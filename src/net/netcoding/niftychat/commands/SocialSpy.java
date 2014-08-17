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

	public SocialSpy(JavaPlugin plugin) {
		super(plugin, "socialspy");
		this.setMinimumArgsLength(0);
		this.setMaximumArgsLength(2);
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		MojangProfile profile;
		String server = "*";
		String playerName = args.length == 0 ? sender.getName() : args[0];

		if (isConsole(sender) && args.length == 0) {
			this.getLog().error(sender, "You must provide a player name when modifying a players socialspy from console!");
			return;
		}

		if (NiftyBukkit.getBungeeHelper().isOnline()) {
			server = NiftyBukkit.getBungeeHelper().getServerName();

			if (args.length > 0) {
				if (NiftyBukkit.getBungeeHelper().getServer(args[args.length - 1]) != null) {
					server = args[args.length - 1];
					if (args.length == 1) playerName = sender.getName();
				}
			}
		}

		if (isConsole(playerName)) {
			this.getLog().error(sender, "You cannot spy on the console!");
			return;
		}

		try {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName)[0];
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(sender, "Unable to locate the uuid of {{0}}!", playerName);
			return;
		}

		if (!sender.getName().equals(profile.getName()) && !this.hasPermissions(sender, "vanish", "others")) {
			this.getLog().error(sender, "You are not allowed to socialspy other players!");
			return;
		}

		UserChatData userData = UserChatData.getCache(profile.getUniqueId());
		userData = userData == null ? new UserChatData(this.getPlugin(), profile) : userData;
		if (Config.isGlobalCommand(alias, server)) server = "*";

		if (server.equals("*") && !this.hasPermissions(sender, "socialspy", "global")) {
			this.getLog().error(sender, "You are not allowed to globally spy on players!");
			return;
		}

		if (Config.isGlobalCommand(alias, server)) userData.resetNonGlobalFlagData("spying");
		boolean isSpying = userData.getFlagData("spying").getValue();
		if (alias.matches("^g?(lobal)?un[\\w]+")) isSpying = true;
		userData.updateFlagData("spying", isSpying, server, 0);
		String serverMsg = server.equals("*") ? "" : StringUtil.format(" in the {{0}} server", server);
		String receiveMsg = "You are {{0}}{1}spying{2}.";
		String sendMsg = "{{0}} is {{1}}{2}spying{3}.";

		if (!sender.getName().equalsIgnoreCase(profile.getName()))
			this.getLog().message(sender, sendMsg, profile.getName(), (!server.equals("*") ? "" : "globally "), (!isSpying ? "" : "un"), serverMsg);

		if (!NiftyBukkit.getBungeeHelper().isOnline()) {
			if (userData.getPlayer() != null)
				this.getLog().message(userData.getPlayer(), receiveMsg, "", (!isSpying ? "" : "un"), "");
		} else {
			if (isConsole(sender) && NiftyBukkit.getBungeeHelper().getServer().getPlayerCount() == 0) return;
			NiftyBukkit.getBungeeHelper().message(profile, ChatColor.GRAY + StringUtil.format(receiveMsg, (!server.equals("*") ? "" : "globally "), (!isSpying ? "" : "un"), serverMsg));
		}
	}

}