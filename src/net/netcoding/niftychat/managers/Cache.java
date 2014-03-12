package net.netcoding.niftychat.managers;

import net.milkbowl.vault.permission.Permission;
import net.netcoding.niftybukkit.database.MySQL;
import net.netcoding.niftybukkit.ghosts.GhostBusters;
import net.netcoding.niftychat.listeners.Notifications;

public class Cache {

	public static transient GhostBusters ghosts;
	public static transient MySQL MySQL;
	public static transient Permission permissions;
	public static transient Notifications notifications;

}