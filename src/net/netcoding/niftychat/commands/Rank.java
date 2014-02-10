package net.netcoding.niftychat.commands;

import static net.netcoding.niftychat.managers.Cache.Log;

import java.sql.SQLException;

import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.utilities.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.managers.Cache;
import net.netcoding.niftychat.managers.RankData;
import net.netcoding.niftychat.managers.UserData;

import org.bukkit.command.CommandSender;

public class Rank extends BukkitCommand {

	public Rank(NiftyChat plugin) {
		super(plugin, "rank");
		this.editUsage(1, "create", "<rank> [group]");
		this.editUsage(1, "edit", "<rank> [group]");
		this.editUsage(1, "check", "<player>");
		this.editUsage(1, "player", "<rank>");
	}

	@Override
	public void command(CommandSender sender, String args[]) throws SQLException, Exception {
		if (super.hasPermissions(sender, "rank")) {
			String action = args[0];

			if (action.matches("^create|edit$")) {
				if (super.hasPermissions(sender, "rank", "manage")) {
					if (args.length >= 2 && args.length <= 3) {
						String rank  = args[1];
						String group = (args.length == 2 ? rank : args[2]);

						if (Cache.MySQL.update("INSERT INTO `nc_ranks` (`rank`, `group`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `group` = ?;", rank, group, group)) {
							String create = "edited";

							if (action.equalsIgnoreCase("create")) {
								create = "created";
								Cache.rankData.put(rank, new RankData(rank, group));
							} else
								Cache.rankData.get(rank).setGroup(group);

							Log.message(sender, "The rank {%1$s} has been %2$s.", rank, create);
						} else
							super.showUsage(sender);
					} else
						super.showUsage(sender);
				}
			} else if (action.matches("^add|remove$")) {
				Log.error(sender, "Multiple ranks per player is currently unsupported.");
				/*if (args.length >= 2 && args.length <= 3) {
					String user      = (args.length == 3 ? args[1] : sender.getName());
					String rank      = (args.length == 3 ? args[2] : args[1]);
					String foundName = null;
					String complete  = "now has the rank of";

					if (action.equalsIgnoreCase("add"))
						foundName = User.addRank(user, rank);
					else if (action.equalsIgnoreCase("remove")) {
						foundName = User.removeRank(user, rank);
						complete  = "no longer has the rank of";
					}

					if (foundName != null) {
						Log.message(sender, "{%1$s} %2$s {%2$s}.", foundName, complete, rank);
					} else
						Log.error(sender, "{%1$s} is an invalid rank!", rank);
				} else
					super.showUsage(sender);*/
			} else if (action.equalsIgnoreCase("check")) {
				if (super.hasPermissions(sender, "rank", "check")) {
					if (args.length == 2) {
						String user = args[1];
						Log.message(sender, "{%1$s} is a member of {%2$s}", user, StringUtil.implode(",", UserData.getOfflineRanks(user)));
					} else
						super.showUsage(sender);
				}
			} else {
				if (super.hasPermissions(sender, "rank", "set")) {
					if (BukkitHelper.isConsole(sender) && args.length < 2)
						Log.error(sender, "Changing player ranks requires a player name when used by the console!");
					else if (args.length >= 1 && args.length <= 2) {
						String user      = (args.length == 2 ? args[0] : sender.getName());
						String rank      = (args.length == 2 ? args[1] : args[0]);

						try {
							String matchedName = UserData.setRank(user, rank);

							if (!"".equals(matchedName))
								Log.message(sender, "{%1$s} has been ranked to {%2$s}", matchedName, rank);
							else
								Log.error(sender, "Player {%1$s} not found!", matchedName);
						} catch (Exception ex) {
							Log.error(sender, "{%1$s} is an invalid rank!", rank);
						}
					} else
						super.showUsage(sender);
				}
			}
		}
	}

}