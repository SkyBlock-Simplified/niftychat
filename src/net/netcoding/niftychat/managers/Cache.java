package net.netcoding.niftychat.managers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.permission.Permission;
import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.database.MySQL;
import net.netcoding.niftybukkit.ghosts.GhostBusters;
import net.netcoding.niftybukkit.minecraft.Log;
import net.netcoding.niftybukkit.utilities.CIHashMap;

public class Cache {

	public static transient GhostBusters ghosts;
	public static transient MySQL MySQL;
	public static transient Permission permissions;
	public static transient Log Log;
	public static final CIHashMap<RankData>   rankData = new CIHashMap<>();
	public static final CIHashMap<UserData>   userData = new CIHashMap<>();
	public static final Map<String, CompiledCensor> censorList = new HashMap<>();

	public Cache(JavaPlugin plugin) {
		Log = new Log(plugin);
		ghosts = new GhostBusters(plugin);

		if ((permissions = NiftyBukkit.getPermissions()) != null)
			Log.console("Connected to Vault");

		Log.console("Loading MySQL");
		FileConfiguration config = plugin.getConfig();
		MySQL = new MySQL(config.getString("host"), config.getInt("port"),
				config.getString("schema"), config.getString("user"),
				config.getString("pass"));
	}

}