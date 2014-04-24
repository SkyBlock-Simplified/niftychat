package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.BungeeHelper;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftybukkit.util.TimeUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.Config;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Mute extends BukkitCommand {

	public static final transient SimpleDateFormat EXPIRE_FORMAT = new SimpleDateFormat("MMM dd, yyyy h:mm a z");

	public Mute(NiftyChat plugin) {
		super(plugin, "mute");
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws SQLException {
		if (args.length >= 1 && args.length <= 3) {
			String playerName = args[0];
			MojangProfile profile;

			if (isConsole(playerName)) {
				this.getLog().error(sender, "You cannot mute the console!");
				return;
			}

			try {
				profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName)[0];
			} catch (ProfileNotFoundException pnfe) {
				this.getLog().error(sender, "Unable to locate the uuid of {{0}}!", playerName);
				return;
			}

			if (sender.getName().equalsIgnoreCase(profile.getName())) {
				this.getLog().error(sender, "You cannot mute yourself!");
				return;
			}

			long expires = args.length >= 2 ? TimeUtil.getDateTime(args[1]) : 0;
			String server = "*";
			BungeeHelper bungeeHelper = NiftyBukkit.getBungeeHelper();

			if (bungeeHelper.isOnline()) {
				server = bungeeHelper.getServerName();

				if ((args.length == 2 && expires == 0) || args.length == 3) {
					if (bungeeHelper.getServer(args[args.length - 1]) != null)
						server = args[args.length - 1];
				}
			}

			boolean globalMute = alias.matches("^globalmute|globalunmute|gmute|gunmute$") || server.matches("^global|all|\\*$");

			if (globalMute) {
				server = "*";
				Cache.MySQL.update(StringUtil.format("DELETE FROM `{0}` WHERE `uuid` = ? AND `flag` = ? AND `server` <> ?;", Config.USER_FLAGS_TABLE), profile.getUniqueId(), "muted", "*");
			}

			boolean isMuted = Cache.MySQL.query(StringUtil.format("SELECT `value` FROM `{0}` WHERE `uuid` = ? AND `flag` = ? AND `server` = ?;", Config.USER_FLAGS_TABLE), new ResultCallback<Boolean>() {
				@Override
				public Boolean handle(ResultSet result) throws SQLException {
					if (result.next())
						return result.getBoolean("value");
					else
						return false;
				}
			}, profile.getUniqueId(), "muted", server);

			if (alias.matches("^unmute|globalunmute|gunmute$")) isMuted = true;
			if (isMuted) expires = 0;
			if (expires != 0) expires += System.currentTimeMillis();
			Cache.MySQL.update(StringUtil.format("INSERT INTO `{0}` (`uuid`, `flag`, `value`, `server`, `_expires`) VALUES (?, ?, ?, ?, FROM_UNIXTIME(?)) ON DUPLICATE KEY UPDATE `value` = ?, `_expires` = FROM_UNIXTIME(?);", Config.USER_FLAGS_TABLE), profile.getUniqueId(), "muted", !isMuted, server, (expires == 0 ? null : expires), !isMuted, (expires == 0 ? null : expires));
			String serverMsg = "";
			if (bungeeHelper.isOnline()) serverMsg = StringUtil.format("Server: {{0}}.", (server.equals("*") ? "all servers" : server));
			String expireMsg = (!isMuted && expires != 0) ? StringUtil.format(" until {{0}}", EXPIRE_FORMAT.format(new Date(expires))) : "";
			this.getLog().message(sender, "{{0}} {1}muted{2}.", profile.getName(), (!isMuted ? "" : "un"), expireMsg);
			this.getLog().message(sender, serverMsg);
			String receivMsg = "You have been {0}muted{1}.";

			if (!bungeeHelper.isOnline()) {
				this.getLog().message(findPlayer(profile.getName()), receivMsg, (!isMuted ? "" : "un"), expireMsg);
				this.getLog().message(findPlayer(profile.getName()), serverMsg);
			} else {
				if (isConsole(sender) && bungeeHelper.getServer().getPlayerCount() == 0) return;
				bungeeHelper.message(profile.getName(), ChatColor.GRAY + StringUtil.format(receivMsg, (!isMuted ? "" : "un"), expireMsg));
				bungeeHelper.message(profile.getName(), ChatColor.GRAY + serverMsg);
			}
		} else
			this.showUsage(sender);
	}

}