package net.netcoding.nifty.chat.commands;

import net.netcoding.nifty.chat.cache.Config;
import net.netcoding.nifty.chat.cache.UserChatData;
import net.netcoding.nifty.chat.cache.UserFlagData;
import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftLogger;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.core.util.TimeUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Mute extends MinecraftListener {

	public static final transient SimpleDateFormat EXPIRE_FORMAT = new SimpleDateFormat("MMM dd, yyyy h:mm:ss a z");
	public static final String FLAG = "muted";

	public Mute(MinecraftPlugin plugin) {
		super(plugin);
	}

	public static void sendMutedError(MinecraftLogger logger, CommandSource source, UserChatData userData) {
		UserFlagData muteData = userData.getFlagData("muted");
		String expiry = muteData.hasExpiry() ? StringUtil.format(" until {{0}}", EXPIRE_FORMAT.format(new Date(muteData.getExpires()))) : "";
		String server = muteData.isGlobal() ? "" : StringUtil.format(" in {{0}}", muteData.getServerName());
		logger.error(source, "You are {0}muted{1}{2}.", (muteData.isGlobal() ? "" : "globally "), server, expiry);
	}

	@Command(name = "mute",
			maximumArgs = 3,
			playerTabComplete = true,
			usages = {
					@Command.Usage(match = "g(lobal)?mute", replace = "<player> [time]"),
					@Command.Usage(match = "(g(lobal)?)?unmute", replace = "<player>")
			}
	)
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		String playerName = args[0];
		MinecraftMojangProfile profile;
		long expires = args.length >= 2 ? System.currentTimeMillis() + TimeUtil.getDateTime(args[1]) : 0;
		String server = Config.getServerNameFromArgs(args, ((args.length == 2 && expires == 0) || args.length == 3));

		if (isConsole(playerName)) {
			this.getLog().error(source, "You cannot mute the console!");
			return;
		}

		try {
			profile = Nifty.getMojangRepository().searchByUsername(playerName);
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(source, "Unable to locate the profile of {{0}}!", playerName);
			return;
		}

		if (source.getName().equalsIgnoreCase(profile.getName()) && !(alias.endsWith("unmute") && this.hasPermissions(source, "mute", "roar"))) {
			this.getLog().error(source, "You cannot mute yourself!");
			return;
		}

		UserChatData userData = UserChatData.getCache(profile);

		if (Config.isGlobalCommand(alias, server)) {
			server = "*";
			userData.resetFlagData(FLAG, "");
		}

		if (server.equals("*") && !this.hasPermissions(source, "mute", "global")) {
			this.getLog().error(source, "You are not allowed to globally mute players!");
			return;
		}

		boolean isMuted = userData.getFlagData(FLAG, server).getValue();
		if (Config.isForcedCommand(alias)) isMuted = true;
		if (isMuted) expires = 0;
		userData.updateFlagData(FLAG, !isMuted, server, expires);
		String serverMsg = server.equals("*") ? "" : StringUtil.format(" in the {{0}} server", server);
		String expireMsg = (!isMuted && expires != 0) ? StringUtil.format(" until {{0}}", EXPIRE_FORMAT.format(new Date(expires))) : "";
		String receiveMsg = "You are {{0}}{1}muted{2}{3}.";
		String sendMsg = "{{0}} is {{1}}{2}muted{3}{4}.";

		if (!source.getName().equalsIgnoreCase(profile.getName()))
			this.getLog().message(source, sendMsg, profile.getName(), (!server.equals("*") ? "" : "globally "), (!isMuted ? "" : "un"), serverMsg, expireMsg);

		if (!Nifty.getBungeeHelper().getDetails().isDetected() && userData.isOnlineLocally())
			this.getLog().message(userData.getOfflinePlayer().getPlayer(), receiveMsg, "", (!isMuted ? "" : "un"), "", expireMsg);
		else {
			if (isConsole(source) && Nifty.getBungeeHelper().getServer().getPlayerCount() == 0) return;
			Nifty.getBungeeHelper().message(profile, ChatColor.GRAY + StringUtil.format(receiveMsg, (!server.equals("*") ? "" : "globally "), (!isMuted ? "" : "un"), serverMsg, expireMsg));
		}
	}

}