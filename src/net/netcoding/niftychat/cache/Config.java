package net.netcoding.niftychat.cache;

import net.netcoding.niftybukkit.yaml.annotations.Path;

import org.bukkit.plugin.java.JavaPlugin;

public class Config extends net.netcoding.niftybukkit.yaml.Config {

	private static final transient String TABLE_PREFIX = "niftychat_";
	public static final transient String USER_TABLE = TABLE_PREFIX + "users";
	public static final transient String CENSOR_TABLE = TABLE_PREFIX + "censors";
	public static final transient String FORMAT_TABLE = TABLE_PREFIX + "formats";
	public static final transient String USER_FLAGS_TABLE = TABLE_PREFIX + "flags_users";
	public static final transient String SERVER_FLAGS_TABLE = TABLE_PREFIX + "flags_servers";

	@Path("mysql.host")
	private String hostname = "localhost";

	@Path("mysql.user")
	private String username = "minecraft";

	@Path("mysql.pass")
	private String password = "";

	@Path("mysql.port")
	private int port = 3306;

	@Path("mysql.schema")
	private String schema = "nifty";

	public Config(JavaPlugin plugin) {
		super(plugin, "config");
	}

	public String getHost() {
		return this.hostname;
	}

	public String getUser() {
		return this.username;
	}

	public String getPass() {
		return this.password;
	}

	public int getPort() {
		return this.port;
	}

	public String getSchema() {
		return this.schema;
	}

}
