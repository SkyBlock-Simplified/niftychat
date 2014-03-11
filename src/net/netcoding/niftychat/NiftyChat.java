package net.netcoding.niftychat;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.DatabaseListener;
import net.netcoding.niftybukkit.database.DatabaseNotification;
import net.netcoding.niftybukkit.database.MySQL;
import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.database.TriggerEvent;
import net.netcoding.niftybukkit.ghosts.GhostBusters;
import net.netcoding.niftybukkit.minecraft.Log;
import net.netcoding.niftychat.commands.Censor;
import net.netcoding.niftychat.commands.Format;
import net.netcoding.niftychat.commands.Nick;
import net.netcoding.niftychat.commands.Rank;
import net.netcoding.niftychat.commands.Realname;
import net.netcoding.niftychat.listeners.Chat;
import net.netcoding.niftychat.listeners.Disconnect;
import net.netcoding.niftychat.listeners.Login;
import net.netcoding.niftychat.listeners.Move;
import net.netcoding.niftychat.managers.Cache;
import net.netcoding.niftychat.managers.CompiledCensor;
import net.netcoding.niftychat.managers.RankData;
import net.netcoding.niftychat.managers.UserData;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class NiftyChat extends JavaPlugin implements DatabaseListener {

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		this.saveConfig();
		Log log = new Log(this);
		Cache.ghosts = new GhostBusters(this);

		if ((Cache.permissions = NiftyBukkit.getPermissions()) != null)
			log.console("Connected to Vault");

		log.console("Loading MySQL");
		FileConfiguration config = this.getConfig();
		Cache.MySQL = new MySQL(config.getString("host"), config.getInt("port"),
				config.getString("schema"), config.getString("user"),
				config.getString("pass"));

		if (Cache.MySQL.testConnection())
			Cache.MySQL.setAutoReconnect();
		else {
			log.console("Invalid MySQL Configuration!");
			this.setEnabled(false);
			return;
		}

		log.console("Updating MySQL Tables & Data");
		if (!this.setupTables()) return;

		try {
			Cache.MySQL.addDatabaseListener("nc_censor", this);
			Cache.MySQL.addDatabaseListener("nc_ranks", this);
			Cache.MySQL.addDatabaseListener("nc_users", this);
			Cache.MySQL.addDatabaseListener("nc_user_ranks", this);
		} catch (Exception ex) {
			log.console(ex);
			return;
		}

		log.console("Registering Commands");
		new Censor(this);
		new Format(this);
		//new Mute(this);
		new Nick(this);
		new Rank(this);
		new Realname(this);

		log.console("Registering Event Listeners");
		new Chat(this);
		new Disconnect(this);
		new Login(this);
		new Move(this);

		log.console("Loading Censor List");
		this.loadCensorList();

		log.console("Loading Rank Formats");
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
					notification.getUpdatedRow(new ResultCallback<Void>() {
						@Override
						public Void handle(ResultSet result) throws SQLException {
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

							return null;
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
					notification.getUpdatedRow(new ResultCallback<Void>() {
						@Override
						public Void handle(ResultSet result) throws SQLException {
							if (result.next()) {
								int userId = result.getInt("user_id");

								Cache.MySQL.query("SELECT * FROM `nc_users` WHERE `id` = ?", new ResultCallback<Void>() {
									@Override
									public Void handle(ResultSet result) throws SQLException {
										if (result.next()) {
											UserData userData = Cache.userData.get(result.getString("user"));
											
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
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			break;
		case "nc_users":
			try {
				notification.getUpdatedRow(new ResultCallback<Void>() {
					@Override
					public Void handle(ResultSet result) throws SQLException {
						if (result.next()) {
							UserData userData = Cache.userData.get(result.getString("user"));

							if (userData != null) {
								userData.updateDisplayName();
								userData.updateTabListName();
							}
						}

						return null;
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

			Cache.MySQL.query("SELECT * FROM `nc_censor`;", new ResultCallback<Void>() {
				@Override
				public Void handle(ResultSet result) throws SQLException {
					while (result.next()) {
						String badword = result.getString("badword");
						String replace = result.getString("replace");
						replace        = (result.wasNull() ? null : replace);
						Cache.censorList.put(badword, new CompiledCensor(badword, replace));
					}
					
					return null;
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void loadFormats() {
		try {
			Cache.rankData.clear();

			Cache.MySQL.query("SELECT * FROM `nc_ranks`;", new ResultCallback<Void>() {
				@Override
				public Void handle(ResultSet result) throws SQLException {
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

					return null;
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