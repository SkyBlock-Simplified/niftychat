package net.netcoding.nifty.chat.listeners;

import net.netcoding.nifty.chat.cache.CensorData;
import net.netcoding.nifty.chat.cache.RankFormat;
import net.netcoding.nifty.chat.commands.Mute;
import net.netcoding.nifty.chat.events.PlayerJsonChatEvent;
import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.MinecraftHelper;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.Event;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.entity.living.human.Player;
import net.netcoding.nifty.common.minecraft.event.player.AsyncPlayerChatEvent;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.util.RegexUtil;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.core.util.json.JsonMessage;
import net.netcoding.nifty.chat.cache.UserChatData;

import java.util.regex.Pattern;

public class Chat extends MinecraftListener {

	public Chat(MinecraftPlugin plugin) {
		super(plugin);
	}

	public static boolean check(MinecraftHelper helper, Player player, String message) {
		MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByPlayer(player);
		UserChatData userData = UserChatData.getCache(profile);
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

		if (userData.getFlagData(Mute.FLAG).getValue() && !helper.hasPermissions(player, "mute", "roar")) {
			Mute.sendMutedError(helper.getLog(), player, userData);
			return false;
		}

		return true;
	}

	public static String format(MinecraftHelper helper, Player player, String action, String message) {
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

	public static String filter(MinecraftHelper helper, Player player, String action, String message) {
		if (!helper.hasPermissions(player, action, "bypass", "advertise")) {
			message = RegexUtil.IP_FILTER_PATTERN.matcher(message).replaceAll("*.*.*.*");

			while (RegexUtil.URL_FILTER_PATTERN.matcher(message).find())
				message = RegexUtil.URL_FILTER_PATTERN.matcher(message).replaceAll("$1 $2");
		}

		if (!helper.hasPermissions(player, action, "bypass", "url")) {
			while (RegexUtil.URL_PATTERN.matcher(message).find())
				message = RegexUtil.URL_PATTERN.matcher(message).replaceAll("$1 $2");
		}

		if (!helper.hasPermissions(player, action, "bypass", "caps")) {
			String[] words = message.split("\\s");

			for (int i = 0; i < words.length; i++) {
				if (words[i].length() > 3)
					words[i] = words[i].toLowerCase();
			}

			message = StringUtil.implode(" ", words);
		}

		if (!helper.hasPermissions(player, action, "bypass", "censor")) {
			String original = message;

			for (CensorData censor : CensorData.getCache()) {
				Pattern pattern = censor.getPattern();
				int current = 0;

				if (censor.isEnabled()) {
					while (pattern.matcher(message).find()) {
						try {
							message = RegexUtil.replace(message, pattern, censor.getReplace());
						} catch (Exception ex) {
							helper.getLog().console("Error detected in censor! Please check the censor {0} => {1} for message {2}", pattern.toString(), censor.getReplace(), original);
							censor.setEnabled(false);
							break;
						}

						if (++current >= 10) {
							helper.getLog().console("Possible infinite loop detected in censor! Please check the censor {0} => {1} for message {2}", pattern.toString(), censor.getReplace(), original);
							censor.setEnabled(false);
							break;
						}
					}
				}
			}
		}

		return message;
	}

	//@Event(priority = Event.Priority.LOWEST, ignoreCancelled = false)
	public void onLegacyPlayerChat(AsyncPlayerChatEvent event) {
		event.setCancelled(true); // Legacy Cancellation
		MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByPlayer(event.getPlayer());
		PlayerJsonChatEvent jsonChat = new PlayerJsonChatEvent(profile, event.getMessage());
		Nifty.getPluginManager().call(jsonChat);

		if (!jsonChat.isCancelled()) {
			for (MinecraftMojangProfile recipient : jsonChat.getRecipients()) {
				try {
					recipient.sendMessage(jsonChat.getMessage());
				} catch (Exception ignore) {
					// TODO: Do something?
				}
			}
		}
	}

	//@Event(priority = Event.Priority.LOWEST)
	public void onProfileJsonChat(PlayerJsonChatEvent event) {
		MinecraftMojangProfile profile = event.getProfile();
		JsonMessage message = event.getMessage();
		Player player = profile.getOfflinePlayer().getPlayer();
	}

	@Event(priority = Event.Priority.LOWEST)
	public void checkPlayerMessage(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		String message = event.getMessage();

		if (check(this, player, message) && this.hasPermissions(player, "chat")) {
			message = format(this, player, "chat", message);
			message = filter(this, player, "chat", message);
		} else
			event.setCancelled(true);

		event.setMessage(message);
	}

	@Event(priority = Event.Priority.LOW)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByPlayer(player);
		UserChatData userData = UserChatData.getCache(profile);

		String rank = userData.getRankData().getPrimaryRank();
		RankFormat rankInfo = RankFormat.getCache(rank);
		String format = rankInfo.getFormat();
		String group = rankInfo.getGroup();
		group = (group == null ? rank : group);
		String world = player.getWorld().getName();
		//Team team = player.getScoreboard().getPlayerTeam(profile.getOfflinePlayer());

		//String teamName = team != null ? team.getDisplayName() : "";
		//String teamPrefix = team != null ? team.getPrefix() : "";
		//String teamSuffix = team != null ? team.getSuffix() : "";
		// TODO

		event.setFormat(StringUtil.format(format, group, world, world.substring(0, 1).toUpperCase()/*, teamName, teamPrefix, teamSuffix*/));
	}

}