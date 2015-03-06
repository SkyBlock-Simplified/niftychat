package net.netcoding.niftychat.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.factory.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.minecraft.BungeeHelper;
import net.netcoding.niftybukkit.minecraft.BungeeServer;
import net.netcoding.niftybukkit.mojang.MojangProfile;
import net.netcoding.niftybukkit.util.ListUtil;
import net.netcoding.niftybukkit.util.RegexUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftybukkit.util.TimeUtil;
import net.netcoding.niftybukkit.util.concurrent.ConcurrentSet;
import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftyranks.cache.UserRankData;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

public class UserChatData extends BukkitHelper {

	private static final transient ConcurrentSet<UserChatData> cache = new ConcurrentSet<>();
	private MojangProfile profile;
	private String lastMessage;
	private MojangProfile lastMessenger;
	private boolean hasMoved = false;
	private HashSet<UserFlagData> flagData = new HashSet<>();

	public UserChatData(JavaPlugin plugin, MojangProfile profile) {
		this(plugin, profile, true);
	}

	private UserChatData(JavaPlugin plugin, MojangProfile profile, boolean addToCache) {
		super(plugin);
		this.profile = profile;
		if (addToCache) cache.add(this);
	}

	public void addFlagData(UserFlagData flagData) {
		this.flagData.add(flagData);
	}

	public void applyFlagData(final String flag) {
		if (!this.isOnline()) return;
		final boolean flagValue = this.getFlagData(flag).getValue();

		if ("vanished".equals(flag)) {
			this.getPlugin().getServer().getScheduler().runTask(this.getPlugin(), new Runnable() {
				@Override
				public void run() {
					for (UserChatData userData : getCache()) {
						if (userData.getProfile().equals(getProfile())) continue;

						if (flagValue && !userData.hasPermissions("vanish", "see"))
							userData.getOfflinePlayer().getPlayer().hidePlayer(getOfflinePlayer().getPlayer());
						else
							userData.getOfflinePlayer().getPlayer().showPlayer(getOfflinePlayer().getPlayer());

						if (userData.getFlagData(flag).getValue() && !hasPermissions("vanish", "see"))
							getOfflinePlayer().getPlayer().hidePlayer(userData.getOfflinePlayer().getPlayer());
						else
							getOfflinePlayer().getPlayer().showPlayer(userData.getOfflinePlayer().getPlayer());
					}
				}
			});
		}
	}

	public static ConcurrentSet<UserChatData> getCache() {
		return cache;
	}

	public static UserChatData getCache(MojangProfile profile) {
		for (UserChatData data : getCache()) {
			if (profile.equals(data.getProfile()))
				return data;
		}

		return new UserChatData(NiftyChat.getPlugin(NiftyChat.class), profile, false);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		UserChatData userData = (UserChatData)obj;
		return this.getProfile().equals(userData.getProfile());
	}

	public String getDisplayName() {
		return this.getDisplayName(false);
	}

	private String getDisplayName(boolean fetch) {
		if (!fetch && this.isOnline())
			return this.getOfflinePlayer().getPlayer().getDisplayName();
		else {
			try {
				return _getDisplayName(this.profile);
			} catch (SQLException ex) {
				return this.getProfile().getName();
			}
		}
	}

	private static String _getDisplayName(final MojangProfile profile) throws SQLException {
		return Cache.MySQL.query(StringUtil.format("SELECT * FROM `{0}` WHERE `uuid` = ?;", Config.USER_TABLE), new ResultCallback<String>() {
			@Override
			public String handle(ResultSet result) throws SQLException {
				String displayName = profile.getName();
				String rank = "default";

				if (result.next()) {
					rank = UserRankData.getOfflineRanks(profile).get(0);
					String nick = result.getString("nick");
					displayName = (result.wasNull() ? displayName : ("*" + RegexUtil.replaceColor(nick, RegexUtil.REPLACE_ALL_PATTERN)));
				}

				RankFormat rankInfo = RankFormat.getCache(rank);
				String prefix = StringUtil.isEmpty(rankInfo.getPrefix()) ? "&7" : rankInfo.getPrefix();
				String suffix = rankInfo.getSuffix();

				return StringUtil.format("{0}{1}{2}", prefix, displayName, suffix);
			}
		}, profile.getUniqueId());
	}

	public List<UserFlagData> getAllFlagData(String flag) {
		if (ListUtil.isEmpty(this.flagData)) this.reloadFlagData();
		List<UserFlagData> flagMatches = new ArrayList<>();

		for (UserFlagData flagData : this.flagData) {
			if (flagData.getFlag().equalsIgnoreCase(flag))
				flagMatches.add(flagData);
		}

		return flagMatches;
	}

	public UserFlagData getFlagData(String flag) {
		return this.getFlagData(flag, "");
	}

	public UserFlagData getFlagData(String flag, String server) {
		List<UserFlagData> flagDatas = this.getAllFlagData(flag);
		BungeeHelper bungeeHelper = NiftyBukkit.getBungeeHelper();
		UserFlagData found = new UserFlagData(flag);
		BungeeServer bungeeServer = StringUtil.isEmpty(server) || server.equals("*") ? bungeeHelper.getServer() : bungeeHelper.getServer(server);

		for (UserFlagData flagData : flagDatas) {
			if (flagData.getValue()) {
				if (flagData.isGlobal()) {
					found = flagData;
					break;
				} else if (bungeeHelper.isOnline()) {
					if (bungeeServer.equals(flagData.getServer())) {
						found = flagData;
						break;
					}
				}
			}
		}

		return found;
	}

	public MojangProfile getLastMessenger() {
		return this.lastMessenger;
	}

	public OfflinePlayer getOfflinePlayer() {
		return this.getPlugin().getServer().getOfflinePlayer(this.getProfile().getUniqueId());
	}

	public MojangProfile getProfile() {
		return this.profile;
	}

	public UserRankData getRankData() {
		return UserRankData.getCache(this.getProfile());
	}

	public boolean hasMoved() {
		return this.hasMoved;
	}

	public boolean hasPermissions(String... permissions) {
		return super.hasPermissions(this.getOfflinePlayer().getPlayer(), permissions);
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

	public boolean isOnline() {
		return this.getOfflinePlayer().isOnline();
	}

	public static void removeCache(MojangProfile profile) {
		for (UserChatData data : cache) {
			if (data.getProfile().equals(profile))
				cache.remove(data);
		}
	}

	public void reloadFlagData() {
		try {
			this.flagData.clear();
			this.flagData.addAll(Cache.MySQL.query(StringUtil.format("SELECT * FROM `{0}` WHERE `uuid` = ?;", Config.USER_FLAGS_TABLE), new ResultCallback<List<UserFlagData>>() {
				@Override
				public List<UserFlagData> handle(ResultSet result) throws SQLException {
					List<UserFlagData> flags = new ArrayList<>();

					while (result.next()) {
						UserFlagData flagData = new UserFlagData(result.getString("flag"));
						Timestamp expires = result.getTimestamp("_expires");
						flagData.setExpires(result.wasNull() ? 0 : expires.getTime());
						flagData.setSubmitted(result.getTimestamp("_submitted").getTime());
						flagData.setServer(result.getString("server"));
						flagData.setValue(result.getBoolean("value"));
						flags.add(flagData);
					}

					return flags;
				}
			}, this.getProfile().getUniqueId()));
		} catch (SQLException ex) {
			this.getLog().console(ex);
		}
	}

	public boolean resetNonGlobalFlagData(String flag) throws SQLException {
		return this.resetFlagData(flag, "*");
	}

	public boolean resetFlagData(String flag, String notServer) throws SQLException {
		return Cache.MySQL.update(StringUtil.format("DELETE FROM `{0}` WHERE `uuid` = ? AND `flag` = ? AND `server` <> ?;", Config.USER_FLAGS_TABLE), this.getProfile().getUniqueId(), flag, notServer);
	}

	public void setLastMessenger(MojangProfile profile) {
		this.lastMessenger = profile;
	}

	public void setMoved() {
		this.setMoved(true);
	}

	public void setMoved(boolean moved) {
		this.hasMoved = moved;
	}

	public void updateDisplayName() {
		String displayName = this.getDisplayName(true);
		this.getOfflinePlayer().getPlayer().setDisplayName(displayName);
		this.getOfflinePlayer().getPlayer().setCustomName(displayName);
	}

	public boolean updateFlagData(String flag, boolean value, String server, long expires) throws SQLException {
		String sqlFormat = expires > 0 ? TimeUtil.SQL_FORMAT.format(new Date(expires)) : null;
		return Cache.MySQL.update(StringUtil.format("INSERT INTO `{0}` (`uuid`, `flag`, `value`, `server`, `_expires`) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `value` = ?, `_expires` = ?;", Config.USER_FLAGS_TABLE), this.getProfile().getUniqueId(), flag, !value, server, sqlFormat, !value, sqlFormat);
	}

	public void updateTabListName() {
		String displayName = this.getDisplayName(true);
		if (displayName.length() > 16) displayName = displayName.substring(0, 16);
		this.getOfflinePlayer().getPlayer().setPlayerListName(displayName);
	}

}