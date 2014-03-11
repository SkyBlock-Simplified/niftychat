package net.netcoding.niftychat.commands;

import java.sql.SQLException;

import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.util.StringUtil;
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
		if (this.hasPermissions(sender, "rank")) {
			String action = args[0];

			if (action.matches("^create|edit$")) {
				if (this.hasPermissions(sender, "rank", "manage")) {
					if (args.length >= 2 && args.length <= 3) {
						String rank  = args[1];
						String group = (args.length == 2 ? rank : args[2]);

						if (Cache.MySQL.update("INSERT INTO `nc_ranks` (`rank`, `group`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `group` = ?;", rank, group, group)) {
							String create = "edited";

							if (action.equalsIgnoreCase("create")) {
								create = "created";
								new RankData(rank, group, "");
							} else
								RankData.getCache(rank).setGroup(group);

							this.getLog().message(sender, "The rank {%1$s} has been %2$s.", rank, create);
						} else
							this.showUsage(sender);
					} else
						this.showUsage(sender);
				}
			} else if (action.matches("^add|remove$")) {
				this.getLog().error(sender, "Multiple ranks per player is currently unsupported.");
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
						this.getLog().message(sender, "{%1$s} %2$s {%2$s}.", foundName, complete, rank);
					} else
						this.getLog().error(sender, "{%1$s} is an invalid rank!", rank);
				} else
					this.showUsage(sender);*/
			} else if (action.equalsIgnoreCase("check")) {
				if (this.hasPermissions(sender, "rank", "check")) {
					if (args.length == 2) {
						String user = args[1];
						this.getLog().message(sender, "{%1$s} is a member of {%2$s}", user, StringUtil.implode(",", UserData.getOfflineRanks(user)));
					} else
						this.showUsage(sender);
				}
			} else {
				if (this.hasPermissions(sender, "rank", "set")) {
					if (BukkitHelper.isConsole(sender) && args.length < 2)
						this.getLog().error(sender, "Changing player ranks requires a player name when used by the console!");
					else if (args.length >= 1 && args.length <= 2) {
						String user      = (args.length == 2 ? args[0] : sender.getName());
						String rank      = (args.length == 2 ? args[1] : args[0]);

						try {
							String matchedName = UserData.setRank(user, rank);

							if (!"".equals(matchedName))
								this.getLog().message(sender, "{%1$s} has been ranked to {%2$s}", matchedName, rank);
							else
								this.getLog().error(sender, "Player {%1$s} not found!", matchedName);
						} catch (Exception ex) {
							this.getLog().error(sender, "{%1$s} is an invalid rank!", rank);
						}
					} else
						this.showUsage(sender);
				}
			}
		}
	}

}