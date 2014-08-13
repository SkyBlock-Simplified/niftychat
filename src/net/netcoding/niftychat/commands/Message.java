package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.minecraft.BungeeServer;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftybukkit.util.RegexUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.RankFormat;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.listeners.Chat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Message extends BukkitCommand {

	public Message(JavaPlugin plugin) {
		super(plugin, "message");
		this.setPlayerOnly();
		this.editUsage(0, "reply", "[message]");
		this.editUsage(0, "r", "[message]");
	}

	private static void notifySpies(final BukkitHelper helper, final UserChatData senderData, final UserChatData receiverData, final String message) {
		helper.getPlugin().getServer().getScheduler().runTaskAsynchronously(helper.getPlugin(), new Runnable() {
			@Override
			public void run() {
				try {
					RankFormat message = RankFormat.getCache("message");
					List<MojangProfile> spies = Cache.MySQL.query(StringUtil.format("SELECT `uuid` FORM {0} WHERE `flag` = ?;", Config.USER_FLAGS_TABLE), new ResultCallback<List<MojangProfile>>() {
						@Override
						public List<MojangProfile> handle(ResultSet result) throws SQLException {
							List<MojangProfile> profiles = new ArrayList<>();
							while (result.next()) profiles.add(NiftyBukkit.getMojangRepository().searchByExactUUID(UUID.fromString(result.getString("uuid"))));
							return profiles;
						}
					}, "spying");

					for (MojangProfile spy : spies) {

						if (!NiftyBukkit.getBungeeHelper().isOnline()) {
							UserChatData spyData = UserChatData.getCache(spy.getUniqueId());
							spyData = spyData == null ? new UserChatData(helper.getPlugin(), spy) : spyData;
							if (spyData.getPlayer() != null) helper.getLog().message(spyData.getPlayer(), message.getFormat(), senderData.getDisplayName(), receiverData.getDisplayName(), message);
						} else {
							BungeeServer server = NiftyBukkit.getBungeeHelper().getPlayerServer(NiftyBukkit.getMojangRepository().searchByExactUUID(spy.getUniqueId()));
							if (server != null) NiftyBukkit.getBungeeHelper().forward(findPlayer(receiverData.getName()), server.getName(), Config.CHAT_CHANNEL, "SpyMessage", senderData.getName(), receiverData.getName(), spy.getName(), message);
						}
					}
				} catch (Exception ex) {
					helper.getLog().console(ex);
				}
			}
		});
	}

	public static boolean send(final BukkitHelper helper, String senderName, String receiverName, String recipientName, String message) {
		// Message sender
		MojangProfile senderProfile = NiftyBukkit.getMojangRepository().searchByExactUsername(senderName);
		UserChatData senderData = UserChatData.getCache(senderProfile.getUniqueId());
		senderData = senderData == null ? new UserChatData(helper.getPlugin(), senderProfile) : senderData;

		// Message receiver
		MojangProfile receiverProfile = NiftyBukkit.getMojangRepository().searchByExactUsername(receiverName);
		UserChatData receiverData = UserChatData.getCache(receiverProfile.getUniqueId());
		receiverData = receiverData == null ? new UserChatData(helper.getPlugin(), receiverProfile) : receiverData;

		// Where message is sent
		MojangProfile recipientProfile = NiftyBukkit.getMojangRepository().searchByExactUsername(recipientName);
		UserChatData recipientData = UserChatData.getCache(recipientProfile.getUniqueId());
		recipientData = recipientData == null ? new UserChatData(helper.getPlugin(), recipientProfile) : recipientData;

		if (recipientProfile.equals(senderProfile)) { // Sending
			if (receiverData.getPlayer() == null) {
				helper.getLog().error(senderData.getPlayer(), "Unable to locate {{0}}!", receiverData.getName());
				return false;
			}

			if (senderData.getFlagData("muted").getValue() && !helper.hasPermissions(senderData.getPlayer(), "mute", "roar")) {
				Mute.sendMutedError(helper.getLog(), senderData.getPlayer(), senderData);
				return false;
			}

			if (receiverData.getFlagData("vanished").getValue() && !senderData.hasPermissions("vanish", "interact")) {
				helper.getLog().error(senderData.getPlayer(), "Unable to locate {{0}}!", receiverData.getName());
				return false;
			}
		} else if (!recipientProfile.equals(receiverProfile)) { // Spying
			if (senderData.getFlagData("vanished").getValue() && !helper.hasPermissions(recipientData.getPlayer(), "vanish", "spy"))
				return false;
		}

		if (recipientProfile.equals(receiverProfile)) receiverData.setLastMessenger(senderProfile);
		RankFormat format = RankFormat.getCache("message");
		String senderDisplayName = recipientProfile.equals(senderProfile) ? RegexUtil.replaceColor("&7me", RegexUtil.REPLACE_ALL_PATTERN) : senderData.getDisplayName();
		String receiverDisplayName = recipientProfile.equals(receiverProfile) ? RegexUtil.replaceColor("&7me", RegexUtil.REPLACE_ALL_PATTERN) : receiverData.getDisplayName();
		helper.getLog().message(recipientData.getPlayer(), format.getFormat(), senderDisplayName, receiverDisplayName, message);
		notifySpies(helper, senderData, receiverData, message);
		return true;
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		Player player = (Player)sender;
		String playerName = args[0];
		MojangProfile profile;
		boolean reply = alias.matches("^r(?:eply)?$");
		String message = StringUtil.implode(" ", args, reply ? 0 : 1);
		MojangProfile senderprofile = NiftyBukkit.getMojangRepository().searchByExactPlayer(player);
		UserChatData senderData = UserChatData.getCache(senderprofile.getUniqueId());

		if (reply) {
			MojangProfile lastMessenger = senderData.getLastMessenger();

			if (lastMessenger != null) {
				playerName = lastMessenger.getName();
			} else {
				this.getLog().error(sender, "You have no one to reply to!");
				return;
			}
		} else {
			if (args.length <= 1) {
				this.showUsage(sender);
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

		try {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName)[0];
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(sender, "Unable to locate the uuid of {{0}}!", playerName);
			return;
		}

		if (sender.getName().equals(profile.getName())) {
			this.getLog().error(sender, "You cannot message yourself!");
			return;
		}

		if (!NiftyBukkit.getBungeeHelper().isOnline()) {
			if (send(this, sender.getName(), profile.getName(), sender.getName(), message))
				send(this, sender.getName(), profile.getName(), profile.getName(), message);
		} else {
			if (NiftyBukkit.getBungeeHelper().isPlayerOnline(profile)) {
				BungeeServer server = NiftyBukkit.getBungeeHelper().getPlayerServer(profile);

				if (!server.equals(NiftyBukkit.getBungeeHelper().getServer())) {
					if (!this.hasPermissions(sender, "message", "global")) {
						this.getLog().error(sender, "You cannot send messages across servers!");
						return;
					}
				}

				if (send(this, sender.getName(), profile.getName(), sender.getName(), message))
					NiftyBukkit.getBungeeHelper().forward(player, server.getName(), Config.CHAT_CHANNEL, "Message", sender.getName(), profile.getName(), message);
			}
		}
	}

}