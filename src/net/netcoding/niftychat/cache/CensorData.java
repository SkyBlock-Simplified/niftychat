package net.netcoding.niftychat.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import net.netcoding.niftybukkit.database.factory.AsyncResultCallback;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftybukkit.util.concurrent.ConcurrentSet;
import net.netcoding.niftychat.NiftyChat;

public class CensorData {

	private static final transient ConcurrentSet<CensorData> CACHE = new ConcurrentSet<>();
	public static final transient String DEFAULT_REPLACE = "***";
	private String badword;
	private Pattern pattern;
	private String replace;

	public CensorData(String badword) {
		this(badword, DEFAULT_REPLACE);
	}

	public CensorData(String badword, String replace) {
		this.badword = badword;
		this.pattern = Pattern.compile(String.format("(?i)\\b(%1$s)\\b", badword.replaceAll("%", "[\\\\S-]*")));
		this.replace = (replace == null ? DEFAULT_REPLACE : replace);
		CACHE.add(this);
	}

	public String getBadword() {
		return this.badword;
	}

	public static ConcurrentSet<CensorData> getCache() {
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

	public static void reload() {
		try {
			CACHE.clear();

			NiftyChat.getSQL().queryAsync(StringUtil.format("SELECT * FROM `{0}`;", Config.CENSOR_TABLE), new AsyncResultCallback() {
				@Override
				public void handle(ResultSet result) throws SQLException {
					while (result.next()) {
						String badword = result.getString("badword");
						String replace = result.getString("replace");
						replace = (result.wasNull() ? null : replace);
						new CensorData(badword, replace);
					}
				}
			});
		} catch (Exception ex) { }
	}

	public static void removeCache(String badword) {
		for (CensorData censor : getCache()) {
			if (censor.getBadword().equalsIgnoreCase(badword))
				CACHE.remove(censor);
		}
	}

	public void setReplace(String replace) {
		this.replace = replace;
	}

}