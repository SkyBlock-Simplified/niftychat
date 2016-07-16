package net.netcoding.nifty.chat.commands;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.api.plugin.messaging.BungeeServer;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.util.StringUtil;

public class Broadcast extends MinecraftListener {

	public Broadcast(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "broadcast")
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		String name = StringUtil.format("{0}[{{1}}{0}]", ChatColor.DARK_GRAY, "Broadcast");
		String message = StringUtil.format("{0} {{1}}", name, StringUtil.implode(" ", args));
		boolean network = alias.matches("^n(etwork)?b(r(oadcast)?)?$") && Nifty.getBungeeHelper().getDetails().isDetected();

		if (network && !this.hasPermissions(source, "broadcast", "network")) {
			this.getLog().error(source, "You cannot send a network broadcast!");
			return;
		}

		if (network) {
			for (BungeeServer<MinecraftMojangProfile> server : Nifty.getBungeeHelper().getServers()) {
				for (MinecraftMojangProfile profile : server.getPlayerList())
					Nifty.getBungeeHelper().message(profile, message);
			}
		} else {
			this.getLog().message(this.getPlugin().getServer().getConsoleSource(), message);
			Nifty.getBungeeHelper().getPlayerList().stream().forEach(profile -> this.getLog().message(profile.getOfflinePlayer().getPlayer(), message));
		}
	}

}