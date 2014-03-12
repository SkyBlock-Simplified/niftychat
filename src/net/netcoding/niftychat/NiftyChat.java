package net.netcoding.niftychat;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.MySQL;
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
import net.netcoding.niftychat.listeners.Notifications;
import net.netcoding.niftychat.managers.Cache;
import net.netcoding.niftychat.managers.CensorData;
import net.netcoding.niftychat.managers.RankData;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class NiftyChat extends JavaPlugin {

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
			Cache.notifications = new Notifications();
			Cache.MySQL.addDatabaseListener("nc_censor", Cache.notifications);
			Cache.MySQL.addDatabaseListener("nc_ranks", Cache.notifications);
			Cache.MySQL.addDatabaseListener("nc_users", Cache.notifications);
			Cache.MySQL.addDatabaseListener("nc_user_ranks", Cache.notifications);
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
		CensorData.reload();

		log.console("Loading Rank Formats");
		RankData.reload();
	}

	@Override
	public void onDisable() {
		Cache.MySQL.stopListening();
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