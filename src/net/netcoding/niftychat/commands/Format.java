package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.util.RegexUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.RankFormat;

import org.bukkit.command.CommandSender;

public class Format extends BukkitCommand {

	public Format(NiftyChat plugin) {
		super(plugin, "format");
	}

	@Override
	public void onCommand(final CommandSender sender, String alias, final String[] args) throws SQLException {
		if (args.length >= 2) {
			final String rank   = args[0].toLowerCase();
			final String action = args[1].toLowerCase();

			if (action.matches("^prefix|suffix|format$")) {
				Cache.MySQL.query(StringUtil.format("SELECT * FROM `{0}` WHERE `rank` = ? LIMIT 1;", Config.FORMAT_TABLE), new ResultCallback<Void>() {
					@Override
					public Void handle(ResultSet result) throws SQLException {
						if (result.next()) {
							String format  = null;
							String _format = null;
							String _null   = RegexUtil.SECTOR_SYMBOL + "onull";
							String now     = "is";

							if (args.length == 2) {
								if (hasPermissions(sender, "format", "view")) {
									format  = result.getString(action);
									_format = format;

									if (result.wasNull()) {
										format  = null;
										_format = _null;
									}
								}
							} else if (args.length >= 3) {
								if (hasPermissions(sender, "format", "manage")) {
									format  = StringUtil.implode(" ", args, 2);
									_format = format;

									if (format.matches("null|\"\"|''")) {
										format  = null;
										_format = _null;
									}

									if (Cache.MySQL.update(StringUtil.format("UPDATE `{0}` SET `{1}` = ? WHERE `rank` = ?;", Config.FORMAT_TABLE, action), format, rank)) {
										now = "set to";
										RankFormat rankInfo = RankFormat.getCache(rank);

										if (action.equalsIgnoreCase("prefix"))
											rankInfo.setPrefix(format);
										else if (action.equalsIgnoreCase("suffix"))
											rankInfo.setSuffix(format);
										else if (action.equalsIgnoreCase("format"))
											rankInfo.setFormat(format);
									} else {
										getLog().error(sender, "Unable to set format for {{0}}!", rank);
										return null;
									}
								}
							}

							String proper = Character.toUpperCase(action.charAt(0)) + action.substring(1);
							getLog().message(sender, "{{0}} of {{1}} {2} {{3}}", proper, rank, now, _format);
						} else
							getLog().error(sender, "{{0}} is not a valid rank!", rank);

						return null;
					}
				}, rank);
			} else
				this.showUsage(sender);
		} else
			this.showUsage(sender);
	}

}