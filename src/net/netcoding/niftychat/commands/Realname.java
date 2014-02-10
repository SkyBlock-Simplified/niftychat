package net.netcoding.niftychat.commands;

import static net.netcoding.niftychat.managers.Cache.Log;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.netcoding.niftybukkit.database.ResultSetCallbackNR;
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
		if (super.hasPermissions(sender, "realname")) {
			String nickName = args[0];

			try {
				final List<String> foundData = new ArrayList<String>();

				Cache.MySQL.query("SELECT * FROM `nc_users` WHERE LOWER(`ufnick`) = LOWER(?) OR LOWER(`ufnick`) LIKE LOWER(?) GROUP BY `ufnick`;", new ResultSetCallbackNR() {
					@Override
					public void handleResult(ResultSet result) throws SQLException, Exception {
						if (result.next()) {
							foundData.add(result.getString("user"));
							String nick = result.getString("nick");
							foundData.add(result.wasNull() ? null : nick);
						}
					}
				}, nickName, ("%" + nickName + "%"));

				if (foundData.size() != 0 && !"".equals(foundData.get(1)))
					Log.message(sender, "{%1$s} has the nickname {%2$s}", foundData.get(0), foundData.get(1));
				else
					Log.error(sender, "No player with the nickname {%1$s} was found!", nickName);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}