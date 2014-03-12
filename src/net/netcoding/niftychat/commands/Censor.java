package net.netcoding.niftychat.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.managers.Cache;
import net.netcoding.niftychat.managers.CensorData;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Censor extends BukkitCommand {

	public Censor(NiftyChat plugin) {
		super(plugin, "censor");
	}

	@Override
	public void command(final CommandSender sender, final String args[]) {
		if (this.hasPermissions(sender, "censor")) {
			if (args.length >= 1) {
				String action = args[0].toLowerCase();

				if (action.equalsIgnoreCase("list")) {
					if (this.hasPermissions(sender, "censor", "list")) {
						List<String> censorList = new ArrayList<String>();

						if (CensorData.getCache().size() > 0) {
							for (CensorData censor : CensorData.getCache()) {
								String badword = censor.getBadword();
								String match   = ChatColor.RED + badword + ChatColor.GRAY;
								String replace = censor.getReplace();
								replace        = ChatColor.RED + replace + ChatColor.GRAY;
								censorList.add(String.format("%1$s => %2$s", match, replace));
							}

							this.getLog().message(sender, "[{%1$s}]\n\n%2$s", "Censor List", StringUtil.implode(", ", censorList));
						} else
							this.getLog().error(sender, "There are no words in the censor list");
					}
				} else if (action.matches("^add|edit$")) {
					if (this.hasPermissions(sender, "censor", "manage")) {
						String badword = args[1].toLowerCase();
						String replace = null;
						if (args.length > 2) replace  = StringUtil.implode(" ", args, 2);

						if (action.equalsIgnoreCase("add")) {
							try {
								Pattern.compile(badword);
							} catch (PatternSyntaxException ex) {
								this.getLog().error(sender, "{%1$s} is invalid regex! {%2$s}", badword, ex.getMessage());
								return;
							}
						}

						try {
							if (Cache.MySQL.update("INSERT INTO `nc_censor` (`badword`,  `replace`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `replace` = ?;", badword, replace, replace)) {
								String _replace = (replace == null ? CensorData.DEFAULT_REPLACE : replace);

								if (action.equalsIgnoreCase("add"))
									new CensorData(badword, _replace);
								else
									CensorData.getCache(badword).setReplace(_replace);

								this.getLog().message(sender, "{%1$s} now filters to {%2$s}", badword, _replace);
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				} else if (action.equalsIgnoreCase("remove")) {
					if (this.hasPermissions(sender, "censor", "manage")) {
						String badword = args[1].toLowerCase();

						try {
							if (Cache.MySQL.update("DELETE FROM `nc_censor` WHERE `badword` = ?;", badword)) {
								CensorData.removeCache(badword);
								this.getLog().message(sender, "Censored word {%1$s} removed", badword);
							} else
								this.getLog().error(sender, "Unable to remove censored word!");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} else
					this.showUsage(sender);
			} else
				this.showUsage(sender);
		}
	}

}