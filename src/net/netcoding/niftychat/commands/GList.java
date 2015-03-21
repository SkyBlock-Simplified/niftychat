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

import org.bukkit.ChatColor;
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

	@SuppressWarnings("deprecation")
	@Override
	protected void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		UserChatData senderData = isConsole(sender) ? null : UserChatData.getCache(NiftyBukkit.getMojangRepository().searchByPlayer((Player)sender));
		List<String> output = new ArrayList<>();
		int totalPlayers = NiftyBukkit.getBungeeHelper().getPlayerCount();
		int maxPlayers = NiftyBukkit.getBungeeHelper().getMaxPlayers();
		BungeeServer selected = null;

		if (NiftyBukkit.getBungeeHelper().isDetected()) {
			if (ListUtil.notEmpty(args)) {
				selected = NiftyBukkit.getBungeeHelper().getServer(args[0]);

				if (selected != null) {
					totalPlayers = selected.getPlayerCount();
					maxPlayers = selected.getMaxPlayers();
				}
			} else
				selected = NiftyBukkit.getBungeeHelper().getServer();
		}

		if (!NiftyBukkit.getBungeeHelper().isDetected() || alias.matches("^list|online$") || !selected.equals(NiftyBukkit.getBungeeHelper().getServer())) {
			List<String> nameList = new ArrayList<>();
			List<MojangProfile> profiles = new ArrayList<>();
			String serverName = NiftyBukkit.getBungeeHelper().isDetected() ? selected.getName() : "*";

			if (!NiftyBukkit.getBungeeHelper().isDetected()) {
				for (Player player : this.getPlugin().getServer().getOnlinePlayers())
					profiles.add(NiftyBukkit.getMojangRepository().searchByPlayer(player));
			} else {
				profiles.addAll(selected.getPlayerList());

				if (!selected.equals(NiftyBukkit.getBungeeHelper().getServer()))
					output.add(StringUtil.format("Showing players in {{0}},", selected.getName()));
			}

			if (profiles.size() > 0) {
				for (MojangProfile profile : profiles) {
					UserChatData userData = UserChatData.getCache(profile);
					boolean isVanished = userData.getFlagData("vanished", serverName).getValue();
					String displayName = isVanished ? StringUtil.format("{{0}}{1}", "*", userData.getDisplayName()) : userData.getDisplayName();

					if (isVanished && isPlayer(sender)) {
						if (senderData.equals(userData) || this.hasPermissions(sender, "vanish", "see"))
							nameList.add(displayName);
						else
							totalPlayers--;
					} else
						nameList.add(displayName);
				}
			} else
				output.add("&oNo players online");

			if (nameList.size() > 0) output.add(StringUtil.implode("&r, ", nameList));
		} else {
			totalPlayers = NiftyBukkit.getBungeeHelper().getPlayerCount("ALL");
			maxPlayers = NiftyBukkit.getBungeeHelper().getMaxPlayers("ALL");

			for (BungeeServer server : NiftyBukkit.getBungeeHelper().getServers()) {
				if (server.isOnline()) {
					String names = "";
					int serverPlayers = server.getPlayerCount();

					if (server.getPlayerCount() > 0) {
						List<String> nameList = new ArrayList<>();

						for (MojangProfile profile : server.getPlayerList()) {
							UserChatData userData = UserChatData.getCache(profile);
							boolean isVanished = userData.getFlagData("vanished", server.getName()).getValue();
							String displayName = isVanished ? StringUtil.format("{{0}}{1}", "*", userData.getDisplayName()) : userData.getDisplayName();

							if (isVanished && isPlayer(sender)) {
								if (senderData.equals(userData) || this.hasPermissions(sender, "vanish", "see"))
									nameList.add(displayName);
								else {
									serverPlayers--;
									totalPlayers--;
								}
							} else
								nameList.add(displayName);
						}

						names += StringUtil.implode("&r, ", nameList);
						output.add(StringUtil.format("&8{0} &7({{1}})&f: &7{2}", server.getName(), serverPlayers, names));
					}
				}
			}
		}

		output.add("");
		if (totalPlayers > 0) output.add(StringUtil.format("{{0}}Players&f: {{1}} &8/ {{2}}", ChatColor.RESET, totalPlayers, maxPlayers));
		this.getLog().message(sender, RegexUtil.replaceColor(StringUtil.implode("\n", output), RegexUtil.REPLACE_ALL_PATTERN));
	}

}