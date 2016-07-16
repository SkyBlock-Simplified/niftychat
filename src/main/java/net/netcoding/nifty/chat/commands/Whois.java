package net.netcoding.nifty.chat.commands;

import net.netcoding.nifty.chat.cache.UserChatData;
import net.netcoding.nifty.chat.cache.UserFlagData;
import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.api.plugin.messaging.BungeeServer;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.minecraft.entity.living.human.Player;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.util.ListUtil;
import net.netcoding.nifty.core.util.NumberUtil;
import net.netcoding.nifty.core.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Whois extends MinecraftListener {

	private static final List<String> ANTI_CONSOLE = Arrays.asList("Top secret!", "Nothing to see here...", "Someone's up to no good!",
			"One does not search for console, it searches for you.");

	public Whois(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "whois",
			playerTabComplete = true
	)
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		if (isConsole(args[0])) {
			this.getLog().error(source, ANTI_CONSOLE.get(NumberUtil.rand(0, ANTI_CONSOLE.size() - 1)));
			return;
		}

		Set<MinecraftMojangProfile> profiles = Realname.getProfileMatches(args[0]);

		if (ListUtil.isEmpty(profiles)) {
			this.getLog().error(source, "Unable to locate the profile of {{0}}!", args[0]);
			return;
		}

		UserChatData userData = UserChatData.getCache(profiles.iterator().next());
		String separator = StringUtil.format("{0}{1}{2}", ChatColor.GRAY, ", ", ChatColor.RED);
		this.getLog().message(source, "Whois {{0}}", userData.getProfile().getName());
		this.getLog().message(source, "Display Name: {0}", userData.getDisplayName());
		this.getLog().message(source, "Ranks: {{0}}", StringUtil.implode(separator, userData.getRankData().getRanks()));

		if (Nifty.getBungeeHelper().getDetails().isDetected() && userData.isOnlineAnywhere()) {
			String serverName = userData.getProfile().getServer().getName();

			if (!userData.getFlagData(Vanish.FLAG).getValue() || this.hasPermissions(source, "vanish", "see"))
				this.getLog().message(source, "Server: {{0}}", serverName);
		}

		if (alias.matches("^.+?admin$") && this.hasPermissions(source, "whois", "admin")) {
			if (userData.isOnlineAnywhere()) {
				if (!userData.getFlagData(Vanish.FLAG).getValue() || this.hasPermissions(source, "vanish", "see"))
					this.getLog().message(source, "Address: {{0}}:{{1,number,#}}", userData.getProfile().getAddress().getAddress().getHostAddress(), userData.getProfile().getAddress().getPort());
			}

			this.getLog().message(source, "Nickname Revoked: {{0}}", (userData.getFlagData("nick-revoke").getValue() ? "Yes" : ChatColor.GREEN + "No"));
			UserFlagData globalMuteData = userData.getFlagData(Mute.FLAG, "*");

			if (globalMuteData.getValue())
				this.getLog().message(source, "Muted: {{0}} (Expires {{1}})", "Globally", (globalMuteData.getExpires() > 0 ? Mute.EXPIRE_FORMAT.format(new Date(globalMuteData.getExpires())) : "Never"));
			else {
				List<UserFlagData> servers = userData.getAllFlagData(Mute.FLAG).stream().filter(UserFlagData::getValue).collect(Collectors.toList());

				if (!servers.isEmpty()) {
					this.getLog().message(source, "Muted:");

					for (UserFlagData muteData : servers)
						this.getLog().message(source, "- {{0}} (Expires {{1}})", muteData.getServerName(), (muteData.getExpires() > 0 ? Mute.EXPIRE_FORMAT.format(new Date(muteData.getExpires())) : "Never"));
				} else
					this.getLog().message(source, "Muted: {0}", ChatColor.GREEN + "No");
			}

			if (userData.getFlagData(SocialSpy.FLAG, "*").getValue())
				this.getLog().message(source, "Spying: {{0}}", "Globally");
			else {
				List<String> servers = userData.getAllFlagData(SocialSpy.FLAG).stream().filter(UserFlagData::getValue).map(UserFlagData::getServerName).collect(Collectors.toList());

				if (!ListUtil.isEmpty(servers))
					this.getLog().message(source, "Spying: {{0}}", StringUtil.implode(separator, servers));
				else
					this.getLog().message(source, "Spying: {0}", ChatColor.GREEN + "No");
			}

			if (this.hasPermissions(source, "vanish", "see")) {
				if (userData.getFlagData(Vanish.FLAG, "*").getValue())
					this.getLog().message(source, "Vanished: {{0}}", "Globally");
				else {
					List<String> servers = userData.getAllFlagData(Vanish.FLAG).stream().filter(UserFlagData::getValue).map(UserFlagData::getServerName).collect(Collectors.toList());

					if (!ListUtil.isEmpty(servers))
						this.getLog().message(source, "Vanished: {{0}}", StringUtil.implode(separator, servers));
					else
						this.getLog().message(source, "Vanished: {0}", ChatColor.GREEN + "No");
				}
			}
		} else {
			if (userData.isOnlineLocally()) {
				if (!userData.getFlagData(Vanish.FLAG).getValue() || this.hasPermissions(source, "vanish", "see")) {
					Player player = userData.getOfflinePlayer().getPlayer();
					this.getLog().message(source, "Operator: {{0}}", (userData.getOfflinePlayer().isOp() ? (ChatColor.GREEN + "Yes") : "No"));
					this.getLog().message(source, "Location: {{0}}, {{1}}, {{2}}, {{3}}", player.getLocation().getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
					this.getLog().message(source, "Game Mode: {{0}}", player.getGameMode().toString().toLowerCase());
					this.getLog().message(source, "Health: {{0}}/{{1}}", player.getHealth(), player.getMaxHealth());
					this.getLog().message(source, "Hunger: {{0}}/{{1}} ({{2}} Saturation)", player.getFoodLevel(), "20", ((player.getSaturation() > 0 ? "+" : "") + player.getSaturation()));
					this.getLog().message(source, "Experience: {{0}} (Level {{1}})", player.getTotalExperience(), player.getLevel());
					//this.getLog().message(sender, "God Mode: {{0}}", "??");
					this.getLog().message(source, "Flight: {{0}} ({1})", (player.getAllowFlight() ? (ChatColor.GREEN + "Yes") : "No"), ((!player.isFlying() ? "Not " : "") + "Flying"));
					//this.getLog().message(sender, "AFK: {{0}}", "??");
					//this.getLog().message(sender, "Jail: {{0}}", "??");
				}
			}
		}
	}

	@Command.TabComplete(index = 0, name = "whois")
	protected List<String> onTabComplete(CommandSource source, String alias, String[] args) throws Exception {
		final String arg = args[0].toLowerCase();
		List<String> names = new ArrayList<>();

		if (Nifty.getBungeeHelper().getDetails().isDetected()) {
			for (BungeeServer<MinecraftMojangProfile> server : Nifty.getBungeeHelper().getServers()) {
				for (MinecraftMojangProfile profile : server.getPlayerList()) {
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