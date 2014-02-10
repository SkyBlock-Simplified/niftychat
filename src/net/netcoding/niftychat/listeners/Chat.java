package net.netcoding.niftychat.listeners;

import static net.netcoding.niftychat.managers.Cache.Log;

import java.util.regex.Pattern;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.utilities.RegexUtil;
import net.netcoding.niftybukkit.utilities.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.managers.Cache;
import net.netcoding.niftychat.managers.CompiledCensor;
import net.netcoding.niftychat.managers.RankData;
import net.netcoding.niftychat.managers.UserData;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Team;

public class Chat extends BukkitListener {

	public Chat(NiftyChat plugin) {
		super(plugin);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void checkPlayerMessage(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		UserData userData = Cache.userData.get(player.getName());
		String stripMessage = RegexUtil.strip(event.getMessage(), RegexUtil.REPLACE_ALL_PATTERN);

		if ("".equals(stripMessage)) event.setCancelled(true);

		if (!userData.hasMoved()) {
			Log.error(player, "You must move before you can speak!");
			event.setCancelled(true);
		}

		if (userData.hasRepeatedMessage(stripMessage)) {
			Log.error(player, "You cannot send the same message!");
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		UserData userData = Cache.userData.get(player.getName());
		String message = event.getMessage();

		String rank = userData.getPrimaryRank();
		RankData rankInfo = Cache.rankData.get(rank);
		String format = rankInfo.getFormat();
		String group = rankInfo.getGroup();
		group = (group == null ? rank : group);
		String world = player.getWorld().getName();
		Team team = player.getScoreboard().getPlayerTeam(Bukkit.getOfflinePlayer(player.getName()));

		format = format.replace("{0}", group);
		format = format.replace("{1}", world);
		format = format.replace("{2}", world.substring(0, 1).toUpperCase());
		format = format.replace("{3}", (team == null ? "" : team.getPrefix()));
		format = format.replace("{4}", (team == null ? "" : team.getSuffix()));
		format = format.replace("{5}", (team == null ? "" : team.getDisplayName()));
		format = format.replaceAll("[ ]{2,}", " ");

		message = this.formatMessage(player, message);
		message = this.filterMessage(player, message);
		event.setMessage(message);

		synchronized (this) {
			event.setFormat(StringUtil.join(format, (userData.getDisplayName() + (RegexUtil.SECTOR_SYMBOL + "r")), message ));
		}
	}

	private String formatMessage(Player player, String message) {
		if (super.hasPermissions(player, "chat", "color"))
			message = RegexUtil.replaceColor(message, RegexUtil.REPLACE_COLOR_PATTERN);
		else
			message = RegexUtil.strip(message, RegexUtil.VANILLA_COLOR_PATTERN);

		if (super.hasPermissions(player, "chat", "magic"))
			message = RegexUtil.replaceColor(message, RegexUtil.REPLACE_MAGIC_PATTERN);
		else
			message = RegexUtil.strip(message, RegexUtil.VANILLA_MAGIC_PATTERN);

		if (super.hasPermissions(player, "chat", "format"))
			message = RegexUtil.replaceColor(message, RegexUtil.REPLACE_FORMAT_PATTERN);
		else
			message = RegexUtil.strip(message, RegexUtil.VANILLA_FORMAT_PATTERN);

		return message;
	}

	private String filterMessage(Player player, String message) {
		if (!super.hasPermissions(player, "chat", "bypass", "advertise")) {
			message = RegexUtil.IP_FILTER_PATTERN.matcher(message).replaceAll("*.*.*.*");

			while (RegexUtil.URL_FILTER_PATTERN.matcher(message).find())
				message = RegexUtil.URL_FILTER_PATTERN.matcher(message).replaceAll("$1 $2");
		}

		if (!super.hasPermissions(player, "chat", "bypass", "url")) {
			while (RegexUtil.URL_PATTERN.matcher(message).find())
				message = RegexUtil.URL_PATTERN.matcher(message).replaceAll("$1 $2");
		}

		if (!super.hasPermissions(player, "chat", "bypass", "censor")) {
			for (String badword : Cache.censorList.keySet()) {
				CompiledCensor censor = Cache.censorList.get(badword);
				Pattern pattern = censor.getPattern();

				while (pattern.matcher(message).find())
					message = RegexUtil.replace(message, pattern, censor.getReplace());
			}
		}

		if (!super.hasPermissions(player, "chat", "bypass", "caps")) {
			String[] words = message.split("\\s");

			for (int i = 0; i < words.length; i++)
				if (words[i].length() > 3) words[i] = words[i].toLowerCase();

			message = StringUtil.implode(" ", words);
		}

		return message;
	}

}