package net.netcoding.nifty.chat.commands;

import net.netcoding.nifty.chat.cache.UserChatData;
import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.api.plugin.messaging.BungeeServer;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.minecraft.entity.living.human.Player;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.MinecraftServer;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.util.ListUtil;
import net.netcoding.nifty.core.util.RegexUtil;
import net.netcoding.nifty.core.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GList extends MinecraftListener {

	public GList(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "list",
			minimumArgs = 0
	)
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		UserChatData senderData = isConsole(source) ? null : UserChatData.getCache(Nifty.getMojangRepository().searchByPlayer((Player)source));
		List<String> output = new ArrayList<>();
		int totalPlayers = Nifty.getBungeeHelper().getPlayerCount();
		int maxPlayers = Nifty.getBungeeHelper().getMaxPlayers();
		BungeeServer<MinecraftMojangProfile> selected = null;

		if (Nifty.getBungeeHelper().getDetails().isDetected()) {
			if (ListUtil.notEmpty(args)) {
				selected = Nifty.getBungeeHelper().getServer(args[0]);

				if (selected != null) {
					totalPlayers = selected.getPlayerCount();
					maxPlayers = selected.getMaxPlayers();

					if (this.hasPermissions(source, "list", "debug") && args.length > 1 && args[1].matches("^(debug|1|y(es)?)$")) {
						this.getLog().message(source, "-- DEBUG --");
						this.getLog().message(source, "  Online: {{0}}", selected.isOnline() ? "Yes" : "No");
						this.getLog().message(source, "  Count: {{0}}", totalPlayers);
						this.getLog().message(source, "  Max: {{0}}", maxPlayers);
						this.getLog().message(source, "  Motd: {{0}}", selected.getMotd());
						this.getLog().message(source, "  Version: {{0}}", selected.getVersion().getName());
						this.getLog().message(source, "  Protocol: {{0}}", selected.getVersion().getProtocol());
						this.getLog().message(source, "-- DEBUG --");
						this.getLog().message(source, "");
					}
				}
			} else
				selected = Nifty.getBungeeHelper().getServer();
		}

		if (!Nifty.getBungeeHelper().getDetails().isDetected() || alias.matches("^list|online$") || !selected.equals(Nifty.getBungeeHelper().getServer())) {
			List<String> nameList = new ArrayList<>();
			List<MinecraftMojangProfile> profiles = new ArrayList<>();
			String serverName = Nifty.getBungeeHelper().getDetails().isDetected() ? selected.getName() : "*";

			if (!Nifty.getBungeeHelper().getDetails().isDetected())
				profiles.addAll(this.getPlugin().getServer().getPlayerList().stream().map(player -> Nifty.getMojangRepository().searchByPlayer(player)).collect(Collectors.toList()));
			else {
				profiles.addAll(selected.getPlayerList());

				if (!selected.equals(Nifty.getBungeeHelper().getServer()))
					output.add(StringUtil.format("Showing players in {{0}},", selected.getName()));
			}

			if (profiles.size() > 0) {
				for (MinecraftMojangProfile profile : profiles) {
					UserChatData userData = UserChatData.getCache(profile);
					boolean isVanished = userData.getFlagData(Vanish.FLAG, serverName).getValue();
					String displayName = isVanished ? StringUtil.format("{{0}}{1}", "*", userData.getDisplayName()) : userData.getDisplayName();

					if (isVanished && isPlayer(source)) {
						if (userData.equals(senderData) || this.hasPermissions(source, "vanish", "see"))
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
			totalPlayers = Nifty.getBungeeHelper().getPlayerCount("ALL");
			maxPlayers = Nifty.getBungeeHelper().getMaxPlayers("ALL");

			for (BungeeServer<MinecraftMojangProfile> server : Nifty.getBungeeHelper().getServers()) {
				if (server.isOnline()) {
					int serverPlayers = server.getPlayerCount();

					if (serverPlayers > 0) {
						List<String> nameList = new ArrayList<>();

						for (MinecraftMojangProfile profile : server.getPlayerList()) {
							UserChatData userData = UserChatData.getCache(profile);
							boolean isVanished = userData.getFlagData(Vanish.FLAG, server.getName()).getValue();
							String displayName = isVanished ? StringUtil.format("{{0}}{1}", "*", userData.getDisplayName()) : userData.getDisplayName();

							if (isVanished && isPlayer(source)) {
								if (userData.equals(senderData) || this.hasPermissions(source, "vanish", "see"))
									nameList.add(displayName);
								else {
									serverPlayers--;
									totalPlayers--;
								}
							} else
								nameList.add(displayName);
						}

						output.add(StringUtil.format("&8{0}&7 ({{1}})&f:&7 {2}", server.getName(), serverPlayers, StringUtil.implode("&r, ", nameList)));
					}
				}
			}
		}

		output.add("");

		if (totalPlayers > 0)
			output.add(StringUtil.format("{{0}}Players&f: {{1}} &8/ {{2}}", ChatColor.RESET, totalPlayers, maxPlayers));

		this.getLog().message(source, RegexUtil.replaceColor(StringUtil.implode("\n", output), RegexUtil.REPLACE_ALL_PATTERN));
	}

	@Command.TabComplete(index = 0, name = "list")
	protected List<String> onTabComplete(CommandSource source, String alias, String[] args) throws Exception {
		final String arg = args[0].toLowerCase();
		List<String> names = new ArrayList<>();

		if (Nifty.getBungeeHelper().getDetails().isDetected())
			names.addAll(Nifty.getBungeeHelper().getServers().stream().filter(server -> server.getName().toLowerCase().startsWith(arg) || server.getName().toLowerCase().contains(arg)).map(MinecraftServer::getName).collect(Collectors.toList()));

		return names;
	}
}