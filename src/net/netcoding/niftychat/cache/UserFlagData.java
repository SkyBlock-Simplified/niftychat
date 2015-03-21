package net.netcoding.niftychat.cache;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BungeeServer;
import net.netcoding.niftybukkit.util.StringUtil;

public class UserFlagData {

	private long expires = 0;

	private final String flag;

	private final String server;

	private long submitted = System.currentTimeMillis();

	private boolean value = false;

	public UserFlagData(String flag, String server) {
		this.flag = flag;
		this.server = StringUtil.isEmpty(server) ? "*" : server;
	}

	public String getFlag() {
		return this.flag;
	}

	public long getExpires() {
		return this.expires;
	}

	public BungeeServer getServer() {
		return NiftyBukkit.getBungeeHelper().getServer(this.server);
	}

	public String getServerName() {
		return this.server;
	}

	public long getSubmitted() {
		return this.submitted;
	}

	public boolean getValue() {
		return this.value ? (this.hasExpiry() ? (this.expires >= System.currentTimeMillis()) : this.value) : this.value;
	}

	public boolean hasExpiry() {
		return this.expires != 0;
	}

	public boolean isGlobal() {
		return this.server.equals("*");
	}

	public void setExpires(long expires) {
		this.expires = expires;
	}

	public void setValue(boolean value) {
		this.value = value;
	}

	public void setSubmitted(long submitted) {
		this.submitted = submitted;
	}

}