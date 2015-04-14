package net.netcoding.niftychat;

import net.netcoding.niftybukkit.database.factory.SQLWrapper;
import net.netcoding.niftybukkit.minecraft.BukkitPlugin;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftychat.cache.CensorData;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.RankFormat;
import net.netcoding.niftychat.commands.Broadcast;
import net.netcoding.niftychat.commands.Censor;
import net.netcoding.niftychat.commands.Format;
import net.netcoding.niftychat.commands.GList;
import net.netcoding.niftychat.commands.Me;
import net.netcoding.niftychat.commands.Message;
import net.netcoding.niftychat.commands.Mute;
import net.netcoding.niftychat.commands.Nick;
import net.netcoding.niftychat.commands.Realname;
import net.netcoding.niftychat.commands.SocialSpy;
import net.netcoding.niftychat.commands.Vanish;
import net.netcoding.niftychat.commands.Whois;
import net.netcoding.niftychat.listeners.Chat;
import net.netcoding.niftychat.listeners.Disconnect;
import net.netcoding.niftychat.listeners.Login;
import net.netcoding.niftychat.listeners.Misc;
import net.netcoding.niftychat.listeners.Notifications;

public class NiftyChat extends BukkitPlugin {

	private static transient Config pluginConfig;

	@Override
	public void onEnable() {
		this.getLog().console("Loading SQL Config");
		try {
			(pluginConfig = new Config(this)).init();

			if (pluginConfig.getSQL() == null) {
				this.getLog().console("Incomplete MySQL Configuration!");
				this.setEnabled(false);
				return;
			}
		} catch (Exception ex) {
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
			Notifications notifications = new Notifications();
			getSQL().addListener(Config.FORMAT_TABLE, notifications);
			getSQL().addListener(Config.CENSOR_TABLE, notifications);
			getSQL().addListener(Config.USER_TABLE, notifications);
			getSQL().addListener(Config.USER_FLAGS_TABLE, notifications);
		} catch (Exception ex) {
			this.getLog().console(ex);
			this.setEnabled(false);
			return;
		}

		this.getLog().console("Registering Commands");
		new Broadcast(this);
		new Censor(this);
		new Format(this);
		new GList(this);
		new Me(this);
		new Message(this);
		new Mute(this);
		new Nick(this);
		new Realname(this);
		new Vanish(this);
		new SocialSpy(this);
		new Whois(this);

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
		if (getSQL() != null)
			getSQL().removeListeners();
	}

	public final static SQLWrapper getSQL() {
		return pluginConfig.getSQL();
	}

	private boolean setupTables() {
		try {
			getSQL().createTable(Config.FORMAT_TABLE, "rank VARCHAR(50) NOT NULL PRIMARY KEY, _group VARCHAR(255), _prefix VARCHAR(255), _suffix VARCHAR(255), _message VARCHAR(50), _format VARCHAR(255)");
			getSQL().createTable(Config.CENSOR_TABLE, "badword VARCHAR(255) NOT NULL PRIMARY KEY, _replace VARCHAR(255)");
			getSQL().createTable(Config.USER_TABLE, "uuid VARCHAR(37) NOT NULL PRIMARY KEY, nick VARCHAR(255), ufnick VARCHAR(16) UNIQUE");
			getSQL().createTable(Config.USER_FLAGS_TABLE, StringUtil.format("uuid VARCHAR(37) NOT NULL, flag VARCHAR(50) NOT NULL, _value BIT(1) NOT NULL, server VARCHAR(100) NOT NULL, _submitted TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, _expires TIMESTAMP NULL, PRIMARY KEY (uuid, flag, server), FOREIGN KEY (uuid) REFERENCES {0}(uuid) ON DELETE CASCADE", Config.USER_TABLE));
			getSQL().createTable(Config.SERVER_FLAGS_TABLE, "server VARCHAR(100) NOT NULL, flag VARCHAR(50) NOT NULL, _value BIT(1) NOT NULL, PRIMARY KEY (server, flag)");
			getSQL().updateAsync(StringUtil.format("INSERT IGNORE INTO {0} (rank, _prefix, message, format) VALUES (?, ?, ?, ?);", Config.FORMAT_TABLE), "default", "&7", "&7", "{displayname} &8>&r {msg}");
			getSQL().updateAsync(StringUtil.format("INSERT IGNORE INTO {0} (rank, message, format) VALUES (?, ?, ?);", Config.FORMAT_TABLE), "message", "&7", "{sender} &8->&r {receiver} &8>&r {pmsg}");

			if (getSQL().checkColumnExists(Config.CENSOR_TABLE, "replace")) {
				getSQL().updateAsync(StringUtil.format("ALTER TABLE {0} CHANGE `replace` _replace VARCHAR(255);", Config.CENSOR_TABLE));
				getSQL().updateAsync(StringUtil.format("ALTER TABLE {0} CHANGE `value` _value BIT(1) NOT NULL;", Config.USER_FLAGS_TABLE));
				getSQL().updateAsync(StringUtil.format("ALTER TABLE {0} CHANGE `value` _value BIT(1) NOT NULL;", Config.SERVER_FLAGS_TABLE));
				getSQL().updateAsync(StringUtil.format("ALTER TABLE {0} CHANGE `group` _group VARCHAR(255);", Config.FORMAT_TABLE));
				getSQL().updateAsync(StringUtil.format("ALTER TABLE {0} CHANGE `prefix` _prefix VARCHAR(255);", Config.FORMAT_TABLE));
				getSQL().updateAsync(StringUtil.format("ALTER TABLE {0} CHANGE `suffix` _suffix VARCHAR(255);", Config.FORMAT_TABLE));
				getSQL().updateAsync(StringUtil.format("ALTER TABLE {0} CHANGE `message` _message VARCHAR(50);", Config.FORMAT_TABLE));
				getSQL().updateAsync(StringUtil.format("ALTER TABLE {0} CHANGE `format` _format VARCHAR(255);", Config.FORMAT_TABLE));
			}

			return true;
		} catch (Exception ex) {
			this.getLog().console(ex);
			return false;
		}
	}

}