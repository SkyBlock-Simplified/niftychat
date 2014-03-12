package net.netcoding.niftychat.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.util.concurrent.ConcurrentSet;

public class CensorData {

	private static final ConcurrentSet<CensorData> cache = new ConcurrentSet<>();
	public static final String DEFAULT_REPLACE = "***";
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
	}

	public String getBadword() {
		return this.badword;
	}

	public static CensorData getCache(String badword) {
		if ("".equals(badword)) return null;

		for (CensorData censor : cache) {
			if (censor.getBadword().equalsIgnoreCase(badword))
				return censor;
		}

		return null;
	}

	public static ConcurrentSet<CensorData> getCache() {
		return cache;
	}

	public Pattern getPattern() {
		return this.pattern;
	}

	public String getReplace() {
		return this.replace;
	}

	public static void reload() {
		try {
			cache.clear();

			Cache.MySQL.query("SELECT * FROM `nc_censor`;", new ResultCallback<Void>() {
				@Override
				public Void handle(ResultSet result) throws SQLException {
					while (result.next()) {
						String badword = result.getString("badword");
						String replace = result.getString("replace");
						replace        = (result.wasNull() ? null : replace);
						new CensorData(badword, replace);
					}

					return null;
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void removeCache(String badword) {
		for (CensorData censor : cache) {
			if (censor.getBadword().equalsIgnoreCase(badword))
				cache.remove(censor);
		}
	}

	public void setReplace(String replace) {
		this.replace = replace;
	}

}