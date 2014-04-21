package net.netcoding.niftychat.listeners;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import net.netcoding.niftybukkit.database.DatabaseListener;
import net.netcoding.niftybukkit.database.DatabaseNotification;
import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.database.TriggerEvent;
import net.netcoding.niftychat.cache.CensorData;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.RankFormat;
import net.netcoding.niftychat.cache.UserChatData;

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
				//HashMap<String, Object> data = databaseNotification.getDeletedData();
				//CensorData.removeCache(String.valueOf(data.get("rank")));
			} else if (event.equals(TriggerEvent.INSERT)) {
				databaseNotification.getUpdatedRow(new ResultCallback<Void>() {
					@Override
					public Void handle(ResultSet result) throws SQLException {
						if (result.next()) {
							String rank = result.getString("rank");
							new RankFormat(rank, result.getString("group"), result.getString("format"));
							//result.getString("suffix");
							//result.getString("format");
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
							rankData.setGroup(result.getString("group"));
							rankData.setPrefix(result.getString("prefix"));
							rankData.setSuffix(result.getString("suffix"));
							rankData.setFormat(result.getString("format"));

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
		}
	}

}