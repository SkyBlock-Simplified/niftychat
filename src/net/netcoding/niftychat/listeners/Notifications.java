package net.netcoding.niftychat.listeners;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.netcoding.niftybukkit.database.DatabaseListener;
import net.netcoding.niftybukkit.database.DatabaseNotification;
import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.database.TriggerEvent;
import net.netcoding.niftychat.cache.CensorData;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.RankFormat;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.cache.UserFlagData;

public class Notifications implements DatabaseListener {

	@Override
	public void onDatabaseNotification(DatabaseNotification databaseNotification) throws SQLException {
		TriggerEvent event = databaseNotification.getEvent();
		String table = databaseNotification.getTable();

		if (table.equals(Config.USER_TABLE)) {
			if (event.equals(TriggerEvent.UPDATE)) {
				databaseNotification.getUpdatedRow(new ResultCallback<Void>() {
					@Override
					public Void handle(ResultSet result) throws SQLException {
						if (result.next()) {
							UserChatData userData = UserChatData.getCache(UUID.fromString(result.getString("uuid")));

							if (userData != null) {
								userData.updateDisplayName();
								userData.updateTabListName();
							}
						}

						return null;
					}
				});
			}
		} else if (table.equals(Config.FORMAT_TABLE)) {
			if (event.equals(TriggerEvent.DELETE)) {
				Map<String, Object> data = databaseNotification.getDeletedData();
				RankFormat.removeCache((String)data.get("rank"));
			} else if (event.equals(TriggerEvent.INSERT)) {
				databaseNotification.getUpdatedRow(new ResultCallback<Void>() {
					@Override
					public Void handle(ResultSet result) throws SQLException {
						if (result.next()) {
							String rank = result.getString("rank");
							new RankFormat(rank, result.getString("group"), result.getString("format"));
						}

						return null;
					}
				});
			} else if (event.equals(TriggerEvent.UPDATE)) {
				databaseNotification.getUpdatedRow(new ResultCallback<Void>() {
					@Override
					public Void handle(ResultSet result) throws SQLException {
						if (result.next()) {
							String rank = result.getString("rank");
							RankFormat rankData = RankFormat.getCache(rank);
							rankData.setFormat(result.getString("format"));
							rankData.setGroup(result.getString("group"));
							rankData.setPrefix(result.getString("prefix"));
							rankData.setSuffix(result.getString("suffix"));

							for (UserChatData userData : UserChatData.getCache()) {
								if (userData.getRankData().getPrimaryRank().equalsIgnoreCase(rank)) {
									userData.updateDisplayName();
									userData.updateTabListName();
								}
							}
						}

						return null;
					}
				});
			}
		} else if (table.equals(Config.CENSOR_TABLE)) {
			CensorData.reload();
		} else if (table.equals(Config.USER_FLAGS_TABLE)) {
			if (!event.equals(TriggerEvent.INSERT)) {
				Map<String, Object> data = databaseNotification.getDeletedData();
				UserChatData userData = UserChatData.getCache(UUID.fromString((String)data.get("uuid")));
				if (userData != null) userData.reloadFlagData();
			}

			if (!event.equals(TriggerEvent.DELETE)) {
				databaseNotification.getUpdatedRow(new ResultCallback<Void>() {
					@Override
					public Void handle(ResultSet result) throws SQLException {
						if (result.next()) {
							UserChatData userData = UserChatData.getCache(UUID.fromString(result.getString("uuid")));

							if (userData != null) {
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

								if (flagMatch == null) userData.addFlagData(flagMatch = new UserFlagData(flag));
								flagMatch.setExpires(_expires);
								flagMatch.setServer(server);
								flagMatch.setSubmitted(_submitted);
								flagMatch.setValue(result.getBoolean("value"));
							}
						}

						return null;
					}
				});
			}
		}
	}

}