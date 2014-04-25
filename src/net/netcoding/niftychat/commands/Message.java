package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.minecraft.BungeeServer;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.listeners.Chat;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Message extends BukkitCommand {

	public Message(JavaPlugin plugin) {
		super(plugin, "message");
		this.setPlayerOnly();
	}

	public static boolean send(BukkitHelper helper, String senderName, String receiverName, String message, boolean checks) {
		MojangProfile senderProfile = NiftyBukkit.getMojangRepository().searchByExactUsername(senderName);
		UserChatData senderData = UserChatData.getCache(senderProfile.getUniqueId());
		senderData = senderData == null ? new UserChatData(helper.getPlugin(), senderProfile) : senderData;

		MojangProfile receiverProfile = NiftyBukkit.getMojangRepository().searchByExactUsername(receiverName);
		UserChatData receiverData = UserChatData.getCache(receiverProfile.getUniqueId());
		receiverData = receiverData == null ? new UserChatData(helper.getPlugin(), receiverProfile) : receiverData;

		if (checks) {
			if (senderData.isMuted()) {
				Mute.sendMutedError(helper.getLog(), senderData.getPlayer(), senderData);
				return false;
			}

			if (senderData.getName().equalsIgnoreCase(receiverData.getName())) {
				//helper.getLog().error(senderData.getPlayer(), "You cannot message yourself!");
				//return false;
			}
		}

		if (receiverData.getPlayer() != null) {
			helper.getLog().message(senderData.getPlayer(), "{{0}}{{1}} {2} {{3}}{{4}} {5}", "[", senderData.getDisplayName(), (ChatColor.DARK_GRAY + ">" + ChatColor.RESET), receiverData.getDisplayName(), "]", (ChatColor.DARK_GREEN + message));
			if (receiverData.getPlayer() != null) receiverData.setLastMessenger(senderProfile);
			return true;
		}

		return false;
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		if (args.length >= 1) {
			Player player = (Player)sender;
			String playerName = args[0];
			MojangProfile profile;
			String message = StringUtil.implode(" ", args, 1);
			boolean sent = false;
			MojangProfile senderprofile = NiftyBukkit.getMojangRepository().searchByExactPlayer(player);
			UserChatData senderData = UserChatData.getCache(senderprofile.getUniqueId());

			if (alias.matches("^reply|r")) {
				MojangProfile lastMessenger = senderData.getLastMessenger();

				if (lastMessenger != null)
					playerName = lastMessenger.getName();
				else {
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
				message = Chat.filter(this, player, message);
				message = Chat.format(this, player, message);
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

			if (send(this, sender.getName(), profile.getName(), message, true)) {
				if (!Cache.chatHelper.isOnline()) {
					UserChatData receiverData = UserChatData.getCache(profile.getUniqueId());

					if (receiverData != null) {
						this.getLog().message(receiverData.getPlayer(), message);
						sent = true;
					}
				} else {
					try {
						if (Cache.chatHelper.isPlayerOnline(profile)) {
							BungeeServer server = Cache.chatHelper.getPlayerServer(profile);
							Cache.chatHelper.forward(player, server.getName(), Config.CHAT_CHANNEL, "Message", sender.getName(), profile.getName(), message);
							sent = true;
						}
					} catch (Exception ex) {
						this.getLog().error(sender, ex.getMessage());
						return;
					}
				}

			} else
				sent = true;

			if (!sent) this.getLog().error(sender, "Unable to locate {{0}}!", profile.getName());
		} else
			this.showUsage(sender);
	}

}