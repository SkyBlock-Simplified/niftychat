package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.factory.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftybukkit.util.ListUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.Config;

import org.bukkit.command.CommandSender;

public class Realname extends BukkitCommand {

	public Realname(NiftyChat plugin) {
		super(plugin, "realname");
		this.setMaximumArgsLength(1);
	}

	@Override
	public void onCommand(final CommandSender sender, String alias, final String[] args) throws Exception {
		final List<String> foundData = Cache.MySQL.query(StringUtil.format("SELECT * FROM `{0}` WHERE LOWER(`ufnick`) = LOWER(?) OR LOWER(`ufnick`) LIKE LOWER(?) GROUP BY `ufnick`;", Config.USER_TABLE), new ResultCallback<List<String>>() {
			@Override
			public List<String> handle(ResultSet result) throws SQLException {
				List<String> data = new ArrayList<>();

				if (result.next()) {
					MojangProfile profile;

					try {
						profile = NiftyBukkit.getMojangRepository().searchByUniqueId(UUID.fromString(result.getString("uuid")));
					} catch (ProfileNotFoundException pnfe) {
						getLog().error(sender, "Unable to locate the profile of {{0}}!", args[0]);
						return data;
					}

					data.add(profile.getName());
					String nick = result.getString("nick");
					data.add(result.wasNull() ? null : nick);
				}

				return data;
			}
		}, args[0], ("%" + args[0] + "%"));

		if (ListUtil.notEmpty(foundData))
			this.getLog().message(sender, "{{0}} has the nickname {{1}}.", foundData.get(0), foundData.get(1));
		else
			this.getLog().error(sender, "No player with the nickname {{0}} was found!", args[0]);
	}

}