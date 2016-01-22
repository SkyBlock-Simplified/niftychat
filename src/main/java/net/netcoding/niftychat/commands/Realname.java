package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.messages.BungeeServer;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftycore.database.factory.callbacks.ResultCallback;
import net.netcoding.niftycore.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftycore.util.ListUtil;
import net.netcoding.niftycore.util.StringUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Realname extends BukkitCommand {

	public Realname(JavaPlugin plugin) {
		super(plugin, "realname");
		this.setPlayerTabComplete();
	}

	public static HashSet<BukkitMojangProfile> getProfileMatches(String lookup) throws Exception {
		BukkitMojangProfile profile;
		String profileLookup = "";

		try {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(lookup);
			profileLookup = profile.getUniqueId().toString();
		} catch (ProfileNotFoundException pnfe) { }

		final HashSet<BukkitMojangProfile> profiles = NiftyChat.getSQL().query(StringUtil.format("SELECT uuid FROM {0} WHERE LOWER(ufnick) = ? OR LOWER(ufnick) LIKE ? OR uuid = ? ORDER BY ufnick;", Config.USER_TABLE), new ResultCallback<HashSet<BukkitMojangProfile>>() {
			@Override
			public HashSet<BukkitMojangProfile> handle(ResultSet result) throws SQLException {
				HashSet<BukkitMojangProfile> data = new HashSet<>();

				while (result.next()) {
					try {
						data.add(NiftyBukkit.getMojangRepository().searchByUniqueId(UUID.fromString(result.getString("uuid"))));
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

		HashSet<BukkitMojangProfile> profiles = getProfileMatches(args[0]);

		if (ListUtil.notEmpty(profiles)) {
			for (BukkitMojangProfile profile : profiles) {
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