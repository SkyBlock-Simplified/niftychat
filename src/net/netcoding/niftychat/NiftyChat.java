package net.netcoding.niftychat;

import static net.netcoding.niftychat.managers.Cache.Log;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.netcoding.niftybukkit.database.*;
import net.netcoding.niftychat.commands.*;
import net.netcoding.niftychat.listeners.*;
import net.netcoding.niftychat.managers.*;

import org.bukkit.plugin.java.JavaPlugin;

public class NiftyChat extends JavaPlugin implements DatabaseListener {

	@Override
	public void onEnable() {
		// commit test
		this.saveDefaultConfig();
		this.saveConfig();
		new Cache(this);

		if (!Cache.MySQL.testConnection()) {
			Log.console("Invalid MySQL Configuration!");
			this.setEnabled(false);
			return;
		}

		Log.console("Updating MySQL Tables & Data");
		if (!this.setupTables()) return;

		try {
			Cache.MySQL.addDatabaseListener("nc_censor", this);
			Cache.MySQL.addDatabaseListener("nc_ranks", this);
			Cache.MySQL.addDatabaseListener("nc_users", this);
			Cache.MySQL.addDatabaseListener("nc_user_ranks", this);
		} catch (Exception ex) {
			Log.console(ex);
			return;
		}

		Log.console("Registering Commands");
		new Censor(this);
		new Format(this);
		//new Mute(this);
		new Nick(this);
		new Rank(this);
		new Realname(this);

		Log.console("Registering Event Listeners");
		new Chat(this);
		new Disconnect(this);
		new Login(this);
		new Move(this);

		Log.console("Loading Censor List");
		this.loadCensorList();

		Log.console("Loading Rank Formats");
		this.loadFormats();
	}

	@Override
	public void onDisable() {
		Cache.MySQL.stopListening();
	}

	@Override
	public void onDatabaseNotification(DatabaseNotification notification) {
		switch (notification.getTable()) {
		case "nc_censor":
			this.loadCensorList();
			break;
		case "nc_ranks":
			if (notification.getEvent() != TriggerEvent.UPDATE)
				this.loadFormats();
			else {
				try {
					notification.getUpdatedRow(new ResultSetCallbackNR() {
						@Override
						public void handleResult(ResultSet result) throws SQLException, Exception {
							if (result.next()) {
								String rank = result.getString("rank");
								RankData rankData = Cache.rankData.get(rank);
								rankData.setGroup(result.getString("group"));
								rankData.setPrefix(result.getString("prefix"));
								rankData.setSuffix(result.getString("suffix"));
								rankData.setFormat(result.getString("format"));

								for (String playerName : Cache.userData.keySet()) {
									UserData userData = Cache.userData.get(playerName);

									if (userData.getPrimaryRank() == rank) {
										userData.updateDisplayName();
										userData.updateTabListName();
									}
								}

								//User.updateTabListNames();
							}
						}
					});
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			break;
		case "nc_user_ranks":
			try {
				if (notification.getEvent() != TriggerEvent.DELETE) {
					notification.getUpdatedRow(new ResultSetCallbackNR() {
						@Override
						public void handleResult(ResultSet result) throws SQLException, Exception {
							if (result.next()) {
								int userId = result.getInt("user_id");

								Cache.MySQL.query("SELECT * FROM `nc_users` WHERE `id` = ?", new ResultSetCallbackNR() {
									@Override
									public void handleResult(ResultSet result) throws SQLException, Exception {
										if (result.next()) {
											UserData userData = Cache.userData.get(result.getString("user"));
											
											if (userData != null) { // Online
												userData.updateRanks();
												userData.updateDisplayName();
												userData.updateTabListName();
												userData.updateVaultRanks();
											}
										}
									}
								}, userId);
							}
						}
					});
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			break;
		case "nc_users":
			try {
				notification.getUpdatedRow(new ResultSetCallbackNR() {
					@Override
					public void handleResult(ResultSet result) throws SQLException, Exception {
						if (result.next()) {
							UserData userData = Cache.userData.get(result.getString("user"));

							if (userData != null) {
								userData.updateDisplayName();
								userData.updateTabListName();
							}
						}
					}
				});
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			break;
		}
	}

	private void loadCensorList() {
		try {
			Cache.censorList.clear();

			Cache.MySQL.query("SELECT * FROM `nc_censor`;", new ResultSetCallback() {
				@Override
				public Object handleResult(ResultSet result) {
					try {
						while (result.next()) {
							String badword = result.getString("badword");
							String replace = result.getString("replace");
							replace        = (result.wasNull() ? null : replace);
							Cache.censorList.put(badword, new CompiledCensor(badword, replace));
						}

						return true;
					} catch (SQLException ex) {
						ex.printStackTrace();
					}

					return false;
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void loadFormats() {
		try {
			Cache.rankData.clear();

			Cache.MySQL.query("SELECT * FROM `nc_ranks`;", new ResultSetCallback() {
				@Override
				public Object handleResult(ResultSet result) throws SQLException {
					while (result.next()) {
						String rank = result.getString("rank");
						String group = result.getString("group");
						String prefix = result.getString("prefix");
						if ("".equals(prefix)) prefix = null;
						String suffix = result.getString("suffix");
						if ("".equals(suffix)) suffix = null;
						String format = result.getString("format");
						if (result.wasNull()) format = null;

						RankData rankInfo = new RankData(group, format, prefix, suffix);
						Cache.rankData.put(rank, rankInfo);
					}

					return true;
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private boolean setupTables() {
		//   user: muted, nick-revoked
		// server: chat-disabled, no-player-list
		try {
			Cache.MySQL.createTable("nc_ranks",        "`id` INT AUTO_INCREMENT PRIMARY KEY, `rank` VARCHAR(50) NOT NULL UNIQUE, `group` VARCHAR(50), `prefix` VARCHAR(255), `suffix` VARCHAR(255), `format` VARCHAR(255)");
			Cache.MySQL.createTable("nc_users",        "`id` INT AUTO_INCREMENT PRIMARY KEY, `user` VARCHAR(16) NOT NULL UNIQUE, `nick` VARCHAR(255), `ufnick` VARCHAR(16) UNIQUE");
			Cache.MySQL.createTable("nc_censor",       "`id` INT AUTO_INCREMENT PRIMARY KEY, `badword` VARCHAR(255) NOT NULL UNIQUE, `replace` VARCHAR(255)");
			Cache.MySQL.createTable("nc_server_flags", "`server_name` VARCHAR(50) NOT NULL, `flag` VARCHAR(50) NOT NULL UNIQUE, PRIMARY KEY (`server_name`, `flag`)");
			Cache.MySQL.createTable("nc_user_flags",   "`user_id` INT NOT NULL, `flag` VARCHAR(50) NOT NULL UNIQUE, PRIMARY KEY (`user_id`, `flag`), FOREIGN KEY (`user_id`) REFERENCES `nc_users`(`id`) ON DELETE CASCADE");
			Cache.MySQL.createTable("nc_user_ranks",   "`user_id` INT NOT NULL, `rank_id` INT NOT NULL, PRIMARY KEY (`user_id`, `rank_id`), FOREIGN KEY (`user_id`) REFERENCES `nc_users`(`id`) ON DELETE CASCADE, FOREIGN KEY (`rank_id`) REFERENCES `nc_ranks`(`id`) ON DELETE CASCADE");
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

}