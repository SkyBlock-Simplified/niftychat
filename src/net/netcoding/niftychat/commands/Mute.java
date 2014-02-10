package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftychat.NiftyChat;

import org.bukkit.command.CommandSender;

public class Mute extends BukkitCommand {

	public Mute(NiftyChat plugin) {
		super(plugin, "mute");
	}

	@Override
	public void command(CommandSender sender, String[] args) {
		if (super.hasPermissions(sender, "chat", "mute")) {
			if (args.length >= 1 && args.length <= 2) {
				// TODO
			} else
				super.showUsage(sender);
		}
	}

}