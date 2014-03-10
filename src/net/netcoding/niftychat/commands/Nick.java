package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import net.netcoding.niftybukkit.database.ResultSetCallback;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.minecraft.BukkitTabCommand;
import net.netcoding.niftybukkit.util.RegexUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.managers.Cache;
import net.netcoding.niftychat.managers.UserData;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class Nick extends BukkitTabCommand {

	public Nick(NiftyChat plugin) {
		super(plugin, "nick");
	}

	@Override
	public void command(CommandSender sender, String args[]) throws SQLException, Exception {
		if (this.hasPermissions(sender, "nick")) {
			if ((BukkitHelper.isConsole(sender)) && args.length < 2)
				this.getLog().error(sender, "The nickname command requires a player name when used by the console!");
			else if (args.length >= 1 && args.length <= 2) {
				String playerName  = (args.length == 2 ? args[0] : sender.getName());
				String nick  = (args.length == 2 ? args[1] : args[0]);
				String your  = (sender.getName() == playerName ? ChatColor.GRAY + "You" : String.format("%2$s%1$s%3$s", playerName, ChatColor.RED, ChatColor.GRAY));
				String has   = (sender.getName() == playerName ? "have" : "has");
				String _nick = nick.toLowerCase();
				boolean off  = _nick.matches("^off|clear$");
				boolean rev  = _nick.matches("^revoke|disable$");
				boolean ena  = _nick.matches("^grant|allow|enable$");

				if (off) {
					if (!playerName.equalsIgnoreCase(sender.getName())) {
						if (!this.hasPermissions(sender, "nick", "clear")) {
							this.getLog().error(sender, "You do not have access to clear other players nicknames!");
							return;
						}
					}

					nick = null;
				} else if (rev || ena) {
					if (!this.hasPermissions(sender, "nick", "revoke")) {
						this.getLog().error(sender, "You do not have access to grant/revoke players nicknames!");
						return;
					}
				} else {
					if (!playerName.equalsIgnoreCase(sender.getName())) {
						if (!this.hasPermissions(sender, "nick", "other")) {
							this.getLog().error(sender, "You do not have access to change other players nicknames!");
							return;
						}
					}

					String noColorNick  = RegexUtil.strip(nick, RegexUtil.REPLACE_ALL_PATTERN);
					String strippedNick = RegexUtil.strip(noColorNick, RegexUtil.INVALID_NICKNAME_CHARZ);

					if (strippedNick != noColorNick) {
						this.getLog().error(sender, "Nicknames can only contain letters, numbers and underscores!");
						return;
					}

					if (!this.hasPermissions(sender, "nick", "length")) {
						int minChars = 3;
						int maxChars = 16;

						if (strippedNick.length() < minChars) {
							this.getLog().error(sender, "Nicknames must contain at least {%1$s} letters!", minChars);
							return;
						}

						if (strippedNick.length() > maxChars) {
							this.getLog().error(sender, "Nicknames can only be 16 characters long!", minChars);
							return;
						}
					}

					if (RegexUtil.replaceColor(nick, RegexUtil.REPLACE_COLOR_PATTERN) != nick) {
						if (!this.hasPermissions(sender, "nick", "color")) {
							this.getLog().error(sender, "You do not have access to use color in nicknames!");
							return;
						}
					}

					if (RegexUtil.replaceColor(nick, RegexUtil.REPLACE_MAGIC_PATTERN) != nick) {
						if (!this.hasPermissions(sender, "nick", "magic")) {
							this.getLog().error(sender, "You do not have access to use magic nicknames!");
							return;
						}
					}

					if (RegexUtil.replaceColor(nick, RegexUtil.REPLACE_FORMAT_PATTERN) != nick) {
						if (!this.hasPermissions(sender, "nick", "format")) {
							this.getLog().error(sender, "You do not have access to use formatting in nicknames!");
							return;
						}
					}

					boolean taken = (boolean)Cache.MySQL.query("SELECT * FROM `nc_users` WHERE LOWER(`ufnick`) = LOWER(?) AND LOWER(`user`) <> ?;", new ResultSetCallback() {
						@Override
						public Object handleResult(ResultSet result) {
							try {
								return result.next();
							} catch (SQLException ex) {
								ex.printStackTrace();
							}

							return true;
						}
					}, strippedNick, sender.getName());

					if (taken) {
						this.getLog().error(sender, "The nickname {%1$s} is already taken!", nick);
						return;
					}
				}

				if (rev) {
					//User.disableNick(user);
				} else if (ena) {
					//User.enableNick(user);
				} else {
					try {
						String matchedName = UserData.matchPlayerName(playerName);
						String _ufnick = null;
						if (nick != null) _ufnick = RegexUtil.replaceColor(nick, RegexUtil.REPLACE_ALL_PATTERN);

						if (Cache.MySQL.update("UPDATE `nc_users` SET `nick` = ?, `ufnick` = ? WHERE `user` = ?;", nick, _ufnick, matchedName)) {
							if (off)
								this.getLog().message(sender, "{%1$s} now %2$s no nickname.", your, has);
							else
								this.getLog().message(sender, "{%1$s} now %2$s the nickname {%3$s}.", your, has, RegexUtil.replaceColor(nick, RegexUtil.REPLACE_COLOR_PATTERN));
						} else
							this.getLog().error(sender, "Player {%1$s} not found!", playerName);
					} catch (Exception ex) {
						this.getLog().error(sender, "Unable to set nickname {%1$s} for {%2$s}.", nick, playerName);
						ex.printStackTrace();
					}
				}
			} else
				this.showUsage(sender);
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		final String firstArg = (args.length > 0 ? args[0] : "");

		Iterable<String> iterable = Iterables.transform(Iterables.filter(Cache.userData.values(), new Predicate<UserData>() {
			@Override
			public boolean apply(UserData userData) {
				String noColorNick = RegexUtil.strip(userData.getDisplayName(), RegexUtil.VANILLA_PATTERN);
				noColorNick = noColorNick.replace("*", "").toLowerCase();
				return noColorNick.startsWith(firstArg) || noColorNick.contains(firstArg);
			}
		}), new Function<UserData, String>() {
			@Override
			public String apply(UserData userData) {
				String noColorNick = RegexUtil.strip(userData.getDisplayName(), RegexUtil.VANILLA_PATTERN);
				noColorNick = noColorNick.replace("*", "");
				return noColorNick;
			}
		});

		return iterableToList(iterable.iterator());
	}

}