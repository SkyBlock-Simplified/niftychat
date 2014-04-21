package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.Config;

import org.bukkit.command.CommandSender;

public class Realname extends BukkitCommand {

	public Realname(NiftyChat plugin) {
		super(plugin, "realname");
	}

	@Override
	public void onCommand(CommandSender sender, String alias, String[] args) {
		String nickName = args[0];

		try {
			final List<String> foundData = Cache.MySQL.query(StringUtil.format("SELECT * FROM `{0}` WHERE LOWER(`ufnick`) = LOWER(?) OR LOWER(`ufnick`) LIKE LOWER(?) GROUP BY `ufnick`;", Config.USER_TABLE), new ResultCallback<List<String>>() {
				@Override
				public List<String> handle(ResultSet result) throws SQLException {
					List<String> data = new ArrayList<>();

					if (result.next()) {
						data.add(result.getString("user"));
						String nick = result.getString("nick");
						data.add(result.wasNull() ? null : nick);
					}

					return data;
				}
			}, nickName, ("%" + nickName + "%"));

			if (foundData.size() != 0 && StringUtil.notEmpty(foundData.get(1)))
				this.getLog().message(sender, "{{0}} has the nickname {{1}}.", foundData.get(0), foundData.get(1));
			else
				this.getLog().error(sender, "No player with the nickname {{0}} was found!", nickName);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}