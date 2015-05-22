package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftycore.database.factory.callbacks.ResultCallback;
import net.netcoding.niftycore.util.RegexUtil;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftyranks.cache.UserRankData;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

public class Format extends BukkitCommand {

	public Format(JavaPlugin plugin) {
		super(plugin, "format");
		this.editUsage(1, "prefix", "<rank> [prefix]");
		this.editUsage(1, "suffix", "<rank> [suffix]");
		this.editUsage(1, "format", "<rank> [format]");
		this.editUsage(1, "group", "<rank> [group]");
		this.editUsage(1, "message", "<rank> [message]");
		this.editUsage(1, "create", "<rank> [group] [format]");
		this.editUsage(1, "delete", "<rank> [group] [format]");
		this.setMinimumArgsLength(2);
	}

	@Override
	protected void onCommand(final CommandSender sender, String alias, final String[] args) throws Exception {
		final String action = args[0];
		final String rank = args[1];
		final String _null = RegexUtil.SECTOR_SYMBOL + "onull";
		String format = null;

		if (action.matches("^create|delete$")) {
			if (args.length >= 2) {
				if (this.hasPermissions(sender, "format", "manage")) {
					String group = args.length >= 3 ? args[2] : "";

					if (args.length >= 4) {
						if (!args[3].matches("null|\"\"|''"))
							format = StringUtil.implode(" ", args, 3);
					}

					if (action.equalsIgnoreCase("create")) {
						try {
							NiftyChat.getSQL().update(StringUtil.format("INSERT INTO {0} (rank, _group, _format) VALUES (?, ?, ?);", Config.FORMAT_TABLE), rank, group, format);
						} catch (MySQLIntegrityConstraintViolationException ex) {
							this.getLog().error(sender, "The format entry for {{0}} already exists!", rank);
						}
					} else {
						if (!rank.matches("^default|message$"))
							NiftyChat.getSQL().update(StringUtil.format("DELETE FROM {0} WHERE rank = ?;", Config.FORMAT_TABLE), rank);
						else {
							this.getLog().error(sender, "You cannot delete the {{0}} format!", rank);
							return;
						}
					}

					this.getLog().message(sender, "The format entry for {{0}} has been {1}d.", rank, action);
				}
			} else
				this.showUsage(sender);
		} else if (action.matches("^prefix|suffix|format|group|message$")) {
			if (!rank.equalsIgnoreCase("message") && !UserRankData.rankExists(rank)) {
				this.getLog().error(sender, "The rank {{0}} does not exist!", rank);
				return;
			}

			if (args.length == 2) {
				if (this.hasPermissions(sender, "format", "view")) {
					String result = NiftyChat.getSQL().query(StringUtil.format("SELECT _{0} FROM {1} WHERE rank = ?;", action, Config.FORMAT_TABLE), new ResultCallback<String>() {
						@Override
						public String handle(ResultSet result) throws SQLException {
							return result.next() ? result.getString(StringUtil.format("_{0}", action)) : _null;
						}
					}, rank);

					this.getLog().message(sender, "The {0} for {{1}} is {{2}}.", action, rank, result);
				}
			} else {
				if (this.hasPermissions(sender, "format", "edit")) {
					format = StringUtil.implode(" ", args, 2);

					if (NiftyChat.getSQL().update(StringUtil.format("UPDATE {0} SET _{1} = ? WHERE rank = ?;", Config.FORMAT_TABLE, action), format, rank))
						this.getLog().message(sender, "The {0} for {{1}} has been set to {{2}}.", action, rank, (StringUtil.isEmpty(format) ? _null : format));
					else
						this.getLog().error(sender, "Unable to set {{0}} for {{1}}!", action, rank);
				}
			}
		} else
			this.showUsage(sender);
	}

}