package net.netcoding.nifty.chat.cache;

import net.netcoding.nifty.chat.NiftyChat;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.core.util.concurrent.Concurrent;
import net.netcoding.nifty.core.util.concurrent.ConcurrentList;

import java.util.regex.Pattern;

public class CensorData {

	private static final transient ConcurrentList<CensorData> CACHE = Concurrent.newList();
	public static final transient String DEFAULT_REPLACE = "***";
	private final String badword;
	private final Pattern pattern;
	private String replace;
	private boolean enabled = true;

	public CensorData(String badword) {
		this(badword, DEFAULT_REPLACE);
	}

	public CensorData(String badword, String replace) {
		this.badword = badword;
		this.pattern = Pattern.compile(StringUtil.format("(?i)\\b{0}\\b", badword.replaceAll("%", "[\\\\S-]*")));
		this.replace = (replace == null ? DEFAULT_REPLACE : replace);
		CACHE.add(this);
	}

	public String getBadword() {
		return this.badword;
	}

	public static ConcurrentList<CensorData> getCache() {
		return CACHE;
	}

	public static CensorData getCache(String badword) {
		for (CensorData data : getCache()) {
			if (data.getBadword().equalsIgnoreCase(badword))
				return data;
		}

		return null;
	}

	public Pattern getPattern() {
		return this.pattern;
	}

	public String getReplace() {
		return this.replace;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public static void reload() {
		CACHE.clear();

		NiftyChat.getSQL().queryAsync(StringUtil.format("SELECT * FROM {0};", Config.CENSOR_TABLE), result -> {
			while (result.next()) {
				String badword = result.getString("badword");
				String replace = result.getString("_replace");
				replace = (result.wasNull() ? null : replace);
				new CensorData(badword, replace);
			}
		});
	}

	public static void removeCache(String badword) {
		getCache().stream().filter(censor -> censor.getBadword().equalsIgnoreCase(badword)).forEach(CACHE::remove);
	}

	public void setEnabled(boolean value) {
		this.enabled = value;
	}

	public void setReplace(String replace) {
		this.replace = replace;
	}

}