package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.netcoding.niftybukkit.database.ResultSetCallback;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.utilities.RegexUtil;
import net.netcoding.niftybukkit.utilities.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.managers.Cache;
import net.netcoding.niftychat.managers.RankData;

import org.bukkit.command.CommandSender;

public class Format extends BukkitCommand {

	public Format(NiftyChat plugin) {
		super(plugin, "format");
	}

	@Override
	public void command(final CommandSender sender, final String[] args) {
		if (this.hasPermissions(sender,  "format")) {
			if (args.length >= 2) {
				final String rank   = args[0].toLowerCase();
				final String action = args[1].toLowerCase();

				if (action.matches("^prefix|suffix|format$")) {
					try {
						Cache.MySQL.query("SELECT * FROM `nc_ranks` WHERE `rank` = ? LIMIT 1;", new ResultSetCallback() {
							@Override
							public Object handleResult(ResultSet result) throws SQLException, Exception {
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

											if (Cache.MySQL.update(String.format("UPDATE `nc_ranks` SET `%1$s` = ? WHERE `rank` = ?;", action), format, rank)) {
												now = "set to";
												RankData rankInfo = Cache.rankData.get(rank);

												if (action.equalsIgnoreCase("prefix"))
													rankInfo.setPrefix(format);
												else if (action.equalsIgnoreCase("suffix"))
													rankInfo.setSuffix(format);
												else if (action.equalsIgnoreCase("format"))
													rankInfo.setFormat(format);
											} else {
												getLog().error(sender, "Unable to set format for {%1$s}!", rank);
												return false;
											}
										}
									}

									String proper = Character.toUpperCase(action.charAt(0)) + action.substring(1);
									getLog().message(sender, "{%1$s} of {%2$s} %3$s {%4$s}", proper, rank, now, _format);
									return true;
								} else
									getLog().error(sender, "{%1$s} is not a valid rank!", rank);

								return false;
							}
						}, rank);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else
					this.showUsage(sender);
			} else
				this.showUsage(sender);
		}
	}

}