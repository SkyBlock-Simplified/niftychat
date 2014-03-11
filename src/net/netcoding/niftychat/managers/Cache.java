package net.netcoding.niftychat.managers;

import java.util.HashMap;
import java.util.Map;

import net.milkbowl.vault.permission.Permission;
import net.netcoding.niftybukkit.database.MySQL;
import net.netcoding.niftybukkit.ghosts.GhostBusters;

public class Cache {

	public static transient GhostBusters ghosts;
	public static transient MySQL MySQL;
	public static transient Permission permissions;
	public static final Map<String, CompiledCensor> censorList = new HashMap<>();

}