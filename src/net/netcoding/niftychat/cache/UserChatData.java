package net.netcoding.niftychat.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.util.RegexUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftybukkit.util.concurrent.ConcurrentSet;
import net.netcoding.niftyranks.cache.UserRankData;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class UserChatData extends BukkitHelper {

	private static final transient ConcurrentSet<UserChatData> cache = new ConcurrentSet<>();

	private MojangProfile profile;

	private String displayName;

	private String lastMessage;

	private boolean hasMoved = false;

	private boolean isVanished = false;

	private UserRankData rankData;

	public UserChatData(JavaPlugin plugin, Player player) {
		super(plugin);
		this.profile = NiftyBukkit.getMojangRepository().searchByExactPlayer(player);
		this.rankData = UserRankData.getCache(this.profile.getUniqueId());
		cache.add(this);
	}

	public static ConcurrentSet<UserChatData> getCache() {
		return cache;
	}

	public static UserChatData getCache(UUID uuid) {
		for (UserChatData data : cache) {
			if (data.getUniqueId().equals(uuid))
				return data;
		}

		return null;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	private String _getDisplayName() throws SQLException {
		return _getDisplayName(this.profile);
	}

	private static String _getDisplayName(final MojangProfile profile) throws SQLException {
		return Cache.MySQL.query(StringUtil.format("SELECT * FROM `{0}` WHERE `uuid` = ?;", Config.USER_TABLE), new ResultCallback<String>() {
			@Override
			public String handle(ResultSet result) throws SQLException {
				String displayName = profile.getName();
				String rank = "default";

				if (result.next()) {
					displayName = result.getString("user");
					rank = UserRankData.getOfflineRanks(profile).get(0);
					Player player = findPlayer(displayName);
					if (player != null) displayName = player.getName();
					String nick = result.getString("nick");
					displayName = (result.wasNull() ? displayName : ("*" + RegexUtil.replaceColor(nick, RegexUtil.REPLACE_ALL_PATTERN)));
				}

				RankFormat rankInfo = RankFormat.getCache(rank);
				String prefix = rankInfo.getPrefix();
				String suffix = rankInfo.getSuffix();

				return StringUtil.format("{0}{1}{2}", prefix, displayName, suffix);
			}
		}, profile.getUniqueId());
	}

	public String getName() {
		return this.profile.getName();
	}

	public static String getOfflineDisplayName(MojangProfile profile) throws SQLException {
		UserChatData userData = getCache(profile.getUniqueId());

		if (userData != null)
			return userData.getDisplayName();
		else
			return _getDisplayName(profile);
	}

	public Player getPlayer() {
		return BukkitHelper.findPlayer(this.getName());
	}

	public UserRankData getRankData() {
		return this.rankData;
	}

	public UUID getUniqueId() {
		return this.profile.getUniqueId();
	}

	public boolean hasMoved() {
		return this.hasMoved;
	}

	public boolean hasPermissions(String... permissions) {
		return super.hasPermissions(this.getPlayer(), permissions);
	}

	public boolean hasRepeatedMessage(String message) {
		if (this.hasPermissions("chat", "bypass", "repeat")) return false;
		String lastMessage  = this.lastMessage;

		if (StringUtil.notEmpty(lastMessage) && lastMessage.equalsIgnoreCase(message))
			return true;
		else
			this.lastMessage = message;

		return false;
	}

	public boolean isVanished() {
		return this.isVanished;
	}

	/*public static String matchPlayerName(final String playerName) {
		Player player = BukkitHelper.findPlayer(playerName);

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
	}*/

	public static void removeCache(UUID uuid) {
		for (UserChatData data : cache) {
			if (data.getUniqueId().equals(uuid))
				cache.remove(data);
		}
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

	public void updateDisplayName() throws SQLException {
		String displayName = this._getDisplayName();
		this.displayName = displayName;
		this.getPlayer().setDisplayName(displayName);
		this.getPlayer().setCustomName(displayName);
	}

	public void updateTabListName() {
		String displayName = this.getDisplayName();
		if (displayName.length() > 16) displayName = displayName.substring(0, 16);
		this.getPlayer().setPlayerListName(displayName);
	}

	/*public static void updateTabListNames() {
		for (String playerName : Cache.userData.keySet())
			Cache.userData.get(playerName).updateTabListName();
	}*/

}