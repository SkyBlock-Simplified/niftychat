package net.netcoding.nifty.chat.commands;

import net.netcoding.nifty.chat.NiftyChat;
import net.netcoding.nifty.chat.cache.Config;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.core.util.RegexUtil;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.ranks.cache.UserRankData;

import java.sql.SQLIntegrityConstraintViolationException;

public class Format extends MinecraftListener {

	public Format(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "format",
			minimumArgs = 2,
			usages = {
					@Command.Usage(match = "(crea|dele)te", replace = "<rank> [group] [format]"),
					@Command.Usage(match = "message", replace = "<rank> [message]"),
					@Command.Usage(match = "group", replace = "<rank> [group]"),
					@Command.Usage(match = "format", replace = "<rank> [format]"),
					@Command.Usage(match = "suffix", replace = "<rank> [suffix]"),
					@Command.Usage(match = "prefix", replace = "<rank> [prefix]")
			}
	)
	protected void onCommand(CommandSource source, String alias, final String[] args) throws Exception {
		final String action = args[0];
		final String rank = args[1];
		final String _null = RegexUtil.SECTOR_SYMBOL + "onull";
		String format = null;

		if (action.matches("^create|delete$")) {
			if (args.length >= 2) {
				if (this.hasPermissions(source, "format", "manage")) {
					String group = args.length >= 3 ? args[2] : "";

					if (args.length >= 4) {
						if (!args[3].matches("null|\"\"|''"))
							format = StringUtil.implode(" ", args, 3);
					}

					if (action.equalsIgnoreCase("create")) {
						try {
							NiftyChat.getSQL().update(StringUtil.format("INSERT INTO {0} (rank, _group, _format) VALUES (?, ?, ?);", Config.FORMAT_TABLE), rank, group, format);
						} catch (SQLIntegrityConstraintViolationException ex) {
							this.getLog().error(source, "The format entry for {{0}} already exists!", rank);
						}
					} else {
						if (!rank.matches("^default|message$"))
							NiftyChat.getSQL().update(StringUtil.format("DELETE FROM {0} WHERE rank = ?;", Config.FORMAT_TABLE), rank);
						else {
							this.getLog().error(source, "You cannot delete the {{0}} format!", rank);
							return;
						}
					}

					this.getLog().message(source, "The format entry for {{0}} has been {1}d.", rank, action);
				}
			} else
				this.showUsage(source);
		} else if (action.matches("^prefix|suffix|format|group|message$")) {
			if (!rank.equalsIgnoreCase("message") && !UserRankData.rankExists(rank)) {
				this.getLog().error(source, "The rank {{0}} does not exist!", rank);
				return;
			}

			if (args.length == 2) {
				if (this.hasPermissions(source, "format", "view")) {
					String result = NiftyChat.getSQL().query(StringUtil.format("SELECT _{0} FROM {1} WHERE rank = ?;", action, Config.FORMAT_TABLE), result1 -> result1.next() ? result1.getString(StringUtil.format("_{0}", action)) : _null, rank);

					this.getLog().message(source, "The {0} for {{1}} is {{2}}.", action, rank, result);
				}
			} else {
				if (this.hasPermissions(source, "format", "edit")) {
					format = StringUtil.implode(" ", args, 2);
					format = (format.matches("^empty|none|null|\"\"|''$") ? null : format);

					if (NiftyChat.getSQL().update(StringUtil.format("UPDATE {0} SET _{1} = ? WHERE rank = ?;", Config.FORMAT_TABLE, action), format, rank))
						this.getLog().message(source, "The {0} for {{1}} has been set to {{2}}.", action, rank, (StringUtil.isEmpty(format) ? _null : format));
					else
						this.getLog().error(source, "Unable to set {{0}} for {{1}}!", action, rank);
				}
			}
		} else
			this.showUsage(source);
	}

}