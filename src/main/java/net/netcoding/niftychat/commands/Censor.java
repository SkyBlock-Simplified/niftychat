package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.CensorData;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftycore.util.NumberUtil;
import net.netcoding.niftycore.util.RegexUtil;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftycore.util.concurrent.ConcurrentList;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Censor extends BukkitCommand {

	public Censor(JavaPlugin plugin) {
		super(plugin, "censor");
		this.editUsage(1, "remove", "<regex>");
		this.editUsage(1, "list", "");
		this.editUsage(1, "test", "<regex> <word> [replace]");
	}

	@Override
	protected void onCommand(final CommandSender sender, String alias, final String[] args) throws Exception {
		String action = args[0].toLowerCase();

		if (action.equalsIgnoreCase("list")) {
			if (!this.hasPermissions(sender, "censor", "list")) {
				this.getLog().error(sender, "You do not have permission to list the censor!");
				return;
			}

			if (CensorData.getCache().size() > 0) {
				ConcurrentList<CensorData> censorCache = CensorData.getCache();
				int rounded = NumberUtil.round(censorCache.size(), 5);
				int pages = rounded / 5;
				int page = args.length > 1 ? NumberUtil.isInt(args[1]) ? censorCache.size() > 5 ? Integer.parseInt(args[1]) : 0 : 0 : 0;

				if (page <= 0)
					page = 1;

				if (page * 5 > rounded)
					page = pages;

				int start = ((page - 1) * 5);
				int many = Math.min(censorCache.size() - start, 5);
				this.getLog().message(sender, "[{{0}} (Page {{1}}/{{2}})]", "Censor List", page, pages);

				for (int i = start; i < (start + many); i++) {
					CensorData censor = censorCache.get(i);
					this.getLog().message(sender, "{{0}} => {1}", ((censor.isEnabled() ? ChatColor.GREEN : "") + censor.getBadword()), censor.getReplace());
				}
			} else
				this.getLog().error(sender, "There are no words in the censor list");
		} else if (action.matches("^add|edit|remove|test$")) {
			if (!this.hasPermissions(sender, "censor", "manage")) {
				this.getLog().error(sender, "You do not have permission to manage the censor!");
				return;
			}

			if (args.length < 2) {
				this.showUsage(sender);
				return;
			}

			String badword = args[1].toLowerCase();

			try {
				Pattern.compile(badword);
			} catch (PatternSyntaxException ex) {
				this.getLog().error(sender, "{{0}} is invalid regex! {{1}}.", badword, ex.getMessage());
				return;
			}

			if (action.matches("^add|edit$")) {
				String replace = null;

				if (args.length > 2)
					replace = StringUtil.implode(" ", args, 2);

				if (NiftyChat.getSQL().update(StringUtil.format("INSERT INTO {0} (badword, _replace) VALUES (?, ?) ON DUPLICATE KEY UPDATE _replace = ?;", Config.CENSOR_TABLE), badword, replace, replace)) {
					String _replace = (StringUtil.isEmpty(replace) ? CensorData.DEFAULT_REPLACE : replace);

					if (action.equalsIgnoreCase("add"))
						new CensorData(badword, _replace);
					else
						CensorData.getCache(badword).setReplace(_replace);

					this.getLog().message(sender, "{{0}} now filters to {{1}}.", badword, _replace);
				}
			} else if (action.equalsIgnoreCase("remove")) {
				if (NiftyChat.getSQL().update(StringUtil.format("DELETE FROM {0} WHERE badword = ?;", Config.CENSOR_TABLE), badword)) {
					CensorData.removeCache(badword);
					this.getLog().message(sender, "Censored word {{0}} removed.", badword);
				} else
					this.getLog().error(sender, "{{0}} is not censored!", badword);
			} else if (action.equalsIgnoreCase("test")) {
				if (args.length < 4) {
					this.showUsage(sender);
					return;
				}

				String replace = args[args.length - 1];
				String message = StringUtil.implode(" ", args, 2, args.length - 1);
				String original = message;
				Pattern pattern = Pattern.compile(StringUtil.format("(?i)\\b{0}\\b", badword.replaceAll("%", "[\\\\S-]*")));
				int runs = 0;

				while (pattern.matcher(message).find()) {
					try {
						message = RegexUtil.replace(message, pattern, replace);
					} catch (Exception ex) {
						this.getLog().console(sender, "Error detected in censor! Please check the censor {0} => {1} for message {2}", pattern.toString(), replace, original);
						break;
					}

					if (++runs >= 10) {
						this.getLog().error(sender, "Possible infinite loop detected in censor! Please check the censor {0} => {1} for message {2}", pattern.toString(), replace, original);
						break;
					}
				}

				this.getLog().message(sender, "Regex: {{0}}", badword);
				this.getLog().message(sender, "Message: {{0}}", original);
				this.getLog().message(sender, "Filtered: {{0}}", message);
			}
		} else
			this.showUsage(sender);
	}

}