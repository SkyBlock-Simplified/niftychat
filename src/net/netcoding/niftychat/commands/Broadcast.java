package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.messages.BungeeServer;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftycore.util.StringUtil;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Broadcast extends BukkitCommand {

	public Broadcast(JavaPlugin plugin) {
		super(plugin, "broadcast");
	}

	@Override
	protected void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		String name = StringUtil.format("{0}[{{1}}{0}]", ChatColor.DARK_GRAY, "Broadcast");
		String message = StringUtil.format("{0} {{1}}", name, StringUtil.implode(" ", args));
		boolean network = alias.matches("^n(etwork)?b(r(oadcast)?)?$") && NiftyBukkit.getBungeeHelper().isDetected();

		if (network && !this.hasPermissions(sender, "broadcast", "network")) {
			this.getLog().error(sender, "You cannot send a network broadcast!");
			return;
		}

		if (network) {
			for (BungeeServer server : NiftyBukkit.getBungeeHelper().getServers()) {
				for (BukkitMojangProfile profile : server.getPlayerList())
					NiftyBukkit.getBungeeHelper().message(profile, message);
			}
		} else {
			this.getLog().message(this.getPlugin().getServer().getConsoleSender(), message);

			for (BukkitMojangProfile profile : NiftyBukkit.getBungeeHelper().getPlayerList())
				this.getLog().message(profile.getOfflinePlayer().getPlayer(), message);
		}
	}

}