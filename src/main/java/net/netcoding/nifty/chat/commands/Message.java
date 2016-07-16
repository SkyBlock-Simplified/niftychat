package net.netcoding.nifty.chat.commands;

import net.netcoding.nifty.chat.cache.Config;
import net.netcoding.nifty.chat.cache.RankFormat;
import net.netcoding.nifty.chat.cache.UserChatData;
import net.netcoding.nifty.chat.listeners.Chat;
import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftHelper;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.api.plugin.messaging.BungeeServer;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.minecraft.entity.living.human.Player;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.mojang.MojangProfile;
import net.netcoding.nifty.core.util.ListUtil;
import net.netcoding.nifty.core.util.RegexUtil;
import net.netcoding.nifty.core.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Message extends MinecraftListener {

	public Message(MinecraftPlugin plugin) {
		super(plugin);
	}

	public static void notifySpies(MinecraftHelper helper, String server, UserChatData senderData, UserChatData receiverData, String message) {
		for (UserChatData userData : UserChatData.getCache()) {
			if (userData.equals(senderData) || userData.equals(receiverData)) continue;
			if (!userData.getFlagData(SocialSpy.FLAG, server).getValue()) continue;
			if ((senderData.getFlagData(Vanish.FLAG).getValue() || receiverData.getFlagData(Vanish.FLAG).getValue()) && !helper.hasPermissions(userData.getOfflinePlayer().getPlayer(), "vanish", "see")) continue;
			send(helper, senderData.getProfile().getName(), receiverData.getProfile().getName(), userData.getProfile().getName(), message);
		}
	}

	public static boolean send(final MinecraftHelper helper, String senderName, String receiverName, String recipientName, String message) {
		UserChatData senderData = UserChatData.getCache(Nifty.getMojangRepository().searchByUsername(senderName)); // Sender
		UserChatData receiverData = UserChatData.getCache(Nifty.getMojangRepository().searchByUsername(receiverName)); // Receiver
		UserChatData recipientData = UserChatData.getCache(Nifty.getMojangRepository().searchByUsername(recipientName)); // Recipient

		if (recipientData.equals(senderData)) { // Sending
			boolean receiverOnline = receiverData.isOnlineAnywhere();

			if (!receiverOnline) {
				helper.getLog().error(senderData.getOfflinePlayer().getPlayer(), "Unable to locate {{0}}!", receiverData.getProfile().getName());
				return false;
			}

			if (senderData.getFlagData(Mute.FLAG).getValue() && !helper.hasPermissions(senderData.getOfflinePlayer().getPlayer(), "mute", "roar")) {
				Mute.sendMutedError(helper.getLog(), senderData.getOfflinePlayer().getPlayer(), senderData);
				return false;
			}

			if (receiverData.getFlagData(Vanish.FLAG).getValue() && !senderData.hasPermissions("vanish", "see")) {
				helper.getLog().error(senderData.getOfflinePlayer().getPlayer(), "Unable to locate {{0}}!", receiverData.getProfile().getName());
				return false;
			}
		} else if (recipientData.equals(receiverData)) { // Receiving
			receiverData.setLastMessenger(senderData.getProfile());

			if (!Nifty.getBungeeHelper().getDetails().isDetected())
				notifySpies(helper, "*", senderData, receiverData, message);
			else {
				Object[] data = new Object[] { "SpyMessage", senderData.getProfile().getName(), receiverData.getProfile().getName(), message };
				Nifty.getBungeeHelper().forward("ONLINE", Config.CHAT_CHANNEL, data);
				Nifty.getBungeeHelper().forward(Nifty.getBungeeHelper().getServerName(), Config.CHAT_CHANNEL, data);
			}
		}

		RankFormat format = RankFormat.getCache("message");
		String senderDisplayName = recipientData.getProfile().equals(senderData.getProfile()) ? RegexUtil.replaceColor("&7me", RegexUtil.REPLACE_ALL_PATTERN) : senderData.getDisplayName();
		String receiverDisplayName = recipientData.getProfile().equals(receiverData.getProfile()) ? RegexUtil.replaceColor("&7me", RegexUtil.REPLACE_ALL_PATTERN) : receiverData.getDisplayName();
		helper.getLog().message(recipientData.getOfflinePlayer().getPlayer(), format.getFormat(), senderDisplayName, receiverDisplayName, message);
		if (senderData.equals(recipientData)) helper.getLog().console(format.getFormat(), senderData.getDisplayName(), receiverData.getDisplayName(), message);
		return true;
	}

	@Command(name = "message",
			playerOnly = true,
			playerTabComplete = true,
			checkHelp = false,
			usages = {
					@Command.Usage(index = 0, match = "r(eply)?", replace = "[message]")
			}
	)
	public void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		Player player = (Player)source;
		MinecraftMojangProfile profile;
		boolean reply = alias.matches("^r(?:eply)?$");

		if (args.length < (reply ? 1 : 2)) {
			this.showUsage(source);
			return;
		}

		String playerName = args[0];
		String message = StringUtil.implode(" ", args, reply ? 0 : 1);
		UserChatData senderData = UserChatData.getCache(Nifty.getMojangRepository().searchByPlayer(player));

		if (reply) {
			MojangProfile lastMessenger = senderData.getLastMessenger();

			if (lastMessenger != null && lastMessenger.isOnline())
				playerName = lastMessenger.getName();
			else {
				this.getLog().error(source, "You have no one to reply to!");
				return;
			}
		}

		if (Chat.check(this, player, message)) {
			message = Chat.filter(this, player, "message", message);
			message = Chat.format(this, player, "message", message);
		} else
			return;

		if (isConsole(playerName)) {
			this.getLog().error(source, "You cannot message the console!");
			return;
		}


		Set<MinecraftMojangProfile> profiles = Realname.getProfileMatches(playerName);

		if (ListUtil.isEmpty(profiles)) {
			this.getLog().error(source, "Unable to locate the profile of {{0}}!", playerName);
			return;
		}

		profile = profiles.iterator().next();

		if (source.getName().equals(profile.getName())) {
			this.getLog().error(source, "You cannot message yourself!");
			return;
		}

		if (!Nifty.getBungeeHelper().getDetails().isDetected()) {
			if (send(this, source.getName(), profile.getName(), source.getName(), message))
				send(this, source.getName(), profile.getName(), profile.getName(), message);
		} else {
			BungeeServer server = Nifty.getBungeeHelper().getPlayerServer(profile);

			if (!server.equals(Nifty.getBungeeHelper().getServer())) {
				if (!this.hasPermissions(source, "message", "global")) {
					this.getLog().error(source, "You cannot send messages across servers!");
					return;
				}
			}

			if (send(this, source.getName(), profile.getName(), source.getName(), message))
				Nifty.getBungeeHelper().forward(senderData.getProfile(), server.getName(), Config.CHAT_CHANNEL, "Message", source.getName(), profile.getName(), message);
		}
	}

	@Command.TabComplete(index = 0, name = "message")
	protected List<String> onTabComplete(CommandSource source, String alias, String[] args) throws Exception {
		final String arg = args[0].toLowerCase();
		List<String> names = new ArrayList<>();

		if (Nifty.getBungeeHelper().getDetails().isDetected()) {
			for (BungeeServer<MinecraftMojangProfile> server : Nifty.getBungeeHelper().getServers()) {
				for (MinecraftMojangProfile profile : server.getPlayerList()) {
					UserChatData userData = UserChatData.getCache(profile);
					String displayName = userData.getStrippedDisplayName();

					if (displayName.toLowerCase().startsWith(arg) || displayName.toLowerCase().contains(arg))
						names.add(displayName);
				}
			}
		}

		return names;
	}

}