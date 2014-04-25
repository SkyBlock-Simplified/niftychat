package net.netcoding.niftychat.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.CensorData;
import net.netcoding.niftychat.cache.Config;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Censor extends BukkitCommand {

	public Censor(NiftyChat plugin) {
		super(plugin, "censor");
	}

	@Override
	public void onCommand(final CommandSender sender, String alias, final String[] args) throws Exception {
		if (args.length >= 1) {
			String action = args[0].toLowerCase();

			if (action.equalsIgnoreCase("list")) {
				if (this.hasPermissions(sender, "censor", "list")) {
					List<String> censorList = new ArrayList<String>();

					if (CensorData.getCache().size() > 0) {
						for (CensorData censor : CensorData.getCache()) {
							String badword = censor.getBadword();
							String match = ChatColor.RED + badword + ChatColor.GRAY;
							String replace = censor.getReplace();
							replace = ChatColor.RED + replace + ChatColor.GRAY;
							censorList.add(StringUtil.format("{0} => {1}", match, replace));
						}

						this.getLog().message(sender, "[{{0}}]\n\n{1}", "Censor List", StringUtil.implode(", ", censorList));
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
							this.getLog().error(sender, "{{0}} is invalid regex! {{1}}.", badword, ex.getMessage());
							return;
						}
					}

					if (Cache.MySQL.update(StringUtil.format("INSERT INTO `{0}` (`badword`, `replace`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `replace` = ?;", Config.CENSOR_TABLE), badword, replace, replace)) {
						String _replace = (replace == null ? CensorData.DEFAULT_REPLACE : replace);

						if (action.equalsIgnoreCase("add"))
							new CensorData(badword, _replace);
						else
							CensorData.getCache(badword).setReplace(_replace);

						this.getLog().message(sender, "{{0}} now filters to {{1}}.", badword, _replace);
					}
				}
			} else if (action.equalsIgnoreCase("remove")) {
				if (this.hasPermissions(sender, "censor", "manage")) {
					String badword = args[1].toLowerCase();

					if (Cache.MySQL.update(StringUtil.format("DELETE FROM `{0}` WHERE `badword` = ?;", Config.CENSOR_TABLE), badword)) {
						CensorData.removeCache(badword);
						this.getLog().message(sender, "Censored word {{0}} removed.", badword);
					} else
						this.getLog().error(sender, "Unable to remove censored word!");
				}
			} else
				this.showUsage(sender);
		} else
			this.showUsage(sender);
	}

}