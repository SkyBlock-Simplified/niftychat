package net.netcoding.niftychat.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.minecraft.BungeeHelper;
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

	private String lastMessage;

	private MojangProfile lastMessenger;

	private boolean hasMoved = false;

	private List<UserFlagData> flagData;

	public UserChatData(JavaPlugin plugin, Player player) {
		this(plugin, NiftyBukkit.getMojangRepository().searchByExactPlayer(player));
		cache.add(this);
	}

	public UserChatData(JavaPlugin plugin, MojangProfile profile) {
		super(plugin);
		this.profile = profile;
	}

	public void addFlagData(UserFlagData flagData) {
		this.flagData.add(flagData);
	}

	public static ConcurrentSet<UserChatData> getCache() {
		return cache;
	}

	public static UserChatData getCache(UUID uuid) {
		for (UserChatData data : cache) {
			if (uuid.equals(data.getUniqueId()))
				return data;
		}

		return null;
	}

	public String getDisplayName() {
		return this.getDisplayName(false);
	}

	public String getDisplayName(boolean fetch) {
		UserChatData userData = getCache(this.getUniqueId());

		if (!fetch && userData != null) {
			if (userData.getPlayer() != null)
				return userData.getPlayer().getDisplayName();
		}

		try {
			return _getDisplayName(this.profile);
		} catch (SQLException ex) { }

		return this.getName();
	}

	private static String _getDisplayName(final MojangProfile profile) throws SQLException {
		return Cache.MySQL.query(StringUtil.format("SELECT * FROM `{0}` WHERE `uuid` = ?;", Config.USER_TABLE), new ResultCallback<String>() {
			@Override
			public String handle(ResultSet result) throws SQLException {
				String displayName = profile.getName();
				String rank = "default";

				if (result.next()) {
					displayName = profile.getName();
					rank = UserRankData.getOfflineRanks(profile).get(0);
					Player player = findPlayer(displayName);
					if (player != null) displayName = player.getName();
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
		if (this.flagData == null) this.reloadFlagData();
		List<UserFlagData> flagMatches = new ArrayList<>();

		for (UserFlagData flagData : this.flagData) {
			if (flagData.getFlag().equalsIgnoreCase(flag))
				flagMatches.add(flagData);
		}

		return flagMatches;
	}

	public UserFlagData getFlagData(String flag) {
		List<UserFlagData> flagDatas = this.getAllFlagData(flag);
		BungeeHelper bungeeHelper = NiftyBukkit.getBungeeHelper();
		UserFlagData found = new UserFlagData(flag);

		for (UserFlagData flagData : flagDatas) {
			if (flagData.getValue()) {
				if (flagData.isGlobal()) {
					found = flagData;
					break;
				} else if (bungeeHelper.isOnline()) {
					if (bungeeHelper.getServer().equals(flagData.getServer())) {
						found = flagData;
						break;
					}
				}
			}
		}

		return found;
	}

	public String getName() {
		return this.profile.getName();
	}

	public MojangProfile getLastMessenger() {
		return this.lastMessenger;
	}

	public Player getPlayer() {
		return BukkitHelper.findPlayer(this.getName());
	}

	public UserRankData getRankData() {
		return UserRankData.getCache(this.profile.getUniqueId());
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

	public boolean isMuted() {
		return this.getFlagData("muted").getValue();
	}

	public boolean isVanished() {
		return this.getFlagData("vanished").getValue();
	}

	public static void removeCache(UUID uuid) {
		for (UserChatData data : cache) {
			if (data.getUniqueId().equals(uuid))
				cache.remove(data);
		}
	}

	public void reloadFlagData() {
		try {
			if (this.flagData == null) this.flagData = new ArrayList<>();
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
			}, this.getUniqueId()));
		} catch (SQLException ex) {
			this.getLog().console(ex);
		}
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
		this.getPlayer().setDisplayName(displayName);
		this.getPlayer().setCustomName(displayName);
	}

	public void updateTabListName() {
		String displayName = this.getDisplayName(true);
		if (displayName.length() > 16) displayName = displayName.substring(0, 16);
		this.getPlayer().setPlayerListName(displayName);
	}

}