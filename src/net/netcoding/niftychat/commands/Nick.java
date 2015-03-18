package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.factory.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.BungeeServer;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftybukkit.util.RegexUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.UserChatData;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Nick extends BukkitCommand {

	private static final int MINIMUM_CHARS = 3;
	private static final int MAXIMUM_CHARS = 16;
	public static final transient Pattern INVALID_NICKNAME_CHARS = Pattern.compile("([^\\w])");

	public Nick(JavaPlugin plugin) {
		super(plugin, "nick");
		this.setMaximumArgsLength(2);
	}

	@Override
	protected void onCommand(CommandSender sender, String alias, String[] args) throws Exception, Exception {
		if (isConsole(sender) && args.length < 2) {
			this.getLog().error(sender, "You must pass a player name when changing the nickname of a player from console!");
			return;
		}

		String playerName = (args.length == 2 ? args[0] : sender.getName());
		String nick = RegexUtil.strip((args.length == 2 ? args[1] : args[0]), RegexUtil.VANILLA_PATTERN);
		boolean isMe = sender.getName() == playerName;
		String your = (isMe ? ChatColor.GRAY + "You" : playerName);
		String has = (isMe ? "have" : "has");
		String _nick = nick.toLowerCase();
		boolean clear = _nick.matches("^off|clear$");
		boolean revoke = _nick.matches("^revoke|disable$");
		boolean grant = _nick.matches("^grant|allow|enable$");
		MojangProfile profile = null;

		if (isConsole(playerName)) {
			this.getLog().error(sender, "You cannot nickname the console!");
			return;
		}

		try {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName);
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(sender, "Unable to locate the profile of {{0}}!", playerName);
			return;
		}

		UserChatData userData = UserChatData.getCache(profile);

		if (clear) {
			if (!playerName.equalsIgnoreCase(sender.getName())) {
				if (!this.hasPermissions(sender, "nick", "clear")) {
					this.getLog().error(sender, "You do not have access to clear other players nicknames!");
					return;
				}
			}

			nick = null;
		} else if (revoke) {
			if (!this.hasPermissions(sender, "nick", "revoke")) {
				this.getLog().error(sender, "You do not have access to revoke player nickname access!");
				return;
			}
		} else if (grant) {
			if (!this.hasPermissions(sender, "nick", "grant")) {
				this.getLog().error(sender, "You do not have access to grant player nickname access!");
				return;
			}
		} else {
			if (!playerName.equalsIgnoreCase(sender.getName())) {
				if (!this.hasPermissions(sender, "nick", "other")) {
					this.getLog().error(sender, "You do not have access to change other players nicknames!");
					return;
				}
			}

			if (userData.getFlagData("nick-revoke").getValue()) {
				this.getLog().message(sender, "{{0}} has nickname access revoked!", profile.getName());
				return;
			}

			String noColorNick = RegexUtil.strip(nick, RegexUtil.REPLACE_ALL_PATTERN);
			String strippedNick = RegexUtil.strip(noColorNick, INVALID_NICKNAME_CHARS);

			if (!strippedNick.equals(noColorNick)) {
				if (!this.hasPermissions(sender, "nick", "special")) {
					this.getLog().error(sender, "Nicknames can only contain letters, numbers and underscores!");
					return;
				}
			}

			if (!this.hasPermissions(sender, "nick", "length")) {
				if (strippedNick.length() < MINIMUM_CHARS) {
					this.getLog().error(sender, "Nicknames must contain at least {{0}} letters!", MINIMUM_CHARS);
					return;
				}

				if (strippedNick.length() > MAXIMUM_CHARS) {
					this.getLog().error(sender, "Nicknames can only be {{0}} characters long!", MAXIMUM_CHARS);
					return;
				}
			}

			if (RegexUtil.strip(nick, RegexUtil.REPLACE_COLOR_PATTERN) != nick) {
				if (!this.hasPermissions(sender, "nick", "color")) {
					this.getLog().error(sender, "You do not have access to use color in nicknames!");
					return;
				}
			}

			if (RegexUtil.strip(nick, RegexUtil.REPLACE_MAGIC_PATTERN) != nick) {
				if (!this.hasPermissions(sender, "nick", "magic")) {
					this.getLog().error(sender, "You do not have access to use magic nicknames!");
					return;
				}
			}

			if (RegexUtil.strip(nick, RegexUtil.REPLACE_FORMAT_PATTERN) != nick) {
				if (!this.hasPermissions(sender, "nick", "format")) {
					this.getLog().error(sender, "You do not have access to use formatting in nicknames!");
					return;
				}
			}

			boolean taken = Cache.MySQL.query(StringUtil.format("SELECT * FROM `{0}` WHERE LOWER(`ufnick`) = LOWER(?) AND `uuid` <> ?;", Config.USER_TABLE), new ResultCallback<Boolean>() {
				@Override
				public Boolean handle(ResultSet result) throws SQLException {
					return result.next();
				}
			}, strippedNick, profile.getUniqueId());

			if (taken) {
				this.getLog().error(sender, "The nickname {{0}} is already taken!", strippedNick);
				return;
			}
		}

		if (revoke) {
			if (userData.updateFlagData("nick-revoke", true, "*", 0))
				this.getLog().message(sender, "{{0}} now {1} no nickname access!", your, has);
		} else if (grant) {
			if (userData.resetFlagData("nick-revoke", "*"))
				this.getLog().message(sender, "{{0}} now {1} nickname access.", your, has);
		} else {
			String _ufnick = null;
			if (nick != null) _ufnick = RegexUtil.strip(nick, RegexUtil.REPLACE_ALL_PATTERN);

			if (Cache.MySQL.update(StringUtil.format("UPDATE `{0}` SET `nick` = ?, `ufnick` = ? WHERE `uuid` = ?;", Config.USER_TABLE), nick, _ufnick, profile.getUniqueId())) {
				if (clear)
					this.getLog().message(sender, "{{0}} now {1} no nickname.", your, has);
				else
					this.getLog().message(sender, "{{0}} now {1} the nickname {{2}}.", your, has, RegexUtil.replaceColor(nick, RegexUtil.REPLACE_COLOR_PATTERN));
			} else
				this.getLog().error(sender, "Player {{0}} not found!", playerName);
		}
	}

	@Override
	protected List<String> onTabComplete(CommandSender sender, String label, String[] args) throws Exception {
		final String firstArg = (args.length > 0 ? args[0] : "");
		List<String> names = new ArrayList<>();

		if (NiftyBukkit.getBungeeHelper().isDetected()) {
			for (BungeeServer server : NiftyBukkit.getBungeeHelper().getServers()) {
				for (MojangProfile profile : server.getPlayerList()) {
					if (profile.getName().startsWith(firstArg) || profile.getName().contains(firstArg))
						names.add(profile.getName());
				}
			}
		} else {
			for (UserChatData userData : UserChatData.getCache()) {
				if (userData.getProfile().getName().startsWith(firstArg) || userData.getProfile().getName().contains(firstArg))
					names.add(userData.getProfile().getName());
			}
		}

		return names;
	}

}