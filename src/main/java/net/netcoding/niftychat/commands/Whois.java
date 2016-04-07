package net.netcoding.niftychat.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.minecraft.messages.BungeeServer;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.cache.UserFlagData;
import net.netcoding.niftycore.util.ListUtil;
import net.netcoding.niftycore.util.NumberUtil;
import net.netcoding.niftycore.util.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class Whois extends BukkitCommand {

	private static final List<String> ANTI_CONSOLE = Arrays.asList("Top secret!", "Nothing to see here...", "Someone's up to no good!",
			"One does not search for console, it searches for you.");

	public Whois(JavaPlugin plugin) {
		super(plugin, "whois");
		this.setPlayerTabComplete();
	}

	@Override
	protected void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		if (isConsole(args[0])) {
			this.getLog().error(sender, ANTI_CONSOLE.get(NumberUtil.rand(0, ANTI_CONSOLE.size() - 1)));
			return;
		}

		Set<BukkitMojangProfile> profiles = Realname.getProfileMatches(args[0]);

		if (ListUtil.isEmpty(profiles)) {
			this.getLog().error(sender, "Unable to locate the profile of {{0}}!", args[0]);
			return;
		}

		UserChatData userData = UserChatData.getCache(profiles.iterator().next());
		String separator = StringUtil.format("{0}{1}{2}", ChatColor.GRAY, ", ", ChatColor.RED);
		this.getLog().message(sender, "Whois {{0}}", userData.getProfile().getName());
		this.getLog().message(sender, "Display Name: {0}", userData.getDisplayName());
		this.getLog().message(sender, "Ranks: {{0}}", StringUtil.implode(separator, userData.getRankData().getRanks()));

		if (NiftyBukkit.getBungeeHelper().isDetected() && userData.isOnlineAnywhere()) {
			String serverName = userData.getProfile().getServer().getName();

			if (!userData.getFlagData(Vanish.FLAG).getValue() || this.hasPermissions(sender, "vanish", "see"))
				this.getLog().message(sender, "Server: {{0}}", serverName);
		}

		if (alias.matches("^.+?admin$") && this.hasPermissions(sender, "whois", "admin")) {
			if (userData.isOnlineAnywhere()) {
				if (!userData.getFlagData(Vanish.FLAG).getValue() || this.hasPermissions(sender, "vanish", "see"))
					this.getLog().message(sender, "Address: {{0}}:{{1,number,#}}", userData.getProfile().getAddress().getAddress().getHostAddress(), userData.getProfile().getAddress().getPort());
			}

			this.getLog().message(sender, "Nickname Revoked: {{0}}", (userData.getFlagData("nick-revoke").getValue() ? "Yes" : ChatColor.GREEN + "No"));
			UserFlagData globalMuteData = userData.getFlagData(Mute.FLAG, "*");

			if (globalMuteData.getValue())
				this.getLog().message(sender, "Muted: {{0}} (Expires {{1}})", "Globally", (globalMuteData.getExpires() > 0 ? Mute.EXPIRE_FORMAT.format(new Date(globalMuteData.getExpires())) : "Never"));
			else {
				List<UserFlagData> servers = new ArrayList<>();

				for (UserFlagData muteData : userData.getAllFlagData(Mute.FLAG)) {
					if (muteData.getValue())
						servers.add(muteData);
				}

				if (!servers.isEmpty()) {
					this.getLog().message(sender, "Muted:");

					for (UserFlagData muteData : servers)
						this.getLog().message(sender, "- {{0}} (Expires {{1}})", muteData.getServerName(), (muteData.getExpires() > 0 ? Mute.EXPIRE_FORMAT.format(new Date(muteData.getExpires())) : "Never"));
				} else
					this.getLog().message(sender, "Muted: {0}", ChatColor.GREEN + "No");
			}

			if (userData.getFlagData(SocialSpy.FLAG, "*").getValue())
				this.getLog().message(sender, "Spying: {{0}}", "Globally");
			else {
				List<String> servers = new ArrayList<>();

				for (UserFlagData spyData : userData.getAllFlagData(SocialSpy.FLAG)) {
					if (spyData.getValue())
						servers.add(spyData.getServerName());
				}

				if (!ListUtil.isEmpty(servers))
					this.getLog().message(sender, "Spying: {{0}}", StringUtil.implode(separator, servers));
				else
					this.getLog().message(sender, "Spying: {0}", ChatColor.GREEN + "No");
			}

			if (this.hasPermissions(sender, "vanish", "see")) {
				if (userData.getFlagData(Vanish.FLAG, "*").getValue())
					this.getLog().message(sender, "Vanished: {{0}}", "Globally");
				else {
					List<String> servers = new ArrayList<>();

					for (UserFlagData vanishData : userData.getAllFlagData(Vanish.FLAG)) {
						if (vanishData.getValue())
							servers.add(vanishData.getServerName());
					}

					if (!ListUtil.isEmpty(servers))
						this.getLog().message(sender, "Vanished: {{0}}", StringUtil.implode(separator, servers));
					else
						this.getLog().message(sender, "Vanished: {0}", ChatColor.GREEN + "No");
				}
			}
		} else {
			if (userData.isOnlineLocally()) {
				if (!userData.getFlagData(Vanish.FLAG).getValue() || this.hasPermissions(sender, "vanish", "see")) {
					Player player = userData.getOfflinePlayer().getPlayer();
					this.getLog().message(sender, "Operator: {{0}}", (userData.getOfflinePlayer().isOp() ? (ChatColor.GREEN + "Yes") : "No"));
					this.getLog().message(sender, "Location: {{0}}, {{1}}, {{2}}, {{3}}", player.getLocation().getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
					this.getLog().message(sender, "Game Mode: {{0}}", player.getGameMode().toString().toLowerCase());
					this.getLog().message(sender, "Health: {{0}}/{{1}}", player.getHealth(), player.getMaxHealth());
					this.getLog().message(sender, "Hunger: {{0}}/{{1}} ({{2}} Saturation)", player.getFoodLevel(), "20", ((player.getSaturation() > 0 ? "+" : "") + player.getSaturation()));
					this.getLog().message(sender, "Experience: {{0}} (Level {{1}})", player.getTotalExperience(), player.getLevel());
					//this.getLog().message(sender, "God Mode: {{0}}", "??");
					this.getLog().message(sender, "Flight: {{0}} ({1})", (player.getAllowFlight() ? (ChatColor.GREEN + "Yes") : "No"), ((!player.isFlying() ? "Not " : "") + "Flying"));
					//this.getLog().message(sender, "AFK: {{0}}", "??");
					//this.getLog().message(sender, "Jail: {{0}}", "??");
				}
			}
		}
	}

	@Override
	protected List<String> onTabComplete(final CommandSender sender, String alias, String[] args) throws Exception {
		final String arg = args[0].toLowerCase();
		List<String> names = new ArrayList<>();

		if (NiftyBukkit.getBungeeHelper().isDetected()) {
			for (BungeeServer server : NiftyBukkit.getBungeeHelper().getServers()) {
				for (BukkitMojangProfile profile : server.getPlayerList()) {
					UserChatData userData = UserChatData.getCache(profile);
					String displayName = userData.getStrippedDisplayName();

					if (displayName.toLowerCase().startsWith(arg) || displayName.toLowerCase().contains(arg))
						names.add(displayName);
				}
			}
		}

		return names;
	}

}