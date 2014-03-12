package net.netcoding.niftychat.listeners;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import net.netcoding.niftybukkit.database.DatabaseListener;
import net.netcoding.niftybukkit.database.DatabaseNotification;
import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.database.TriggerEvent;
import net.netcoding.niftychat.managers.Cache;
import net.netcoding.niftychat.managers.CensorData;
import net.netcoding.niftychat.managers.RankData;
import net.netcoding.niftychat.managers.UserData;

public class Notifications implements DatabaseListener {

	@Override
	public void onDatabaseNotification(DatabaseNotification databaseNotification) {
		try {
			switch (databaseNotification.getTable()) {
			case "nc_censor":
				CensorData.reload();
				break;
			case "nc_ranks":
				if (databaseNotification.getEvent() == TriggerEvent.DELETE) {
					HashMap<String, Object> data = databaseNotification.getDeletedData();
					CensorData.removeCache(String.valueOf(data.get("rank")));
				} else {
					databaseNotification.getUpdatedRow(new ResultCallback<Void>() {
						@Override
						public Void handle(ResultSet result) throws SQLException {
							if (result.next()) {
								String rank = result.getString("rank");
								RankData rankData = RankData.getCache(rank);
								rankData.setGroup(result.getString("group"));
								rankData.setPrefix(result.getString("prefix"));
								rankData.setSuffix(result.getString("suffix"));
								rankData.setFormat(result.getString("format"));

								for (String playerName : UserData.getCachedPlayers()) {
									UserData userData = UserData.getCache(playerName);

									if (userData.getPrimaryRank() == rank) {
										userData.updateDisplayName();
										userData.updateTabListName();
									}
								}
							}

							return null;
						}
					});
				}
				break;
			case "nc_user_ranks":
				if (databaseNotification.getEvent() != TriggerEvent.DELETE) {
					databaseNotification.getUpdatedRow(new ResultCallback<Void>() {
						@Override
						public Void handle(ResultSet result) throws SQLException {
							if (result.next()) {
								int userId = result.getInt("user_id");

								Cache.MySQL.query("SELECT * FROM `nc_users` WHERE `id` = ?", new ResultCallback<Void>() {
									@Override
									public Void handle(ResultSet result) throws SQLException {
										if (result.next()) {
											UserData userData = UserData.getCache(result.getString("user"));

											if (userData != null) { // Online
												userData.updateRanks();
												userData.updateDisplayName();
												userData.updateTabListName();
												userData.updateVaultRanks();
											}
										}

										return null;
									}
								}, userId);
							}

							return null;
						}
					});
				}
				break;
			case "nc_users":
				databaseNotification.getUpdatedRow(new ResultCallback<Void>() {
					@Override
					public Void handle(ResultSet result) throws SQLException {
						if (result.next()) {
							UserData userData = UserData.getCache(result.getString("user"));

							if (userData != null) {
								userData.updateDisplayName();
								userData.updateTabListName();
							}
						}

						return null;
					}
				});
				break;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

}