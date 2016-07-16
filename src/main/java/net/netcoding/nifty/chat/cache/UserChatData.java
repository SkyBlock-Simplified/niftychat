package net.netcoding.nifty.chat.cache;

import net.netcoding.nifty.chat.NiftyChat;
import net.netcoding.nifty.chat.commands.Vanish;
import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.api.plugin.messaging.BungeeServer;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.common.mojang.MinecraftMojangCache;
import net.netcoding.nifty.core.util.ListUtil;
import net.netcoding.nifty.core.util.RegexUtil;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.core.util.TimeUtil;
import net.netcoding.nifty.core.util.concurrent.Concurrent;
import net.netcoding.nifty.core.util.concurrent.ConcurrentSet;
import net.netcoding.nifty.ranks.cache.UserRankData;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class UserChatData extends MinecraftMojangCache<MinecraftMojangProfile> {

	private static final transient ConcurrentSet<UserChatData> CACHE = Concurrent.newSet();
	private final HashSet<UserFlagData> flagData = new HashSet<>();
	private String lastMessage;
	private MinecraftMojangProfile lastMessenger;
	private boolean hasMoved = false;

	public UserChatData(MinecraftPlugin plugin, MinecraftMojangProfile profile) {
		this(plugin, profile, true);
	}

	private UserChatData(MinecraftPlugin plugin, MinecraftMojangProfile profile, boolean addToCache) {
		super(plugin, profile);

		if (addToCache)
			CACHE.add(this);
	}

	public void addFlagData(UserFlagData flagData) {
		this.flagData.add(flagData);
	}

	public void applyFlagData(final String flag) {
		if (!this.getOfflinePlayer().isOnline()) return;
		final boolean flagValue = this.getFlagData(flag).getValue();

		if (Vanish.FLAG.equals(flag)) {
			Nifty.getScheduler().schedule(this.getPlugin(), () -> {
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
			});
		}
	}

	public static ConcurrentSet<UserChatData> getCache() {
		CACHE.stream().filter(data -> !data.isOnlineLocally()).forEach(CACHE::remove);
		return CACHE;
	}

	public static UserChatData getCache(MinecraftMojangProfile profile) {
		for (UserChatData data : getCache()) {
			if (profile.equals(data.getProfile()))
				return data;
		}

		return new UserChatData(NiftyChat.getPlugin(NiftyChat.class), profile, false);
	}

	public String getDisplayName() {
		return this.getDisplayName(false);
	}

	private String getDisplayName(boolean fetch) {
		if (!fetch && this.isOnlineLocally())
			return this.getOfflinePlayer().getPlayer().getDisplayName();

		try {
			return _getDisplayName(this.getProfile());
		} catch (SQLException ex) {
			return this.getProfile().getName();
		}
	}

	private static String _getDisplayName(final MinecraftMojangProfile profile) throws SQLException {
		return NiftyChat.getSQL().query(StringUtil.format("SELECT * FROM {0} WHERE uuid = ?;", Config.USER_TABLE), result -> {
			String displayName = profile.getName();
			String rank = RankFormat.DEFAULT.getRank();

			if (result.next()) {
				rank = UserRankData.getCache(profile).getPrimaryRank();
				String nick = result.getString("nick");
				displayName = (result.wasNull() ? displayName : ("*" + RegexUtil.replaceColor(nick, RegexUtil.REPLACE_ALL_PATTERN)));
			}

			RankFormat rankInfo = RankFormat.getCache(rank);
			String prefix = StringUtil.isEmpty(rankInfo.getPrefix()) ? "&7" : rankInfo.getPrefix();
			String suffix = rankInfo.getSuffix();

			return StringUtil.format("{0}{1}{2}", prefix, displayName, suffix);
		}, profile.getUniqueId());
	}

	public List<UserFlagData> getAllFlagData(String flag) {
		if (ListUtil.isEmpty(this.flagData))
			this.reloadFlagData();

		return this.flagData.stream().filter(flagData -> flagData.getFlag().equalsIgnoreCase(flag)).collect(Collectors.toList());
	}

	public UserFlagData getFlagData(String flag) {
		return this.getFlagData(flag, "");
	}

	public UserFlagData getFlagData(String flag, String server) {
		List<UserFlagData> flagDatas = this.getAllFlagData(flag);
		UserFlagData found = null;

		for (UserFlagData flagData : flagDatas) {
			if (flagData.isGlobal() && (flagData.getValue() || "*".equals(server))) {
				found = flagData;
				break;
			}
		}

		if (Nifty.getBungeeHelper().getDetails().isDetected() && found == null) {
			BungeeServer bungeeServer = (StringUtil.isEmpty(server) || server.equals("*")) ? Nifty.getBungeeHelper().getServer() : Nifty.getBungeeHelper().getServer(server);

			for (UserFlagData flagData : flagDatas) {
				if (flagData.isGlobal()) continue;

				if (bungeeServer.equals(flagData.getServer())) {
					found = flagData;
					break;
				}
			}
		}

		return found == null ? new UserFlagData(flag, server) : found;
	}

	public MinecraftMojangProfile getLastMessenger() {
		return this.lastMessenger;
	}

	public UserRankData getRankData() {
		return UserRankData.getCache(this.getProfile());
	}

	public String getStrippedDisplayName() {
		return RegexUtil.strip(this.getDisplayName(), RegexUtil.VANILLA_PATTERN).replaceAll("^\\*", "");
	}

	public boolean hasMoved() {
		return this.hasMoved;
	}

	public boolean hasRepeatedMessage(String message) {
		if (this.hasPermissions("chat", "bypass", "repeat")) return false;
		if (StringUtil.notEmpty(this.lastMessage) && this.lastMessage.equalsIgnoreCase(message)) return true;
		this.lastMessage = message;
		return false;
	}

	public void reloadFlagData() {
		try {
			this.flagData.clear();
			this.flagData.addAll(NiftyChat.getSQL().query(StringUtil.format("SELECT * FROM {0} WHERE uuid = ?;", Config.USER_FLAGS_TABLE), result -> {
				List<UserFlagData> flags = new ArrayList<>();

				while (result.next()) {
					UserFlagData flagData = new UserFlagData(result.getString("flag"), result.getString("server"));
					Timestamp expires = result.getTimestamp("_expires");
					flagData.setExpires(result.wasNull() ? 0 : expires.getTime());
					flagData.setSubmitted(result.getTimestamp("_submitted").getTime());
					flagData.setValue(result.getBoolean("_value"));
					flags.add(flagData);
				}

				return flags;
			}, this.getProfile().getUniqueId()));
		} catch (SQLException ex) {
			this.getLog().console(ex);
		}
	}

	public static void removeCache(MinecraftMojangProfile profile) {
		for (UserChatData data : CACHE) {
			if (data.getProfile().equals(profile)) {
				CACHE.remove(data);
				break;
			}
		}
	}

	public boolean resetNonGlobalFlagData2(String flag) throws SQLException {
		return this.resetFlagData(flag, "");
	}

	public boolean resetFlagData(String flag, String server) throws SQLException {
		return NiftyChat.getSQL().update(StringUtil.format("DELETE FROM {0} WHERE uuid = ? AND flag = ? AND (server = ? OR \"\" = ?);", Config.USER_FLAGS_TABLE), this.getProfile().getUniqueId(), flag, server, server);
	}

	public void setLastMessenger(MinecraftMojangProfile profile) {
		this.lastMessenger = profile;
	}

	public void setMoved() {
		this.setMoved(true);
	}

	public void setMoved(boolean moved) {
		this.hasMoved = moved;
	}

	public void updateDisplayName() {
		if (this.isOnlineLocally()) {
			String displayName = this.getDisplayName(true);
			this.getOfflinePlayer().getPlayer().setDisplayName(displayName);
			this.getOfflinePlayer().getPlayer().setCustomName(displayName);
		}
	}

	public boolean updateFlagData(String flag, boolean value, String server, long expires) throws SQLException {
		String sqlFormat = expires > 0 ? TimeUtil.SQL_FORMAT.format(new Date(expires)) : null;
		return NiftyChat.getSQL().update(StringUtil.format("INSERT INTO {0} (uuid, flag, _value, server, _expires) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE uuid = ?, _value = ?, _expires = ?;", Config.USER_FLAGS_TABLE), this.getProfile().getUniqueId(), flag, value, server, sqlFormat, this.getProfile().getUniqueId(), value, sqlFormat);
	}

	public void updateTabListName() {
		//if (this.isOnlineLocally()) {
		try {
			String displayName = this.getDisplayName(true);
			if (displayName.length() > 16) displayName = displayName.substring(0, 16);
			this.getOfflinePlayer().getPlayer().setPlayerListName(displayName);
		} catch (Exception ex) {
			this.getLog().console("LOLWAT 2: {0}:{1}", this.getProfile(), this.isOnlineLocally());
		}
		//}
	}

}