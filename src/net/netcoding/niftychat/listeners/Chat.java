package net.netcoding.niftychat.listeners;

import java.util.regex.Pattern;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.util.RegexUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.CensorData;
import net.netcoding.niftychat.cache.RankFormat;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.commands.Mute;

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

	public static boolean check(BukkitHelper helper, Player player, String message) {
		MojangProfile profile = NiftyBukkit.getMojangRepository().searchByExactPlayer(player);
		UserChatData userData = UserChatData.getCache(profile.getUniqueId());
		String stripMessage = RegexUtil.strip(message, RegexUtil.REPLACE_ALL_PATTERN);

		if (StringUtil.isEmpty(stripMessage)) return false;

		if (!userData.hasMoved()) {
			helper.getLog().error(player, "You must move before you can speak!");
			return false;
		}

		if (userData.hasRepeatedMessage(stripMessage)) {
			helper.getLog().error(player, "You cannot send the same message!");
			return false;
		}

		if (userData.getFlagData("muted").getValue() && !helper.hasPermissions(player, "mute", "roar")) {
			Mute.sendMutedError(helper.getLog(), player, userData);
			return false;
		}

		return true;
	}

	public static String format(BukkitHelper helper, Player player, String action, String message) {
		if (helper.hasPermissions(player, action, "color"))
			message = RegexUtil.replaceColor(message, RegexUtil.REPLACE_COLOR_PATTERN);
		else
			message = RegexUtil.strip(message, RegexUtil.VANILLA_COLOR_PATTERN);

		if (helper.hasPermissions(player, action, "magic"))
			message = RegexUtil.replaceColor(message, RegexUtil.REPLACE_MAGIC_PATTERN);
		else
			message = RegexUtil.strip(message, RegexUtil.VANILLA_MAGIC_PATTERN);

		if (helper.hasPermissions(player, action, "format"))
			message = RegexUtil.replaceColor(message, RegexUtil.REPLACE_FORMAT_PATTERN);
		else
			message = RegexUtil.strip(message, RegexUtil.VANILLA_FORMAT_PATTERN);

		return message;
	}

	public static String filter(BukkitHelper helper, Player player, String action, String message) {
		if (!helper.hasPermissions(player, action, "bypass", "advertise")) {
			message = RegexUtil.IP_FILTER_PATTERN.matcher(message).replaceAll("*.*.*.*");

			while (RegexUtil.URL_FILTER_PATTERN.matcher(message).find())
				message = RegexUtil.URL_FILTER_PATTERN.matcher(message).replaceAll("$1 $2");
		}

		if (!helper.hasPermissions(player, action, "bypass", "url")) {
			while (RegexUtil.URL_PATTERN.matcher(message).find())
				message = RegexUtil.URL_PATTERN.matcher(message).replaceAll("$1 $2");
		}

		if (!helper.hasPermissions(player, action, "bypass", "censor")) {
			for (CensorData censor : CensorData.getCache()) {
				Pattern pattern = censor.getPattern();

				while (pattern.matcher(message).find())
					message = RegexUtil.replace(message, pattern, censor.getReplace());
			}
		}

		if (!helper.hasPermissions(player, action, "bypass", "caps")) {
			String[] words = message.split("\\s");

			for (int i = 0; i < words.length; i++)
				if (words[i].length() > 3) words[i] = words[i].toLowerCase();

			message = StringUtil.implode(" ", words);
		}

		return message;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void checkPlayerMessage(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		String message = event.getMessage();

		if (check(this, player, message)) {
			message = format(this, player, "chat", message);
			message = filter(this, player, "chat", message);
		} else
			event.setCancelled(true);

		event.setMessage(message);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		MojangProfile profile = NiftyBukkit.getMojangRepository().searchByExactPlayer(player);
		UserChatData userData = UserChatData.getCache(profile.getUniqueId());

		String rank = userData.getRankData().getPrimaryRank();
		RankFormat rankInfo = RankFormat.getCache(rank);
		String format = rankInfo.getFormat();
		String group = rankInfo.getGroup();
		group = (group == null ? rank : group);
		String world = player.getWorld().getName();
		Team team = player.getScoreboard().getPlayerTeam(Bukkit.getOfflinePlayer(profile.getUniqueId()));

		String teamName = team != null ? team.getDisplayName() : "";
		String teamPrefix = team != null ? team.getPrefix() : "";
		String teamSuffix = team != null ? team.getSuffix() : "";

		synchronized (this) {
			event.setFormat(StringUtil.format(format, group, world, world.substring(0, 1).toUpperCase(), teamName, teamPrefix, teamSuffix));
		}
	}

}