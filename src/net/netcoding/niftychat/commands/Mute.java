package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftychat.NiftyChat;

import org.bukkit.command.CommandSender;

public class Mute extends BukkitCommand {

	public Mute(NiftyChat plugin) {
		super(plugin, "mute");
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) {
		if (args.length >= 1 && args.length <= 2) {
			// TODO
		} else
			this.showUsage(sender);
	}

}