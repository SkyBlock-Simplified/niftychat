package net.netcoding.nifty.chat.cache;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.messaging.BungeeServer;
import net.netcoding.nifty.core.util.StringUtil;

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

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof UserChatData)) return false;
		if (this == obj) return true;
		UserFlagData flagData = (UserFlagData)obj;

		if (this.getFlag().equalsIgnoreCase(flagData.getFlag())) {
			if (this.getServerName().equalsIgnoreCase(flagData.getServerName()))
				return true;
		}

		return false;
	}

	public String getFlag() {
		return this.flag;
	}

	public long getExpires() {
		return this.expires;
	}

	public BungeeServer getServer() {
		return Nifty.getBungeeHelper().getServer(this.server);
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

	@Override
	public int hashCode() {
		return this.getServerName().hashCode();
	}

}