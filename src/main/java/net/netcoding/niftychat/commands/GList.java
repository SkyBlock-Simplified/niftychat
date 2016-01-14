package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.messages.BungeeServer;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftycore.util.ListUtil;
import net.netcoding.niftycore.util.RegexUtil;
import net.netcoding.niftycore.util.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class GList extends BukkitCommand {

	public GList(JavaPlugin plugin) {
		super(plugin, "list");
		this.setMinimumArgsLength(0);
	}

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

					if (this.hasPermissions(sender, "list", "debug") && args.length > 1 && args[1].matches("^(debug|1|y(es)?)$")) {
						this.getLog().message(sender, "-- DEBUG --");
						this.getLog().message(sender, "  Online: {{0}}", selected.isOnline() ? "Yes" : "No");
						this.getLog().message(sender, "  Count: {{0}}", totalPlayers);
						this.getLog().message(sender, "  Max: {{0}}", maxPlayers);
						this.getLog().message(sender, "  Motd: {{0}}", selected.getMotd());
						this.getLog().message(sender, "  Version: {{0}}", selected.getVersion().getName());
						this.getLog().message(sender, "  Protocol: {{0}}", selected.getVersion().getProtocol());
						this.getLog().message(sender, "-- DEBUG --");
						this.getLog().message(sender, "");
					}
				}
			} else
				selected = NiftyBukkit.getBungeeHelper().getServer();
		}

		if (!NiftyBukkit.getBungeeHelper().isDetected() || alias.matches("^list|online$") || !selected.equals(NiftyBukkit.getBungeeHelper().getServer())) {
			List<String> nameList = new ArrayList<>();
			List<BukkitMojangProfile> profiles = new ArrayList<>();
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
				for (BukkitMojangProfile profile : profiles) {
					UserChatData userData = UserChatData.getCache(profile);
					boolean isVanished = userData.getFlagData(Vanish.FLAG, serverName).getValue();
					String displayName = isVanished ? StringUtil.format("{{0}}{1}", "*", userData.getDisplayName()) : userData.getDisplayName();

					if (isVanished && isPlayer(sender)) {
						if (userData.equals(senderData) || this.hasPermissions(sender, "vanish", "see"))
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

						for (BukkitMojangProfile profile : server.getPlayerList()) {
							UserChatData userData = UserChatData.getCache(profile);
							boolean isVanished = userData.getFlagData(Vanish.FLAG, server.getName()).getValue();
							String displayName = isVanished ? StringUtil.format("{{0}}{1}", "*", userData.getDisplayName()) : userData.getDisplayName();

							if (isVanished && isPlayer(sender)) {
								if (userData.equals(senderData) || this.hasPermissions(sender, "vanish", "see"))
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

	@Override
	protected List<String> onTabComplete(CommandSender sender, String label, String[] args) throws Exception {
		final String arg = args[0].toLowerCase();
		List<String> names = new ArrayList<>();

		if (NiftyBukkit.getBungeeHelper().isDetected()) {
			for (BungeeServer server : NiftyBukkit.getBungeeHelper().getServers()) {
				if (server.getName().toLowerCase().startsWith(arg) || server.getName().toLowerCase().contains(arg))
					names.add(server.getName());
			}
		}

		return names;
	}
}