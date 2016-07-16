package net.netcoding.nifty.chat.commands;

import net.netcoding.nifty.chat.cache.Config;
import net.netcoding.nifty.chat.cache.UserChatData;
import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.nifty.core.util.StringUtil;

public class Vanish extends MinecraftListener {

	public static final String FLAG = "vanished";

	public Vanish(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "vanish",
			minimumArgs = 0,
			maximumArgs = 2,
			aliases = {
					"globalunvanish",
					"globalvanish",
					"globalunv",
					"gunvanish",
					"globalv",
					"gvanish",
					"gunv",
					"unv",
					"gv",
					"v",
			},
			usages = {
					@Command.Usage(index = 0, match = "g?(lobal)?v(anish)?", replace = "[player]"),
					@Command.Usage(index = 0, match = "g?(lobal)?unv(anish)?", replace = "[player]")
			}
	)
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		MinecraftMojangProfile profile;
		String server = Config.getServerNameFromArgs(args, (args.length > 0));
		String playerName = args.length == 0 ? source.getName() : args[0];
		playerName = Nifty.getBungeeHelper().getDetails().isDetected() ? !Nifty.getBungeeHelper().getServerName().equals(server) ? args.length == 1 ? source.getName() : playerName : playerName : playerName;

		if (isConsole(source) && args.length == 0) {
			this.getLog().error(source, "You must provide a player name when vanishing a player from console!");
			return;
		}

		if (isConsole(playerName)) {
			this.getLog().error(source, "You cannot vanish the console!");
			return;
		}

		try {
			profile = Nifty.getMojangRepository().searchByUsername(playerName);
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(source, "Unable to locate the profile of {{0}}!", playerName);
			return;
		}

		if (!source.getName().equals(profile.getName()) && !this.hasPermissions(source, "vanish", "others")) {
			this.getLog().error(source, "You are not allowed to vanish other players!");
			return;
		}

		UserChatData userData = UserChatData.getCache(profile);

		if (Config.isGlobalCommand(alias, server)) {
			server = "*";
			userData.resetFlagData(FLAG, "");
		}

		boolean isVanished = userData.getFlagData(FLAG, server).getValue();
		if (Config.isForcedCommand(alias)) isVanished = true;
		userData.updateFlagData(FLAG, !isVanished, server, 0);
		String serverMsg = server.equals("*") ? "" : StringUtil.format(" in the {{0}} server", server);
		String receiveMsg = "You are {{0}}{1}vanished{2}.";
		String sendMsg = "{{0}} is {{1}}{2}vanished{3}.";

		if (!source.getName().equalsIgnoreCase(profile.getName()))
			this.getLog().message(source, sendMsg, profile.getName(), (!server.equals("*") ? "" : "globally "), (!isVanished ? "" : "un"), serverMsg);

		if (!Nifty.getBungeeHelper().getDetails().isDetected() && userData.isOnlineLocally())
			this.getLog().message(userData.getOfflinePlayer().getPlayer(), receiveMsg, "", (!isVanished ? "" : "un"), "");
		else {
			if (isConsole(source) && Nifty.getBungeeHelper().getServer().getPlayerCount() == 0) return;
			Nifty.getBungeeHelper().message(profile, ChatColor.GRAY + StringUtil.format(receiveMsg, (!server.equals("*") ? "" : "globally "), (!isVanished ? "" : "un"), serverMsg));
		}
	}

}