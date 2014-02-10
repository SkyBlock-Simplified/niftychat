package net.netcoding.niftychat.commands;

import static net.netcoding.niftychat.managers.Cache.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.utilities.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.managers.Cache;
import net.netcoding.niftychat.managers.CompiledCensor;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Censor extends BukkitCommand {

	public Censor(NiftyChat plugin) {
		super(plugin, "censor");
	}

	@Override
	public void command(final CommandSender sender, final String args[]) {
		if (super.hasPermissions(sender, "censor")) {
			if (args.length >= 1) {
				String action = args[0].toLowerCase();

				if (action.equalsIgnoreCase("list")) {
					if (super.hasPermissions(sender, "censor", "list")) {
						List<String> censorList = new ArrayList<String>();

						if (Cache.censorList.size() > 0) {
							for (String badword : Cache.censorList.keySet()) {
								CompiledCensor censor = Cache.censorList.get(badword);
								String match          = ChatColor.RED + badword + ChatColor.GRAY;
								String replace        = censor.getReplace();
								replace               = ChatColor.RED + replace + ChatColor.GRAY;
								censorList.add(String.format("%1$s => %2$s", match, replace));
							}

							Log.message(sender, "[{%1$s}]\n\n%2$s", "Censor List", StringUtil.implode(", ", censorList));
						} else
							Log.error(sender, "There are no words in the censor list");
					}
				} else if (action.matches("^add|edit$")) {
					if (super.hasPermissions(sender, "censor", "manage")) {
						String badword = args[1].toLowerCase();
						String replace = null;
						if (args.length > 2) replace  = StringUtil.implode(" ", args, 2);

						if (action.equalsIgnoreCase("add")) {
							try {
								Pattern.compile(badword);
							} catch (PatternSyntaxException ex) {
								Log.error(sender, "{%1$s} is invalid regex! {%2$s}", badword, ex.getMessage());
								return;
							}
						}

						try {
							if (Cache.MySQL.update("INSERT INTO `nc_censor` (`badword`,  `replace`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `replace` = ?;", badword, replace, replace)) {
								String _replace = (replace == null ? CompiledCensor.DEFAULT_REPLACE : replace);

								if (action.equalsIgnoreCase("add"))
									Cache.censorList.put(badword, new CompiledCensor(badword, _replace));
								else
									Cache.censorList.get(badword).setReplace(_replace);

								Log.message(sender, "{%1$s} now filters to {%2$s}", badword, _replace);
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				} else if (action.equalsIgnoreCase("remove")) {
					if (super.hasPermissions(sender, "censor", "manage")) {
						String badword = args[1].toLowerCase();

						try {
							if (Cache.MySQL.update("DELETE FROM `nc_censor` WHERE `badword` = ?;", badword)) {
								Cache.censorList.remove(badword);
								Log.message(sender, "Censored word {%1$s} removed", badword);
							} else
								Log.error(sender, "Unable to remove censored word!");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} else
					super.showUsage(sender);
			} else
				super.showUsage(sender);
		}
	}

}