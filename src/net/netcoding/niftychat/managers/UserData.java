package net.netcoding.niftychat.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.util.RegexUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class UserData extends BukkitHelper {

	private String playerName;

	private List<String> ranks = new ArrayList<String>();

	private String displayName;

	private String lastMessage;

	private boolean hasMoved = false;

	private boolean isVanished = false;

	public UserData(JavaPlugin plugin, Player player) {
		this(plugin, player.getName());
	}

	public UserData(JavaPlugin plugin, String playerName) {
		super(plugin);
		this.playerName = playerName;
	}

	/*public void addRank(String rank) {
		this.ranks.add(rank);
	}*/

	public void addRanks(List<String> ranks) {
		this.ranks.addAll(ranks);
	}

	public void clearRanks() {
		this.ranks.clear();
	}

	public String getDisplayName() {
		return this.displayName;
	}

	private String _getDisplayName() throws SQLException {
		return _getDisplayName(this.getName(), this.getPrimaryRank());
	}

	private static String _getDisplayName(final String playerName, final String primaryRank) throws SQLException {

		return Cache.MySQL.query("SELECT * FROM `nc_users` WHERE `user` = ? LIMIT 1;", new ResultCallback<String>() {
			@Override
			public String handle(ResultSet result) throws SQLException {
				String displayName = playerName;
				String rank        = "default";

				if (result.next()) {
					displayName   = result.getString("user");
					rank          = primaryRank;
					Player player = Bukkit.getPlayer(displayName);
					if (player != null) displayName = player.getName();
					String nick   = result.getString("nick");
					displayName   = (result.wasNull() ? displayName : ("*" + RegexUtil.replaceColor(nick, RegexUtil.REPLACE_ALL_PATTERN)));
				}

				RankData rankInfo = Cache.rankData.get(rank);
				String prefix     = rankInfo.getPrefix();
				String suffix     = rankInfo.getSuffix();

				return String.format("%1$s%2$s%3$s", prefix, displayName, suffix);
			}
		}, matchPlayerName(playerName));
	}

	public String getName() {
		return this.playerName;
	}

	public static String getOfflineDisplayName(String playerName) throws SQLException {
		UserData userData = Cache.userData.get(playerName);

		if (userData != null) {
			return userData.getDisplayName();
		} else {
			List<String> ranks = _getRanks(playerName);
			return _getDisplayName(playerName, ranks.get(0));
		}
	}

	public static List<String> getOfflineRanks(final String playerName) throws SQLException {
		UserData userData = Cache.userData.get(playerName);

		if (userData != null) {
			return userData.getRanks();
		} else
			return _getRanks(playerName);
	}

	public Player getPlayer() {
		return BukkitHelper.matchPlayer(this.getName());
	}

	public String getPrimaryRank() {
		return this.getRanks().get(0);
	}

	public List<String> getRanks() {
		return this.ranks;
	}

	public List<String> _getRanks() throws SQLException {
		return _getRanks(this.getName());
	}

	private static List<String> _getRanks(String playerName) throws SQLException {
		return Cache.MySQL.query("SELECT `rank` FROM `nc_ranks` `r` LEFT JOIN `nc_user_ranks` `ur` ON `ur`.`rank_id` = `r`.`id` JOIN `nc_users` `u` ON `u`.`id` = `ur`.`user_id` WHERE `u`.`user` = ?;", new ResultCallback<List<String>>() {
			@Override
			public List<String> handle(ResultSet result) throws SQLException {
				List<String> ranks = new ArrayList<>();
				while (result.next()) ranks.add(result.getString("rank"));
				if (ranks.size() == 0) ranks.add("default");
				return ranks;
			}
		}, matchPlayerName(playerName));
	}

	public boolean hasMoved() {
		return this.hasMoved;
	}

	public boolean hasPermissions(String... permissions) {
		return super.hasPermissions(Bukkit.getPlayer(this.getName()), permissions);
	}

	public boolean hasRank(String rank) {
		return this.ranks.contains(rank);
	}

	public boolean hasRepeatedMessage(String message) {
		if (this.hasPermissions("chat", "bypass", "repeat")) return false;
		String lastMessage  = this.lastMessage;

		if (!"".equals(message) && message.equalsIgnoreCase(lastMessage))
			return true;
		else
			this.lastMessage = message;

		return false;
	}

	public boolean isVanished() {
		return this.isVanished;
	}

	public static String matchPlayerName(final String playerName) {
		Player player = BukkitHelper.matchPlayer(playerName);

		if (player != null)
			return player.getName();
		else {
			try {
				return Cache.MySQL.query("SELECT `user` FROM `nc_users` WHERE LOWER(`user`) = LOWER(?) OR LOWER(`user`) LIKE LOWER(?) GROUP BY `user` LIMIT 1;", new ResultCallback<String>() {
					@Override
					public String handle(ResultSet result) throws SQLException {
						return (result.next() ? result.getString("user") : playerName);
					}
				}, playerName, (playerName + "%"));
			} catch (SQLException ex) {
				return playerName;
			}
		}
	}

	public void removeRank(String rank) {
		this.ranks.remove(rank);
	}

	public void setMoved() {
		this.setMoved(true);
	}

	public void setMoved(boolean moved) {
		this.hasMoved = moved;
	}

	public void setVanished() {
		this.setVanished(true);
	}

	public void setVanished(boolean vanished) {
		this.isVanished = vanished;
	}

	public static String setRank(final String playerName, final String rank) throws SQLException {		
		return Cache.MySQL.query("SELECT * FROM `nc_ranks` WHERE `rank` = ? LIMIT 1;", new ResultCallback<String>() {
			@Override
			public String handle(ResultSet result) throws SQLException {
				if (result.next()) {
					final int rankId = result.getInt("id");
					String userName  = matchPlayerName(playerName);

					Cache.MySQL.query("SELECT * FROM `nc_users` WHERE `user` = ? LIMIT 1;", new ResultCallback<Void>() {
						@Override
						public Void handle(ResultSet result) throws SQLException {
							if (result.next()) {
								int userId = result.getInt("id");
								Cache.MySQL.update("DELETE FROM `nc_user_ranks` WHERE `user_id` = ?;", userId);
								Cache.MySQL.update("INSERT INTO `nc_user_ranks` (`user_id`, `rank_id`) VALUES (?, ?);", userId, rankId);
							}

							return null;
						}
					}, userName);

					return userName;
				}

				return null;
			}
		}, rank);
	}

	public void updateDisplayName() throws SQLException {
		String displayName = this._getDisplayName();
		this.displayName = displayName;
		Player player = Bukkit.getPlayer(this.getName());

		if (player != null) {
			player.setDisplayName(displayName);
			player.setCustomName(displayName);
		}
	}

	public void updateRanks() throws SQLException {
		this.clearRanks();
		this.addRanks(this._getRanks());
	}

	public void updateTabListName() {
		String displayName = this.getDisplayName();
		if (displayName.length() > 16) displayName = displayName.substring(0, 16);
		Player player = Bukkit.getPlayer(this.getName());
		if (player != null) player.setPlayerListName(displayName);
	}

	/*public static void updateTabListNames() {
		for (String playerName : Cache.userData.keySet())
			Cache.userData.get(playerName).updateTabListName();
	}*/

	public void updateVaultRanks() {
		try {
			if (Cache.permissions == null) return;
			String playerName = this.getName();

			for (String group : Cache.permissions.getPlayerGroups((String)null, playerName))
				Cache.permissions.playerRemoveGroup((String)null, playerName, group); // error

			if (this.getPrimaryRank() != "default") {
				for (String rank : this.getRanks())
					Cache.permissions.playerAddGroup((String)null, playerName, rank);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}