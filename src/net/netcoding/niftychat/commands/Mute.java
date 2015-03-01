package net.netcoding.niftychat.commands;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.Log;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftybukkit.util.TimeUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.cache.UserFlagData;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Mute extends BukkitCommand {

	public static final transient SimpleDateFormat EXPIRE_FORMAT = new SimpleDateFormat("MMM dd, yyyy h:mm:ss a z");

	public Mute(NiftyChat plugin) {
		super(plugin, "mute");
		this.setMaximumArgsLength(3);
	}

	public static void sendMutedError(Log logger, CommandSender sender, UserChatData userData) {
		UserFlagData muteData = userData.getFlagData("muted");
		String expiry = muteData.hasExpiry() ? StringUtil.format(" until {{0}}", EXPIRE_FORMAT.format(new Date(muteData.getExpires()))) : "";
		String server = muteData.isGlobal() ? "" : StringUtil.format(" in {{0}}", muteData.getServerName());
		logger.error(sender, "You are {0}muted{1}{2}.", (muteData.isGlobal() ? "" : "globally "), server, expiry);
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		String playerName = args[0];
		MojangProfile profile;
		long expires = args.length >= 2 ? TimeUtil.getDateTime(args[1]) : 0;
		String server = "*";

		if (isConsole(playerName)) {
			this.getLog().error(sender, "You cannot mute the console!");
			return;
		}

		if (NiftyBukkit.getBungeeHelper().isOnline()) {
			server = NiftyBukkit.getBungeeHelper().getServerName();

			if ((args.length == 2 && expires == 0) || args.length == 3) {
				if (NiftyBukkit.getBungeeHelper().getServer(args[args.length - 1]) != null)
					server = args[args.length - 1];
			}
		}

		try {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName);
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(sender, "Unable to locate the uuid of {{0}}!", playerName);
			return;
		}

		if (sender.getName().equalsIgnoreCase(profile.getName()) && !(alias.endsWith("unmute") && this.hasPermissions(sender, "mute", "roar"))) {
			this.getLog().error(sender, "You cannot mute yourself!");
			return;
		}

		UserChatData userData = UserChatData.getCache(profile);

		if (Config.isGlobalCommand(alias, server)) {
			server = "*";
			userData.resetNonGlobalFlagData("muted");
		}

		boolean isMuted = userData.getFlagData("muted", server).getValue();
		if (alias.matches("^g?(lobal)?un[\\w]+")) isMuted = true;
		if (isMuted) expires = 0;
		userData.updateFlagData("muted", isMuted, server, expires);
		String serverMsg = server.equals("*") ? "" : StringUtil.format(" in the {{0}} server", server);
		String expireMsg = (!isMuted && expires != 0) ? StringUtil.format(" until {{0}}", EXPIRE_FORMAT.format(new Date(expires))) : "";
		String receiveMsg = "You are {{0}}{1}muted{2}{3}.";
		String sendMsg = "{{0}} is {{1}}{2}muted{3}{4}.";

		if (!sender.getName().equalsIgnoreCase(profile.getName()))
			this.getLog().message(sender, sendMsg, profile.getName(), (!server.equals("*") ? "" : "globally "), (!isMuted ? "" : "un"), serverMsg, expireMsg);

		if (!NiftyBukkit.getBungeeHelper().isOnline()) {
			if (userData.isOnline())
				this.getLog().message(userData.getOfflinePlayer().getPlayer(), receiveMsg, "", (!isMuted ? "" : "un"), "", expireMsg);
		} else {
			if (isConsole(sender) && NiftyBukkit.getBungeeHelper().getServer().getPlayerCount() == 0) return;
			NiftyBukkit.getBungeeHelper().message(profile, ChatColor.GRAY + StringUtil.format(receiveMsg, (!server.equals("*") ? "" : "globally "), (!isMuted ? "" : "un"), serverMsg, expireMsg));
		}
	}

}