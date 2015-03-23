package net.netcoding.niftychat.cache;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.MySQL;
import net.netcoding.niftybukkit.yaml.ConfigSection;
import net.netcoding.niftybukkit.yaml.SQLConfig;
import net.netcoding.niftybukkit.yaml.exceptions.InvalidConfigurationException;

import org.bukkit.plugin.java.JavaPlugin;

public class Config extends SQLConfig<MySQL> {

	public static final transient String CHAT_CHANNEL = "NiftyChat";
	private static final transient String TABLE_PREFIX = CHAT_CHANNEL.toLowerCase() + "_";
	public static final transient String USER_TABLE = TABLE_PREFIX + "users";
	public static final transient String CENSOR_TABLE = TABLE_PREFIX + "censors";
	public static final transient String FORMAT_TABLE = TABLE_PREFIX + "formats";
	public static final transient String USER_FLAGS_TABLE = TABLE_PREFIX + "flags_users";
	public static final transient String SERVER_FLAGS_TABLE = TABLE_PREFIX + "flags_servers";

	public Config(JavaPlugin plugin) throws Exception {
		super(plugin, "config");
	}

	public static boolean isForcedCommand(String alias) {
		return alias.matches("^(g|global)?un[\\w]+");
	}

	public static boolean isGlobalCommand(String alias, String server) {
		return alias.matches("^g(lobal)?(un)?[\\w]+") || server.matches("^global|all|\\*$");
	}

	public static String getServerNameFromArgs(String[] args, boolean check) {
		String server = "*";

		if (NiftyBukkit.getBungeeHelper().isDetected()) {
			server = NiftyBukkit.getBungeeHelper().getServerName();

			if (check) {
				if (NiftyBukkit.getBungeeHelper().getServer(args[args.length - 1]) != null)
					server = args[args.length - 1];
			}
		}

		return server;
	}

	@Override
	public boolean update(ConfigSection section) throws InvalidConfigurationException {
		if (section.has("mysql")) {
			ConfigSection mysql = (ConfigSection)section.get("mysql");
			section.remove("mysql");
			this.driver = "mysql";
			this.hostname = mysql.get("host");
			this.username = mysql.get("user");
			this.password = mysql.get("pass");
			this.port = mysql.get("port");
			this.schema = mysql.get("schema");

			return true;
		}

		return false;
	}

}
