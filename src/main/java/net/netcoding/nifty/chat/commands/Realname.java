package net.netcoding.nifty.chat.commands;

import net.netcoding.nifty.chat.NiftyChat;
import net.netcoding.nifty.chat.cache.Config;
import net.netcoding.nifty.chat.cache.UserChatData;
import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.api.plugin.messaging.BungeeServer;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.nifty.core.util.ListUtil;
import net.netcoding.nifty.core.util.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Realname extends MinecraftListener {

	public Realname(MinecraftPlugin plugin) {
		super(plugin);
	}

	public static Set<MinecraftMojangProfile> getProfileMatches(String lookup) throws Exception {
		MinecraftMojangProfile profile;
		String profileLookup = "";

		try {
			profile = Nifty.getMojangRepository().searchByUsername(lookup);
			profileLookup = profile.getUniqueId().toString();
		} catch (ProfileNotFoundException ignore) { }

		return NiftyChat.getSQL().query(StringUtil.format("SELECT uuid FROM {0} WHERE LOWER(ufnick) = ? OR LOWER(ufnick) LIKE ? OR uuid = ? ORDER BY CASE WHEN ufnick LIKE ? THEN 1 WHEN ufnick LIKE ? THEN 2 WHEN ufnick LIKE ? THEN 3 END;", Config.USER_TABLE), result -> {
			Set<MinecraftMojangProfile> data = new HashSet<>();

			while (result.next()) {
				try {
					data.add(Nifty.getMojangRepository().searchByUniqueId(UUID.fromString(result.getString("uuid"))));
				} catch (ProfileNotFoundException ignore) { }
			}

			return data;
		}, lookup, ("%" + lookup + "%"), profileLookup, (lookup + "%"), ("%" + lookup + "%"), ("%" + lookup));
	}

	@Command(name = "realname",
			playerTabComplete = true
	)
	public void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		if (args[0].length() < 3) {
			this.getLog().error(source, "At least 3 letters must be queried.");
			return;
		}

		Set<MinecraftMojangProfile> profiles = getProfileMatches(args[0]);

		if (ListUtil.notEmpty(profiles)) {
			for (MinecraftMojangProfile profile : profiles) {
				UserChatData userData = UserChatData.getCache(profile);

				if (!profile.getName().equals(userData.getStrippedDisplayName()))
					this.getLog().message(source, "{{0}} has the nickname {{1}}.", profile.getName(), userData.getStrippedDisplayName());
				else
					this.getLog().message(source, "{{0}} has no nickname.", profile.getName());
			}
		} else
			this.getLog().error(source, "No players matching {{0}} were found!", args[0]);
	}

	@Command.TabComplete(index = 0, name = "realname")
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