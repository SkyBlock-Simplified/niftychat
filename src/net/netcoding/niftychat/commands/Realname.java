package net.netcoding.niftychat.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftychat.managers.Cache;

import org.bukkit.command.CommandSender;

public class Realname extends BukkitCommand {

	public Realname(NiftyChat plugin) {
		super(plugin, "realname");
	}

	@Override
	public void command(CommandSender sender, String[] args) {
		if (this.hasPermissions(sender, "realname")) {
			String nickName = args[0];

			try {
				final List<String> foundData = Cache.MySQL.query("SELECT * FROM `nc_users` WHERE LOWER(`ufnick`) = LOWER(?) OR LOWER(`ufnick`) LIKE LOWER(?) GROUP BY `ufnick`;", new ResultCallback<List<String>>() {
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

				if (foundData.size() != 0 && !"".equals(foundData.get(1)))
					this.getLog().message(sender, "{%1$s} has the nickname {%2$s}", foundData.get(0), foundData.get(1));
				else
					this.getLog().error(sender, "No player with the nickname {%1$s} was found!", nickName);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}