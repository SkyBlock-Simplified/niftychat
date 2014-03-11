package net.netcoding.niftychat.managers;

import java.util.HashMap;
import java.util.Map;

import net.milkbowl.vault.permission.Permission;
import net.netcoding.niftybukkit.database.MySQL;
import net.netcoding.niftybukkit.ghosts.GhostBusters;
import net.netcoding.niftybukkit.util.concurrent.ConcurrentIHashMap;

public class Cache {

	public static transient GhostBusters ghosts;
	public static transient MySQL MySQL;
	public static transient Permission permissions;
	public static final ConcurrentIHashMap<RankData>   rankData = new ConcurrentIHashMap<>();
	public static final ConcurrentIHashMap<UserData>   userData = new ConcurrentIHashMap<>();
	public static final Map<String, CompiledCensor> censorList = new HashMap<>();

}