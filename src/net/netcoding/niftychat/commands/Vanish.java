package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Vanish extends BukkitCommand {

	public Vanish(JavaPlugin plugin) {
		super(plugin, "vanish");
		this.setMinimumArgsLength(0);
		this.setMaximumArgsLength(2);
	}

	private boolean getToggleArg(String toggle) {
		return toggle.matches("^on|yes|enable$");
	}

	@SuppressWarnings("unused")
	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		if (isConsole(sender) && args.length != 2) {
			this.getLog().error(sender, "You must provide a player name when vanishing a player from console!");
			return;
		}

		String playerName = sender.getName();
		boolean toggleValue = true;
		boolean setValue = false;
		MojangProfile profile;

		if (args.length == 2) {
			playerName = args[0];
			toggleValue = false;
			setValue = getToggleArg(args[1]);
		} else if (args.length == 1) {
			if (args[0].matches("^o(?:n|ff)$")) {
				toggleValue = false;
				setValue = getToggleArg(args[0]);
			} else
				playerName = args[0];
		}

		try {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName)[0];
		} catch (ProfileNotFoundException pnfe) {
			this.getLog().error(sender, "Unable to locate the uuid of {{0}}!", playerName);
			return;
		}

		
	}

}