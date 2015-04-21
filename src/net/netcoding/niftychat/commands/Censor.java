package net.netcoding.niftychat.commands;

import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.util.NumberUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftybukkit.util.concurrent.ConcurrentSet;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.CensorData;
import net.netcoding.niftychat.cache.Config;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Censor extends BukkitCommand {

	public Censor(JavaPlugin plugin) {
		super(plugin, "censor");
	}

	@Override
	protected void onCommand(final CommandSender sender, String alias, final String[] args) throws Exception {
		String action = args[0].toLowerCase();

		if (action.equalsIgnoreCase("list")) {
			if (this.hasPermissions(sender, "censor", "list")) {
				ConcurrentSet<CensorData> censorCache = CensorData.getCache();

				if (censorCache.size() > 0) {
					int total = (int)Math.floor(censorCache.size() / 5.0);
					int page = args.length > 1 ? NumberUtil.isInt(args[1]) ? censorCache.size() > 5 ? Integer.parseInt(args[1]) : 0 : 0 : 0;
					if (page == 0) page = 1;
					if (page * 5 > censorCache.size()) page = total - 1;
					Iterator<CensorData> totalIterator = censorCache.iterator();

					if (censorCache.size() > 5 && page > 1) {
						for (int i = 0; i < (5 * page); i++)
							totalIterator.next();
					}

					this.getLog().message(sender, "[{{0}} (Page {{1}}/{{2}})]", "Censor List", page, total);
					for (int i = 0; i < 5 && totalIterator.hasNext(); i++) {
						CensorData censor = totalIterator.next();
						this.getLog().message(sender, "{{0}} => {1}", ((censor.isEnabled() ? ChatColor.GREEN : "") + censor.getBadword()), censor.getReplace());
					}
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

				if (NiftyChat.getSQL().update(StringUtil.format("INSERT INTO {0} (badword, _replace) VALUES (?, ?) ON DUPLICATE KEY UPDATE _replace = ?;", Config.CENSOR_TABLE), badword, replace, replace)) {
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

				if (NiftyChat.getSQL().update(StringUtil.format("DELETE FROM {0} WHERE badword = ?;", Config.CENSOR_TABLE), badword)) {
					CensorData.removeCache(badword);
					this.getLog().message(sender, "Censored word {{0}} removed.", badword);
				} else
					this.getLog().error(sender, "Unable to remove censored word!");
			}
		} else
			this.showUsage(sender);
	}

}