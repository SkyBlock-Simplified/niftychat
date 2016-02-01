package net.netcoding.niftychat.cache;

import net.netcoding.niftychat.NiftyChat;
import net.netcoding.niftycore.database.factory.callbacks.VoidResultCallback;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftycore.util.concurrent.ConcurrentList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class CensorData {

	private static final transient ConcurrentList<CensorData> CACHE = new ConcurrentList<>();
	public static final transient String DEFAULT_REPLACE = "***";
	private String badword;
	private Pattern pattern;
	private String replace;
	private boolean enabled = true;

	public CensorData(String badword) {
		this(badword, DEFAULT_REPLACE);
	}

	public CensorData(String badword, String replace) {
		this.badword = badword;
		this.pattern = Pattern.compile(StringUtil.format("(?i)\\b({0})\\b", badword.replaceAll("%", "[\\\\S-]*")));
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

	public static void reload() throws SQLException {
		CACHE.clear();

		NiftyChat.getSQL().queryAsync(StringUtil.format("SELECT * FROM {0};", Config.CENSOR_TABLE), new VoidResultCallback() {
			@Override
			public void handle(ResultSet result) throws SQLException {
				while (result.next()) {
					String badword = result.getString("badword");
					String replace = result.getString("_replace");
					replace = (result.wasNull() ? null : replace);
					new CensorData(badword, replace);
				}
			}
		});
	}

	public static void removeCache(String badword) {
		for (CensorData censor : getCache()) {
			if (censor.getBadword().equalsIgnoreCase(badword))
				CACHE.remove(censor);
		}
	}

	public void setEnabled(boolean value) {
		this.enabled = value;
	}

	public void setReplace(String replace) {
		this.replace = replace;
	}

}