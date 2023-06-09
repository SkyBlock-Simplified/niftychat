package net.netcoding.nifty.chat.listeners;

import net.netcoding.nifty.chat.cache.UserFlagData;
import net.netcoding.nifty.core.database.notifications.DatabaseListener;
import net.netcoding.nifty.core.database.notifications.DatabaseNotification;
import net.netcoding.nifty.core.database.notifications.TriggerEvent;
import net.netcoding.nifty.chat.cache.CensorData;
import net.netcoding.nifty.chat.cache.Config;
import net.netcoding.nifty.chat.cache.RankFormat;
import net.netcoding.nifty.chat.cache.UserChatData;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Notifications implements DatabaseListener {

	@Override
	public void onDatabaseNotification(final DatabaseNotification databaseNotification) throws SQLException {
		TriggerEvent event = databaseNotification.getEvent();
		String table = databaseNotification.getTable();

		if (table.equals(Config.USER_TABLE)) {
			if (event == TriggerEvent.UPDATE) {
				databaseNotification.getUpdatedRow(result -> {
					if (result.next()) {
						UUID uuid = UUID.fromString(result.getString("uuid"));

						for (UserChatData userData : UserChatData.getCache()) {
							if (userData.getProfile().getUniqueId().equals(uuid)) {
								userData.updateDisplayName();
								userData.updateTabListName();
								break;
							}
						}
					}
				});
			}
		} else if (table.equals(Config.FORMAT_TABLE)) {
			if (event == TriggerEvent.DELETE) {
				Map<String, Object> data = databaseNotification.getDeletedData();
				RankFormat.removeCache((String)data.get("rank"));
			} else if (event == TriggerEvent.INSERT) {
				databaseNotification.getUpdatedRow(result -> {
					if (result.next()) {
						String rank = result.getString("rank");
						new RankFormat(rank, result.getString("_group"), result.getString("_format"));
					}
				});
			} else if (event == TriggerEvent.UPDATE) {
				databaseNotification.getUpdatedRow(result -> {
					if (result.next()) {
						String rank = result.getString("rank");
						RankFormat rankData = RankFormat.getCache(rank);
						rankData.setGroup(result.getString("_group"));
						rankData.setPrefix(result.getString("_prefix"));
						rankData.setSuffix(result.getString("_suffix"));
						rankData.setMessage(result.getString("_message"));
						rankData.setFormat(result.getString("_format"));

						UserChatData.getCache().stream().filter(userData -> userData.getRankData().getPrimaryRank().equalsIgnoreCase(rank)).forEach(userData -> {
							userData.updateDisplayName();
							userData.updateTabListName();
						});
					}
				});
			}
		} else if (table.equals(Config.CENSOR_TABLE))
			CensorData.reload();
		else if (table.equals(Config.USER_FLAGS_TABLE)) {
			if (event != TriggerEvent.INSERT) {
				Map<String, Object> data = databaseNotification.getDeletedData();
				UUID uuid = UUID.fromString((String)data.get("uuid"));

				UserChatData.getCache().stream().filter(userData -> userData.getProfile().getUniqueId().equals(uuid)).forEach(userData -> {
					userData.reloadFlagData();
					userData.applyFlagData((String) data.get("flag"));
				});
			}

			if (event != TriggerEvent.DELETE) {
				databaseNotification.getUpdatedRow(result -> {
					if (result.next()) {
						UUID uuid = UUID.fromString(result.getString("uuid"));

						for (UserChatData userData : UserChatData.getCache()) {
							if (userData.getProfile().getUniqueId().equals(uuid)) {
								String flag = result.getString("flag");
								List<UserFlagData> flagDatas = userData.getAllFlagData(flag);
								String server = result.getString("server");
								long _submitted = result.getTimestamp("_submitted").getTime();
								Timestamp expires = result.getTimestamp("_expires");
								long _expires = result.wasNull() ? 0 : expires.getTime();
								UserFlagData flagMatch = null;

								for (UserFlagData flagData : flagDatas) {
									if (flagData.isGlobal() && server.equals("*")) {
										flagMatch = flagData;
										break;
									} else if (!flagData.isGlobal() && flagData.getServerName().equals(server)) {
										flagMatch = flagData;
										break;
									}
								}

								if (flagMatch == null) userData.addFlagData(flagMatch = new UserFlagData(flag, server));
								flagMatch.setExpires(_expires);
								flagMatch.setSubmitted(_submitted);
								flagMatch.setValue(result.getBoolean("_value"));
								userData.applyFlagData(flag);
								break;
							}
						}
					}
				});
			}
		}
	}

}