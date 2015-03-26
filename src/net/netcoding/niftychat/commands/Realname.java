package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.factory.callbacks.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.BungeeServer;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftybukkit.util.ListUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.UserChatData;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Realname extends BukkitCommand {

	public Realname(JavaPlugin plugin) {
		super(plugin, "realname");
		this.setPlayerTabComplete();
	}

	public static HashSet<MojangProfile> getProfileMatches(String lookup) throws Exception {
		MojangProfile profile;
		String profileLookup = "";

		try {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(lookup);
			profileLookup = profile.getUniqueId().toString();
		} catch (ProfileNotFoundException pnfe) { }

		final HashSet<MojangProfile> profiles = NiftyChat.getSQL().query(StringUtil.format("SELECT uuid FROM {0} WHERE LOWER(ufnick) = ? OR LOWER(ufnick) LIKE ? OR uuid = ? ORDER BY ufnick;", Config.USER_TABLE), new ResultCallback<HashSet<MojangProfile>>() {
			@Override
			public HashSet<MojangProfile> handle(ResultSet result) throws SQLException {
				HashSet<MojangProfile> data = new HashSet<>();

				while (result.next()) {
					MojangProfile profile;

					try {
						profile = NiftyBukkit.getMojangRepository().searchByUniqueId(UUID.fromString(result.getString("uuid")));
						data.add(profile);
					} catch (ProfileNotFoundException pnfe) { }
				}

				return data;
			}
		}, lookup, ("%" + lookup + "%"), profileLookup);

		return profiles;
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		if (args[0].length() < 3) {
			this.getLog().error(sender, "At least 3 letters must be queried.");
			return;
		}

		HashSet<MojangProfile> profiles = getProfileMatches(args[0]);

		if (ListUtil.notEmpty(profiles)) {
			for (MojangProfile profile : profiles) {
				UserChatData userData = UserChatData.getCache(profile);

				if (!profile.getName().equals(userData.getStrippedDisplayName()))
					this.getLog().message(sender, "{{0}} has the nickname {{1}}.", profile.getName(), userData.getStrippedDisplayName());
				else
					this.getLog().message(sender, "{{0}} has no nickname.", profile.getName());
			}
		} else
			this.getLog().error(sender, "No players matching {{0}} were found!", args[0]);
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, String alias, String[] args) throws Exception {
		final String arg = args[0].toLowerCase();
		List<String> names = new ArrayList<>();

		if (NiftyBukkit.getBungeeHelper().isDetected()) {
			for (BungeeServer server : NiftyBukkit.getBungeeHelper().getServers()) {
				for (MojangProfile profile : server.getPlayerList()) {
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