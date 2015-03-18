package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.factory.ResultCallback;
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
		this.setPlayerTabComplete();
		this.setCheckHelp(false);
		this.editUsage(0, "reply", "[message]");
		this.editUsage(0, "r", "[message]");
	}

	protected static void notifySpies(final BukkitHelper helper, final UserChatData senderData, final UserChatData receiverData, final String message) {
		helper.getPlugin().getServer().getScheduler().runTaskAsynchronously(helper.getPlugin(), new Runnable() {
			@Override
			public void run() {
				try {
					RankFormat format = RankFormat.getCache("message");
					List<MojangProfile> spies = Cache.MySQL.query(StringUtil.format("SELECT `uuid` FROM `{0}` WHERE `flag` = ? AND `value` = ?;", Config.USER_FLAGS_TABLE), new ResultCallback<List<MojangProfile>>() {
						@Override
						public List<MojangProfile> handle(ResultSet result) throws SQLException {
							List<MojangProfile> profiles = new ArrayList<>();
							while (result.next()) {
								try {
									profiles.add(NiftyBukkit.getMojangRepository().searchByUniqueId(UUID.fromString(result.getString("uuid")), false));
								} catch (ProfileNotFoundException pnfex) { }
							}
							return profiles;
						}
					}, "spying", true);

					for (MojangProfile spy : spies) {
						if (!spy.getUniqueId().equals(senderData.getProfile().getUniqueId()) && !spy.getUniqueId().equals(receiverData.getProfile().getUniqueId())) {
							if (!NiftyBukkit.getBungeeHelper().isDetected()) {
								UserChatData spyData = UserChatData.getCache(spy);
								if (spyData.getOfflinePlayer().isOnline()) helper.getLog().message(spyData.getOfflinePlayer().getPlayer(), format.getFormat(), senderData.getDisplayName(), receiverData.getDisplayName(), format);
							} else {
								if (spy.isOnlineAnywhere())
									NiftyBukkit.getBungeeHelper().forward(receiverData.getProfile(), NiftyBukkit.getBungeeHelper().getPlayerServer(spy).getName(), Config.CHAT_CHANNEL, "SpyMessage", senderData.getProfile().getName(), receiverData.getProfile().getName(), spy.getName(), message);
							}	
						}
					}
				} catch (Exception ex) {
					helper.getLog().console(ex);
				}
			}
		});
	}

	public static boolean send(final BukkitHelper helper, String senderName, String receiverName, String recipientName, String message) {
		UserChatData senderData = UserChatData.getCache(NiftyBukkit.getMojangRepository().searchByUsername(senderName)); // Sender
		UserChatData receiverData = UserChatData.getCache(NiftyBukkit.getMojangRepository().searchByUsername(receiverName)); // Receiver
		UserChatData recipientData = UserChatData.getCache(NiftyBukkit.getMojangRepository().searchByUsername(recipientName)); // Sent

		if (recipientData.getProfile().equals(senderData.getProfile())) { // Sending
			boolean receiverOnline = NiftyBukkit.getBungeeHelper().isDetected() ? receiverData.getProfile().isOnlineAnywhere() : receiverData.isOnline();

			if (!receiverOnline) {
				helper.getLog().error(senderData.getOfflinePlayer().getPlayer(), "Unable to locate {{0}}!", receiverData.getProfile().getName());
				return false;
			}

			if (senderData.getFlagData("muted").getValue() && !helper.hasPermissions(senderData.getOfflinePlayer().getPlayer(), "mute", "roar")) {
				Mute.sendMutedError(helper.getLog(), senderData.getOfflinePlayer().getPlayer(), senderData);
				return false;
			}

			if (receiverData.getFlagData("vanished").getValue() && !senderData.hasPermissions("vanish", "interact")) {
				helper.getLog().error(senderData.getOfflinePlayer().getPlayer(), "Unable to locate {{0}}!", receiverData.getProfile().getName());
				return false;
			}
		} else if (recipientData.getProfile().equals(receiverData.getProfile())) { // Receiving
			receiverData.setLastMessenger(senderData.getProfile());
			notifySpies(helper, senderData, receiverData, message);
		} else { // Spying
			if ((senderData.getFlagData("vanished").getValue() || receiverData.getFlagData("vanished").getValue()) && !helper.hasPermissions(recipientData.getOfflinePlayer().getPlayer(), "vanish", "spy"))
				return false;
		}

		RankFormat format = RankFormat.getCache("message");
		String senderDisplayName = recipientData.getProfile().equals(senderData.getProfile()) ? RegexUtil.replaceColor("&7me", RegexUtil.REPLACE_ALL_PATTERN) : senderData.getDisplayName();
		String receiverDisplayName = recipientData.getProfile().equals(receiverData.getProfile()) ? RegexUtil.replaceColor("&7me", RegexUtil.REPLACE_ALL_PATTERN) : receiverData.getDisplayName();
		helper.getLog().message(recipientData.getOfflinePlayer().getPlayer(), format.getFormat(), senderDisplayName, receiverDisplayName, message);
		if (recipientData.getProfile().equals(senderData.getProfile())) helper.getLog().console(format.getFormat(), senderData.getDisplayName(), receiverData.getDisplayName(), message);
		return true;
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		Player player = (Player)sender;
		MojangProfile profile;
		boolean reply = alias.matches("^r(?:eply)?$");

		if (args.length < (reply ? 1 : 2)) {
			this.showUsage(sender);
			return;
		}

		String playerName = args[0];
		String message = StringUtil.implode(" ", args, reply ? 0 : 1);
		MojangProfile senderprofile = NiftyBukkit.getMojangRepository().searchByPlayer(player);
		UserChatData senderData = UserChatData.getCache(senderprofile);

		if (reply) {
			MojangProfile lastMessenger = senderData.getLastMessenger();

			if (lastMessenger != null) {
				playerName = lastMessenger.getName();
			} else {
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

		try {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName);
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(sender, "Unable to locate the profile of {{0}}!", playerName);
			return;
		}

		if (sender.getName().equals(profile.getName())) {
			this.getLog().error(sender, "You cannot message yourself!");
			return;
		}

		if (!NiftyBukkit.getBungeeHelper().isDetected()) {
			if (send(this, sender.getName(), profile.getName(), sender.getName(), message))
				send(this, sender.getName(), profile.getName(), profile.getName(), message);
		} else {
			if (profile.isOnlineAnywhere()) {
				BungeeServer server = NiftyBukkit.getBungeeHelper().getPlayerServer(profile);

				if (!server.equals(NiftyBukkit.getBungeeHelper().getServer())) {
					if (!this.hasPermissions(sender, "message", "global")) {
						this.getLog().error(sender, "You cannot send messages across servers!");
						return;
					}
				}

				if (send(this, sender.getName(), profile.getName(), sender.getName(), message))
					NiftyBukkit.getBungeeHelper().forward(senderprofile, server.getName(), Config.CHAT_CHANNEL, "Message", sender.getName(), profile.getName(), message);
			}
		}
	}

}