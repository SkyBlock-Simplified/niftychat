package net.netcoding.niftychat.commands;

import java.sql.SQLException;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftychat.cache.UserChatData;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Vanish extends BukkitCommand {

	public Vanish(JavaPlugin plugin) {
		super(plugin, "vanish");
	}

	private boolean getToggleArg(String toggle) {
		return toggle.matches("^on|yes|enable$");
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) throws SQLException, Exception {
		if (isConsole(sender) && args.length < 2)
			this.getLog().error(sender, "The vanish command requires a player name when used by the console!");
		else if (args.length >= 0 && args.length <= 2) {
			String playerName   = sender.getName();
			boolean toggleValue = true;
			boolean setValue    = false;

			if (args.length == 2) {
				playerName  = args[0];
				toggleValue = false;
				setValue    = getToggleArg(args[1]);
			} else if (args.length == 1) {
				if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off")) {
					toggleValue = false;
					setValue    = getToggleArg(args[0]);
				} else
					playerName = args[0];
			}

			if (playerName.equalsIgnoreCase("CONSOLE")) {
				this.getLog().error(sender, "You cannot modify the visibility of the console!");
				return;
			}

			try {
				MojangProfile profile = NiftyBukkit.getMojangRepository().searchByUsername(playerName)[0];
				UserChatData userData = UserChatData.getCache(profile.getUniqueId());

				if (userData != null) {
					userData.setVanished(toggleValue ? !userData.isVanished() : setValue);
					//Player player = userData.getPlayer();
					//Cache.ghosts.setGhost(player, userData.isVanished());

					if (!userData.isVanished()) {
						//if (!userData.hasPermissions("vanish", "see"))
						//	Cache.ghosts.remove(player);
					}

					this.getLog().message(sender, "{{0}} {1} vanished", (profile.getName().equalsIgnoreCase(sender.getName()) ? "You are" : profile.getName() + " is"), (userData.isVanished() ? "now" : "no longer"));
				}
			} catch (ProfileNotFoundException pnfe) {
				
			}
		} else
			this.showUsage(sender);
	}

}