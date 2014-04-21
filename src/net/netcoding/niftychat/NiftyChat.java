package net.netcoding.niftychat;

import net.netcoding.niftybukkit.database.MySQL;
import net.netcoding.niftybukkit.minecraft.BukkitPlugin;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.CensorData;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.RankFormat;
import net.netcoding.niftychat.commands.Censor;
import net.netcoding.niftychat.commands.Format;
import net.netcoding.niftychat.commands.Nick;
import net.netcoding.niftychat.commands.Realname;
import net.netcoding.niftychat.listeners.Chat;
import net.netcoding.niftychat.listeners.Disconnect;
import net.netcoding.niftychat.listeners.Login;
import net.netcoding.niftychat.listeners.Move;
import net.netcoding.niftychat.listeners.Notifications;
import net.netcoding.niftyranks.commands.Rank;

public class NiftyChat extends BukkitPlugin {

	@Override
	public void onEnable() {
		this.getLog().console("Loading Config");
		Cache.Config = new Config(this);
		Cache.Config.init();

		this.getLog().console("Loading MySQL");
		Cache.MySQL = new MySQL(Cache.Config.getHost(), Cache.Config.getPort(),
				Cache.Config.getUser(), Cache.Config.getPass(), Cache.Config.getSchema());

		if (Cache.MySQL.testConnection())
			Cache.MySQL.setAutoReconnect();
		else {
			this.getLog().console("Invalid MySQL Configuration!");
			this.setEnabled(false);
			return;
		}

		this.getLog().console("Updating MySQL Tables & Data");
		if (!this.setupTables()) return;

		try {
			Cache.notifications = new Notifications();
			Cache.MySQL.addDatabaseListener(Config.FORMAT_TABLE, Cache.notifications);
			Cache.MySQL.addDatabaseListener(Config.CENSOR_TABLE, Cache.notifications);
			Cache.MySQL.addDatabaseListener(Config.USER_TABLE, Cache.notifications);
		} catch (Exception ex) {
			this.getLog().console(ex);
			return;
		}

		this.getLog().console("Registering Commands");
		new Censor(this);
		new Format(this);
		//new Mute(this);
		new Nick(this);
		new Rank(this);
		new Realname(this);

		this.getLog().console("Registering Listeners");
		//Cache.ghosts = new GhostBusters(this);
		new Chat(this);
		new Disconnect(this);
		new Login(this);
		new Move(this);

		this.getLog().console("Loading Censor List");
		CensorData.reload();

		this.getLog().console("Loading Rank Formats");
		RankFormat.reload();
	}

	@Override
	public void onDisable() {
		Cache.MySQL.stopListening();
	}

	private boolean setupTables() {
		//   user: muted, nick-revoked
		// server: chat-disabled, no-player-list
		try {
			Cache.MySQL.createTable(Config.FORMAT_TABLE, "`rank` VARCHAR(50) NOT NULL UNIQUE, `group` VARCHAR(255), `prefix` VARCHAR(255), `suffix` VARCHAR(255), `format` VARCHAR(255)");
			Cache.MySQL.createTable(Config.CENSOR_TABLE, "`badword` VARCHAR(255) NOT NULL UNIQUE, `replace` VARCHAR(255)");
			Cache.MySQL.createTable(Config.USER_TABLE, "`uuid` VARCHAR(64) NOT NULL UNIQUE, `nick` VARCHAR(255), `ufnick` VARCHAR(16) UNIQUE");
			//Cache.MySQL.createTable("nifty_server_flags", "`server_name` VARCHAR(50) NOT NULL, `flag` VARCHAR(50) NOT NULL UNIQUE, PRIMARY KEY (`server_name`, `flag`)");
			//Cache.MySQL.createTable("nifty_user_flags",   "`uuid` VARCHAR(64) NOT NULL, `flag` VARCHAR(50) NOT NULL UNIQUE, PRIMARY KEY (`uuid`, `flag`), FOREIGN KEY (`uuid`) REFERENCES `nifty_users_chat`(`uuid`) ON DELETE CASCADE");
			return true;
		} catch (Exception ex) {
			this.getLog().console(ex);
			return false;
		}
	}

}