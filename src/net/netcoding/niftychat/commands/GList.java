package net.netcoding.niftychat.commands;

import java.util.ArrayList;
import java.util.List;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.BungeeServer;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.util.ListUtil;
import net.netcoding.niftybukkit.util.RegexUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.cache.UserChatData;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class GList extends BukkitCommand {

	public GList(JavaPlugin plugin) {
		super(plugin, "list");
		this.setMinimumArgsLength(0);
		this.editUsage(0, "glist", "[server]");
		this.editUsage(0, "gonline", "[server]");
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		UserChatData senderData = isConsole(sender) ? null : UserChatData.getCache(NiftyBukkit.getMojangRepository().searchByExactPlayer((Player)sender).getUniqueId());
		List<String> output = new ArrayList<>();
		int totalPlayers = this.getPlugin().getServer().getOnlinePlayers().length;
		int maxPlayers = this.getPlugin().getServer().getMaxPlayers();
		BungeeServer selected = null;

		if (NiftyBukkit.getBungeeHelper().isOnline()) {
			if (ListUtil.notEmpty(args)) {
				selected = NiftyBukkit.getBungeeHelper().getServer(args[0]);

				if (selected != null) {
					totalPlayers = selected.getPlayerCount();
					maxPlayers = selected.getMaxPlayers();
				}
			}
		}

		if (alias.matches("^list|online$") || selected != null || !NiftyBukkit.getBungeeHelper().isOnline()) {
			List<String> nameList = new ArrayList<>();
			List<MojangProfile> profiles = new ArrayList<>();

			if (!NiftyBukkit.getBungeeHelper().isOnline()) {
				for (Player player : this.getPlugin().getServer().getOnlinePlayers())
					profiles.add(NiftyBukkit.getMojangRepository().searchByExactPlayer(player));
			} else {
				if (selected != null) {
					profiles.addAll(selected.getPlayerList());
					output.add(StringUtil.format("Showing players in {{0}},", selected.getName()));
				} else {
					profiles.addAll(NiftyBukkit.getBungeeHelper().getPlayerList());
					output.add(StringUtil.format("Showing players in {{0}},", NiftyBukkit.getBungeeHelper().getServerName()));
				}
			}

			if (profiles.size() > 0) {
				for (MojangProfile profile : profiles) {
					UserChatData userData = UserChatData.getCache(profile.getUniqueId());
					userData = userData == null ? new UserChatData(this.getPlugin(), profile) : userData;

					if (userData.getFlagData("vanished").getValue()) {
						if ((isPlayer(sender) && userData.getUniqueId().equals(senderData.getUniqueId())) || this.hasPermissions(sender, "vanish", "see"))
							nameList.add(StringUtil.format("{{0}}{1}", "*", userData.getDisplayName()));
						else
							totalPlayers--;
					} else
						nameList.add(userData.getDisplayName());
				}
			} else
				output.add("&oNo players online");

			if (nameList.size() > 0) output.add(StringUtil.implode("&r, ", nameList));
		} else {
			List<BungeeServer> servers = new ArrayList<>(NiftyBukkit.getBungeeHelper().getServers());
			totalPlayers = NiftyBukkit.getBungeeHelper().getPlayerCount();
			maxPlayers = NiftyBukkit.getBungeeHelper().getMaxPlayers();

			for (BungeeServer server : servers) {
				if (server.isOnline()) {
					String names = "";
					int serverPlayers = server.getPlayerCount();

					if (server.getPlayerCount() > 0) {
						List<String> nameList = new ArrayList<>();

						for (MojangProfile profile : server.getPlayerList()) {
							UserChatData userData = UserChatData.getCache(profile.getUniqueId());
							userData = userData == null ? new UserChatData(this.getPlugin(), profile) : userData;

							if (userData.getFlagData("vanished").getValue()) {
								if ((isPlayer(sender) && userData.getUniqueId().equals(senderData.getUniqueId())) || this.hasPermissions(sender, "vanish", "see"))
									nameList.add(StringUtil.format("{{0}}{1}", "*", userData.getDisplayName()));
								else {
									serverPlayers--;
									totalPlayers--;
								}
							} else
								nameList.add(userData.getDisplayName());
						}

						names += StringUtil.implode("&r, ", nameList);
					}

					output.add(StringUtil.format("&8{0} &7({{1}})&f: &7{2}", server.getName(), serverPlayers, names));
				}
			}
		}

		output.add("");
		output.add(StringUtil.format("Players&f: {{0}} &8/ {{1}}", totalPlayers, maxPlayers));
		this.getLog().message(sender, RegexUtil.replaceColor(StringUtil.implode("\n", output), RegexUtil.REPLACE_ALL_PATTERN));
	}

}