package net.netcoding.nifty.chat.cache;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.yaml.BukkitSQLConfig;
import net.netcoding.nifty.core.database.MySQL;
import net.netcoding.nifty.core.yaml.ConfigSection;
import net.netcoding.nifty.core.yaml.exceptions.InvalidConfigurationException;

public class Config extends BukkitSQLConfig<MySQL> {

	public static final String CHAT_CHANNEL = "NiftyChat";
	private static final String TABLE_PREFIX = CHAT_CHANNEL.toLowerCase() + "_";
	public static final String USER_TABLE = TABLE_PREFIX + "users";
	public static final String CENSOR_TABLE = TABLE_PREFIX + "censors";
	public static final String FORMAT_TABLE = TABLE_PREFIX + "formats";
	public static final String USER_FLAGS_TABLE = TABLE_PREFIX + "flags_users";
	public static final String SERVER_FLAGS_TABLE = TABLE_PREFIX + "flags_servers";

	public Config(MinecraftPlugin plugin) {
		super(plugin.getDataFolder(), "config");
	}

	public static boolean isForcedCommand(String alias) {
		return alias.matches("^(g|global)?un[\\w]+");
	}

	public static boolean isGlobalCommand(String alias, String server) {
		return alias.matches("^g(lobal)?(un)?[\\w]+") || server.matches("^global|all|\\*$");
	}

	public static String getServerNameFromArgs(String[] args, boolean check) {
		String server = "*";

		if (Nifty.getBungeeHelper().getDetails().isDetected()) {
			server = Nifty.getBungeeHelper().getServerName();

			if (check) {
				if (Nifty.getBungeeHelper().getServer(args[args.length - 1]) != null)
					server = args[args.length - 1];
			}
		}

		return server;
	}

	@Override
	public boolean update(ConfigSection section) throws InvalidConfigurationException {
		if (section.has("mysql")) {
			ConfigSection mysql = section.get("mysql");
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
