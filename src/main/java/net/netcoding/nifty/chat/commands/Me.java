package net.netcoding.nifty.chat.commands;

import net.netcoding.nifty.chat.listeners.Chat;
import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.minecraft.entity.living.human.Player;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.mojang.MojangProfile;
import net.netcoding.nifty.core.util.StringUtil;

public class Me extends MinecraftListener {

	public Me(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "me",
			checkHelp = false
	)
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		String name = StringUtil.format("{0}[{{1}}{0}]", ChatColor.DARK_GRAY, "Console");
		String message = StringUtil.implode(" ", args);

		if (isPlayer(source)) {
			Player player = (Player)source;
			name = player.getDisplayName();

			if (Chat.check(this, player, message)) {
				message = Chat.filter(this, player, "me", message);
				message = Chat.format(this, player, "me", message);
			} else
				return;
		}

		message = StringUtil.format("{0} {1}{2}", name, ChatColor.GRAY, message);
		this.getLog().message(this.getPlugin().getServer().getConsoleSource(), message);

		for (MojangProfile profile : Nifty.getBungeeHelper().getPlayerList())
			this.getLog().message(((MinecraftMojangProfile)profile).getOfflinePlayer().getPlayer(), message);
	}

}