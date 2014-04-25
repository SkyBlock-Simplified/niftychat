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
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.cache.UserFlagData;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Mute extends BukkitCommand {

	public static final transient SimpleDateFormat EXPIRE_FORMAT = new SimpleDateFormat("MMM dd, yyyy h:mm:ss a z");

	public static final transient SimpleDateFormat SQL_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public Mute(NiftyChat plugin) {
		super(plugin, "mute");
	}

	public static void sendMutedError(Log logger, CommandSender sender, UserChatData userData) {
		UserFlagData muteData = userData.getFlagData("muted");
		String expiry = muteData.hasExpiry() ? StringUtil.format(" until {{0}}", EXPIRE_FORMAT.format(new Date(muteData.getExpires()))) : "";
		String server = muteData.isGlobal() ? "" : StringUtil.format(" in {{0}}", muteData.getServerName());
		logger.error(sender, "You are {0}muted{1}{2}.", (muteData.isGlobal() ? "" : "globally "), server, expiry);
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		if (args.length >= 1 && args.length <= 3) {
			String playerName = args[0];
			MojangProfile profile;
			long expires = args.length >= 2 ? TimeUtil.getDateTime(args[1]) : 0;
			String server = "*";

			if (isConsole(playerName)) {
				this.getLog().error(sender, "You cannot mute the console!");
				return;
			}

			try {
				profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName)[0];
			} catch (ProfileNotFoundException pnfe) {
				this.getLog().error(sender, "Unable to locate the uuid of {{0}}!", playerName);
				return;
			}

			if (sender.getName().equalsIgnoreCase(profile.getName()) && !(alias.endsWith("unmute") && this.hasPermissions(sender, "mute", "roar"))) {
				this.getLog().error(sender, "You cannot mute yourself!");
				return;
			}

			if (Cache.chatHelper.isOnline()) {
				server = Cache.chatHelper.getServerName();

				if ((args.length == 2 && expires == 0) || args.length == 3) {
					if (Cache.chatHelper.getServer(args[args.length - 1]) != null)
						server = args[args.length - 1];
				}
			}

			if (alias.matches("^globalmute|globalunmute|gmute|gunmute$") || server.matches("^global|all|\\*$")) {
				server = "*";
				Cache.MySQL.update(StringUtil.format("DELETE FROM `{0}` WHERE `uuid` = ? AND `flag` = ? AND `server` <> ?;", Config.USER_FLAGS_TABLE), profile.getUniqueId(), "muted", "*");
			}

			UserChatData userData = UserChatData.getCache(profile.getUniqueId());
			userData = userData == null ? new UserChatData(this.getPlugin(), profile) : userData;
			boolean isMuted = userData.isMuted();
			if (alias.matches("^unmute|globalunmute|gunmute$")) isMuted = true;
			if (isMuted) expires = 0;
			if (expires != 0) expires += System.currentTimeMillis();
			String sqlFormat = SQL_FORMAT.format(new Date(expires));
			Cache.MySQL.update(StringUtil.format("INSERT INTO `{0}` (`uuid`, `flag`, `value`, `server`, `_expires`) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `value` = ?, `_expires` = ?;", Config.USER_FLAGS_TABLE), profile.getUniqueId(), "muted", !isMuted, server, (expires == 0 ? null : sqlFormat), !isMuted, (expires == 0 ? null : sqlFormat));
			String serverMsg = server.equals("*") ? "" : StringUtil.format(" in {{0}}", server);
			String expireMsg = (!isMuted && expires != 0) ? StringUtil.format(" until {{0}}", EXPIRE_FORMAT.format(new Date(expires))) : "";
			String receiveMsg = "You are {{0}}{1}muted{2}{3}.";
			String sendMsg = "{{0}} {{1}}{2}muted{3}{4}";

			if (!sender.getName().equalsIgnoreCase(profile.getName()))
				this.getLog().message(sender, sendMsg, profile.getName(), (!server.equals("*") ? "" : "globally "), (!isMuted ? "" : "un"), serverMsg, expireMsg);

			if (!Cache.chatHelper.isOnline()) {
				if (userData.getPlayer() != null)
					this.getLog().message(userData.getPlayer(), receiveMsg, "", (!isMuted ? "" : "un"), "", expireMsg);
			} else {
				if (isConsole(sender) && Cache.chatHelper.getServer().getPlayerCount() == 0) return;
				Cache.chatHelper.message(profile, ChatColor.GRAY + StringUtil.format(receiveMsg, (!server.equals("*") ? "" : "globally "), (!isMuted ? "" : "un"), serverMsg, expireMsg));
			}
		} else
			this.showUsage(sender);
	}

}