package net.netcoding.niftychat;

import java.sql.SQLException;

import net.netcoding.niftybukkit.database.MySQL;
import net.netcoding.niftybukkit.minecraft.BukkitPlugin;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.cache.Cache;
import net.netcoding.niftychat.cache.CensorData;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.RankFormat;
import net.netcoding.niftychat.commands.Censor;
import net.netcoding.niftychat.commands.Format;
import net.netcoding.niftychat.commands.GList;
import net.netcoding.niftychat.commands.Message;
import net.netcoding.niftychat.commands.Mute;
import net.netcoding.niftychat.commands.Nick;
import net.netcoding.niftychat.commands.Realname;
import net.netcoding.niftychat.commands.SocialSpy;
import net.netcoding.niftychat.commands.Vanish;
import net.netcoding.niftychat.listeners.Chat;
import net.netcoding.niftychat.listeners.Disconnect;
import net.netcoding.niftychat.listeners.Login;
import net.netcoding.niftychat.listeners.Misc;
import net.netcoding.niftychat.listeners.Notifications;

public class NiftyChat extends BukkitPlugin {

	@Override
	public void onEnable() {
		this.getLog().console("Loading Config");
		Cache.Config = new Config(this);
		Cache.Config.init();

		this.getLog().console("Loading MySQL");
		try {
			Cache.MySQL = new MySQL(Cache.Config.getHost(), Cache.Config.getPort(),
					Cache.Config.getUser(), Cache.Config.getPass(), Cache.Config.getSchema());
		} catch (SQLException ex) {
			this.getLog().console("Invalid MySQL Configuration!", ex);
			this.setEnabled(false);
			return;
		}

		this.getLog().console("Updating MySQL Tables & Data");
		if (!this.setupTables()) {
			this.getLog().console("Unable to setup MySQL Tables & Data!");
			this.setEnabled(false);
			return;
		}

		try {
			Cache.notifications = new Notifications();
			Cache.MySQL.addListener(Config.FORMAT_TABLE, Cache.notifications);
			Cache.MySQL.addListener(Config.CENSOR_TABLE, Cache.notifications);
			Cache.MySQL.addListener(Config.USER_TABLE, Cache.notifications);
			Cache.MySQL.addListener(Config.USER_FLAGS_TABLE, Cache.notifications);
		} catch (Exception ex) {
			this.getLog().console(ex);
			this.setEnabled(false);
			return;
		}

		this.getLog().console("Registering Commands");
		new Censor(this);
		new Format(this);
		new GList(this);
		new Message(this);
		new Mute(this);
		new Nick(this);
		new Realname(this);
		new Vanish(this);
		new SocialSpy(this);

		this.getLog().console("Registering Listeners");
		new Chat(this);
		new Disconnect(this);
		new Login(this);
		new Misc(this);

		this.getLog().console("Loading Censor List");
		CensorData.reload();

		this.getLog().console("Loading Rank Formats");
		RankFormat.reload();
	}

	@Override
	public void onDisable() {
		Cache.MySQL.removeListeners();
	}

	private boolean setupTables() {
		try {
			Cache.MySQL.createTable(Config.FORMAT_TABLE, "`rank` VARCHAR(50) NOT NULL PRIMARY KEY, `group` VARCHAR(255), `prefix` VARCHAR(255), `suffix` VARCHAR(255), `message` VARCHAR(50), `format` VARCHAR(255)");
			Cache.MySQL.createTable(Config.CENSOR_TABLE, "`badword` VARCHAR(255) NOT NULL PRIMARY KEY, `replace` VARCHAR(255)");
			Cache.MySQL.createTable(Config.USER_TABLE, "`uuid` VARCHAR(37) NOT NULL PRIMARY KEY, `nick` VARCHAR(255), `ufnick` VARCHAR(16) UNIQUE");
			Cache.MySQL.createTable(Config.USER_FLAGS_TABLE, StringUtil.format("`uuid` VARCHAR(37) NOT NULL, `flag` VARCHAR(50) NOT NULL, `value` BIT(1) NOT NULL, `server` VARCHAR(100) NOT NULL, `_submitted` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `_expires` TIMESTAMP NULL, PRIMARY KEY (`uuid`, `flag`, `server`), FOREIGN KEY (`uuid`) REFERENCES `{0}`(`uuid`) ON DELETE CASCADE", Config.USER_TABLE));
			Cache.MySQL.createTable(Config.SERVER_FLAGS_TABLE, "`server` VARCHAR(100) NOT NULL, `flag` VARCHAR(50) NOT NULL, `value` BIT(1) NOT NULL, PRIMARY KEY (`server`, `flag`)");
			Cache.MySQL.update(StringUtil.format("INSERT IGNORE INTO `{0}` (`rank`, `prefix`, `message`, `format`) VALUES (?, ?, ?, ?);", Config.FORMAT_TABLE), "default", "&7", "&7", "{displayname} &8>&r {msg}");
			Cache.MySQL.update(StringUtil.format("INSERT IGNORE INTO `{0}` (`rank`, `message`, `format`) VALUES (?, ?, ?);", Config.FORMAT_TABLE), "message", "&7", "{sender} &8->&r {receiver} &8>&r {pmsg}");
			return true;
		} catch (Exception ex) {
			this.getLog().console(ex);
			return false;
		}
	}

}