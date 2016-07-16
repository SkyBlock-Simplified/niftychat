package net.netcoding.nifty.chat.commands;

import net.netcoding.nifty.chat.NiftyChat;
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
import net.netcoding.nifty.core.util.RegexUtil;
import net.netcoding.nifty.core.util.StringUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Nick extends MinecraftListener {

	private static final int MINIMUM_CHARS = 3;
	private static final int MAXIMUM_CHARS = 16;
	public static final transient Pattern INVALID_NICKNAME_CHARS = Pattern.compile("([^\\w])");

	public Nick(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "nick",
			maximumArgs = 2,
			playerTabComplete = true
	)
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		if (isConsole(source) && args.length < 2) {
			this.getLog().error(source, "You must pass a player name when changing the nickname of a player from console!");
			return;
		}

		String playerName = (args.length == 2 ? args[0] : source.getName());
		String nick = RegexUtil.strip((args.length == 2 ? args[1] : args[0]), RegexUtil.VANILLA_PATTERN);
		boolean isMe = source.getName().equals(playerName);
		String your = (isMe ? ChatColor.GRAY + "You" : playerName);
		String has = (isMe ? "have" : "has");
		String _nick = nick.toLowerCase();
		boolean clear = _nick.matches("^off|clear$");
		boolean revoke = _nick.matches("^revoke|disable$");
		boolean grant = _nick.matches("^grant|allow|enable$");
		MinecraftMojangProfile profile;

		if (isConsole(playerName)) {
			this.getLog().error(source, "You cannot nickname the console!");
			return;
		}

		try {
			profile = Nifty.getMojangRepository().searchByUsername(playerName);
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(source, "Unable to locate the profile of {{0}}!", playerName);
			return;
		}

		UserChatData userData = UserChatData.getCache(profile);

		if (clear) {
			if (!playerName.equalsIgnoreCase(source.getName())) {
				if (!this.hasPermissions(source, "nick", "clear")) {
					this.getLog().error(source, "You do not have access to clear other players nicknames!");
					return;
				}
			}

			nick = null;
		} else if (revoke) {
			if (!this.hasPermissions(source, "nick", "revoke")) {
				this.getLog().error(source, "You do not have access to revoke player nickname access!");
				return;
			}
		} else if (grant) {
			if (!this.hasPermissions(source, "nick", "grant")) {
				this.getLog().error(source, "You do not have access to grant player nickname access!");
				return;
			}
		} else {
			if (!playerName.equalsIgnoreCase(source.getName())) {
				if (!this.hasPermissions(source, "nick", "other")) {
					this.getLog().error(source, "You do not have access to change other players nicknames!");
					return;
				}
			}

			if (userData.getFlagData("nick-revoke").getValue()) {
				this.getLog().message(source, "{{0}} has nickname access revoked!", profile.getName());
				return;
			}

			String noColorNick = RegexUtil.strip(nick, RegexUtil.REPLACE_ALL_PATTERN);
			String strippedNick = RegexUtil.strip(noColorNick, INVALID_NICKNAME_CHARS);

			if (!strippedNick.equals(noColorNick)) {
				if (!this.hasPermissions(source, "nick", "special")) {
					this.getLog().error(source, "Nicknames can only contain letters, numbers and underscores!");
					return;
				}
			}

			if (!this.hasPermissions(source, "nick", "length")) {
				if (strippedNick.length() < MINIMUM_CHARS) {
					this.getLog().error(source, "Nicknames must contain at least {{0}} letters!", MINIMUM_CHARS);
					return;
				}

				if (strippedNick.length() > MAXIMUM_CHARS) {
					this.getLog().error(source, "Nicknames can only be {{0}} characters long!", MAXIMUM_CHARS);
					return;
				}
			}

			if (!RegexUtil.strip(nick, RegexUtil.REPLACE_COLOR_PATTERN).equals(nick)) {
				if (!this.hasPermissions(source, "nick", "color")) {
					this.getLog().error(source, "You do not have access to use color in nicknames!");
					return;
				}
			}

			if (!RegexUtil.strip(nick, RegexUtil.REPLACE_MAGIC_PATTERN).equals(nick)) {
				if (!this.hasPermissions(source, "nick", "magic")) {
					this.getLog().error(source, "You do not have access to use magic nicknames!");
					return;
				}
			}

			if (!RegexUtil.strip(nick, RegexUtil.REPLACE_FORMAT_PATTERN).equals(nick)) {
				if (!this.hasPermissions(source, "nick", "format")) {
					this.getLog().error(source, "You do not have access to use formatting in nicknames!");
					return;
				}
			}

			boolean taken = NiftyChat.getSQL().query(StringUtil.format("SELECT * FROM {0} WHERE LOWER(ufnick) = LOWER(?) AND uuid <> ?;", Config.USER_TABLE), ResultSet::next, strippedNick, profile.getUniqueId());

			if (taken) {
				this.getLog().error(source, "The nickname {{0}} is already taken!", strippedNick);
				return;
			}
		}

		if (revoke) {
			if (userData.updateFlagData("nick-revoke", true, "*", 0))
				this.getLog().message(source, "{{0}} now {1} no nickname access!", your, has);
		} else if (grant) {
			if (userData.resetFlagData("nick-revoke", "*"))
				this.getLog().message(source, "{{0}} now {1} nickname access.", your, has);
		} else {
			String _ufnick = null;
			if (nick != null) _ufnick = RegexUtil.strip(nick, RegexUtil.REPLACE_ALL_PATTERN);

			try {
				if (NiftyChat.getSQL().update(StringUtil.format("UPDATE {0} SET nick = ?, ufnick = ? WHERE uuid = ?;", Config.USER_TABLE), nick, _ufnick, profile.getUniqueId())) {
					if (clear)
						this.getLog().message(source, "{{0}} now {1} no nickname.", your, has);
					else
						this.getLog().message(source, "{{0}} now {1} the nickname {{2}}.", your, has, RegexUtil.replaceColor(nick, RegexUtil.REPLACE_COLOR_PATTERN));
				} else
					this.getLog().error(source, "Player {{0}} not found!", playerName);
			} catch (SQLException sqlex) {
				this.getLog().error(source, "Unable to set nickname {{0}} for {{1}}!", nick, profile.getName());
			}
		}
	}

}