package net.netcoding.nifty.chat.commands;

import net.netcoding.nifty.chat.NiftyChat;
import net.netcoding.nifty.chat.cache.CensorData;
import net.netcoding.nifty.chat.cache.Config;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.util.NumberUtil;
import net.netcoding.nifty.core.util.RegexUtil;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.core.util.concurrent.ConcurrentList;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class Censor extends MinecraftListener {

	public Censor(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "censor",
			usages = {
					@Command.Usage(match = "add|edit", replace = "<regex> [replace]"),
					@Command.Usage(match = "rem(ove)?", replace = "<regex>"),
					@Command.Usage(match = "list"),
					@Command.Usage(match = "test", replace = "<regex> <message> <replace>"),
			}
	)
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		String action = args[0].toLowerCase();

		if (action.equalsIgnoreCase("list")) {
			if (CensorData.getCache().size() > 0) {
				ConcurrentList<CensorData> censorCache = CensorData.getCache();
				int rounded = NumberUtil.roundUp(censorCache.size(), 5);
				int pages = rounded / 5;
				int page = args.length > 1 ? NumberUtil.isNumber(args[1]) ? censorCache.size() > 5 ? Integer.parseInt(args[1]) : 0 : 0 : 0;

				if (page <= 0)
					page = 1;

				if (page * 5 > rounded)
					page = pages;

				int start = ((page - 1) * 5);
				int many = Math.min(censorCache.size() - start, 5);
				this.getLog().message(source, "[{{0}} (Page {{1}}/{{2}})]", "Censor List", page, pages);

				for (int i = start; i < (start + many); i++) {
					CensorData censor = censorCache.get(i);
					this.getLog().message(source, "{{0}} => {1}", ((censor.isEnabled() ? ChatColor.GREEN : "") + censor.getBadword()), censor.getReplace());
				}
			} else
				this.getLog().error(source, "There are no words in the censor list");
		} else if (action.matches("^add|edit|remove|test$")) {
			if (!this.hasPermissions(source, "censor", "manage")) {
				this.getLog().error(source, "You do not have permission to manage the censor!");
				return;
			}

			if (args.length < 2) {
				this.showUsage(source);
				return;
			}

			String badword = args[1].toLowerCase();

			try {
				Pattern.compile(badword);
			} catch (PatternSyntaxException ex) {
				this.getLog().error(source, "{{0}} is invalid regex! {{1}}.", badword, ex.getMessage());
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

					this.getLog().message(source, "{{0}} now filters to {{1}}.", badword, _replace);
				}
			} else if (action.equalsIgnoreCase("remove")) {
				if (NiftyChat.getSQL().update(StringUtil.format("DELETE FROM {0} WHERE badword = ?;", Config.CENSOR_TABLE), badword)) {
					CensorData.removeCache(badword);
					this.getLog().message(source, "Censored word {{0}} removed.", badword);
				} else
					this.getLog().error(source, "{{0}} is not censored!", badword);
			} else if (action.equalsIgnoreCase("test")) {
				if (args.length < 4) {
					this.showUsage(source);
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
						this.getLog().error(source, "Error detected in censor! Please check the censor {0} => {1} for message {2}", pattern.toString(), replace, original);
						break;
					}

					if (++runs >= 10) {
						this.getLog().error(source, "Possible infinite loop detected in censor! Please check the censor {0} => {1} for message {2}", pattern.toString(), replace, original);
						break;
					}
				}

				this.getLog().message(source, "Regex: {{0}}", badword);
				this.getLog().message(source, "Message: {{0}}", original);
				this.getLog().message(source, "Filtered: {{0}}", message);
			}
		} else
			this.showUsage(source);
	}

	@Command.TabComplete(index = 0, name = "censor")
	protected List<String> onTabComplete(CommandSource source, String alias, String[] args) throws Exception {
		ConcurrentList<CensorData> censorCache = CensorData.getCache();
		String arg = args[1].toLowerCase();
		return censorCache.stream().filter(data -> data.getBadword().startsWith(arg)).map(CensorData::getBadword).collect(Collectors.toList());
	}
}