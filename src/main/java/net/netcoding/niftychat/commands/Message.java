package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.minecraft.messages.BungeeServer;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.RankFormat;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.listeners.Chat;
import net.netcoding.niftycore.mojang.MojangProfile;
import net.netcoding.niftycore.util.ListUtil;
import net.netcoding.niftycore.util.RegexUtil;
import net.netcoding.niftycore.util.StringUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Message extends BukkitCommand {

	public Message(JavaPlugin plugin) {
		super(plugin, "message");
		this.setPlayerOnly();
		this.setPlayerTabComplete();
		this.setCheckHelp(false);
		this.editUsage(0, "reply", "[message]");
		this.editUsage(0, "r", "[message]");
	}

	public static void notifySpies(BukkitHelper helper, String server, UserChatData senderData, UserChatData receiverData, String message) {
		for (UserChatData userData : UserChatData.getCache()) {
			if (userData.equals(senderData) || userData.equals(receiverData)) continue;
			if (!userData.getFlagData(SocialSpy.FLAG, server).getValue()) continue;
			if ((senderData.getFlagData(Vanish.FLAG).getValue() || receiverData.getFlagData(Vanish.FLAG).getValue()) && !helper.hasPermissions(userData.getOfflinePlayer().getPlayer(), "vanish", "see")) continue;
			send(helper, senderData.getProfile().getName(), receiverData.getProfile().getName(), userData.getProfile().getName(), message);
		}
	}

	public static boolean send(final BukkitHelper helper, String senderName, String receiverName, String recipientName, String message) {
		UserChatData senderData = UserChatData.getCache(NiftyBukkit.getMojangRepository().searchByUsername(senderName)); // Sender
		UserChatData receiverData = UserChatData.getCache(NiftyBukkit.getMojangRepository().searchByUsername(receiverName)); // Receiver
		UserChatData recipientData = UserChatData.getCache(NiftyBukkit.getMojangRepository().searchByUsername(recipientName)); // Recipient

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

			if (!NiftyBukkit.getBungeeHelper().isDetected())
				notifySpies(helper, "*", senderData, receiverData, message);
			else {
				Object[] data = new Object[] { "SpyMessage", senderData.getProfile().getName(), receiverData.getProfile().getName(), message };
				NiftyBukkit.getBungeeHelper().forward("ONLINE", Config.CHAT_CHANNEL, data);
				NiftyBukkit.getBungeeHelper().forward(NiftyBukkit.getBungeeHelper().getServerName(), Config.CHAT_CHANNEL, data);
			}
		}

		RankFormat format = RankFormat.getCache("message");
		String senderDisplayName = recipientData.getProfile().equals(senderData.getProfile()) ? RegexUtil.replaceColor("&7me", RegexUtil.REPLACE_ALL_PATTERN) : senderData.getDisplayName();
		String receiverDisplayName = recipientData.getProfile().equals(receiverData.getProfile()) ? RegexUtil.replaceColor("&7me", RegexUtil.REPLACE_ALL_PATTERN) : receiverData.getDisplayName();
		helper.getLog().message(recipientData.getOfflinePlayer().getPlayer(), format.getFormat(), senderDisplayName, receiverDisplayName, message);
		if (senderData.equals(recipientData)) helper.getLog().console(format.getFormat(), senderData.getDisplayName(), receiverData.getDisplayName(), message);
		return true;
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		Player player = (Player)sender;
		BukkitMojangProfile profile;
		boolean reply = alias.matches("^r(?:eply)?$");

		if (args.length < (reply ? 1 : 2)) {
			this.showUsage(sender);
			return;
		}

		String playerName = args[0];
		String message = StringUtil.implode(" ", args, reply ? 0 : 1);
		UserChatData senderData = UserChatData.getCache(NiftyBukkit.getMojangRepository().searchByPlayer(player));

		if (reply) {
			MojangProfile lastMessenger = senderData.getLastMessenger();

			if (lastMessenger != null && lastMessenger.isOnlineAnywhere())
				playerName = lastMessenger.getName();
			else {
				this.getLog().error(sender, "You have no one to reply to!");
				return;
			}
		}

		if (Chat.check(this, player, message)) {
			message = Chat.filter(this, player, "message", message);
			message = Chat.format(this, player, "message", message);
		} else
			return;

		if (isConsole(playerName)) {
			this.getLog().error(sender, "You cannot message the console!");
			return;
		}


		Set<BukkitMojangProfile> profiles = Realname.getProfileMatches(playerName);

		if (ListUtil.isEmpty(profiles)) {
			this.getLog().error(sender, "Unable to locate the profile of {{0}}!", playerName);
			return;
		}

		profile = profiles.iterator().next();

		if (sender.getName().equals(profile.getName())) {
			this.getLog().error(sender, "You cannot message yourself!");
			return;
		}

		if (!NiftyBukkit.getBungeeHelper().isDetected()) {
			if (send(this, sender.getName(), profile.getName(), sender.getName(), message))
				send(this, sender.getName(), profile.getName(), profile.getName(), message);
		} else {
			BungeeServer server = NiftyBukkit.getBungeeHelper().getPlayerServer(profile);

			if (!server.equals(NiftyBukkit.getBungeeHelper().getServer())) {
				if (!this.hasPermissions(sender, "message", "global")) {
					this.getLog().error(sender, "You cannot send messages across servers!");
					return;
				}
			}

			if (send(this, sender.getName(), profile.getName(), sender.getName(), message))
				NiftyBukkit.getBungeeHelper().forward(senderData.getProfile(), server.getName(), Config.CHAT_CHANNEL, "Message", sender.getName(), profile.getName(), message);
		}
	}

	@Override
	protected List<String> onTabComplete(final CommandSender sender, String alias, String[] args) throws Exception {
		final String arg = args[0].toLowerCase();
		List<String> names = new ArrayList<>();

		if (NiftyBukkit.getBungeeHelper().isDetected()) {
			for (BungeeServer server : NiftyBukkit.getBungeeHelper().getServers()) {
				for (BukkitMojangProfile profile : server.getPlayerList()) {
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