package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftychat.listeners.Chat;
import net.netcoding.niftycore.mojang.MojangProfile;
import net.netcoding.niftycore.util.StringUtil;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Me extends BukkitCommand {

	public Me(JavaPlugin plugin) {
		super(plugin, "me");
		this.setCheckHelp(false);
	}

	@Override
	protected void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		String name = StringUtil.format("{0}[{{1}}{0}]", ChatColor.DARK_GRAY, "Console");
		String message = StringUtil.implode(" ", args);

		if (isPlayer(sender)) {
			Player player = (Player)sender;
			name = player.getDisplayName();

			if (Chat.check(this, player, message)) {
				message = Chat.filter(this, player, "me", message);
				message = Chat.format(this, player, "me", message);
			} else
				return;
		}

		message = StringUtil.format("{0} {1}{2}", name, ChatColor.GRAY, message);
		this.getLog().message(this.getPlugin().getServer().getConsoleSender(), message);

		for (MojangProfile profile : NiftyBukkit.getBungeeHelper().getPlayerList())
			this.getLog().message(((BukkitMojangProfile)profile).getOfflinePlayer().getPlayer(), message);
	}

}