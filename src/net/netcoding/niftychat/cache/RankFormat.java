package net.netcoding.niftychat.cache;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.netcoding.niftybukkit.database.ResultCallback;
import net.netcoding.niftybukkit.util.RegexUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftybukkit.util.concurrent.ConcurrentSet;
import net.netcoding.niftyranks.cache.Cache;

public class RankFormat {

	private static final transient ConcurrentSet<RankFormat> cache = new ConcurrentSet<>();
	private String rank;
	private String group;
	private String format;
	private String prefix;
	private String suffix;

	public RankFormat(String rank, String group, String format) {
		this(rank, group, format, "", "");
	}

	public RankFormat(String rank, String group, String format, String prefix, String suffix) {
		this.rank = rank;
		this.setGroup(group);
		this.setFormat(format);
		this.setPrefix(prefix);
		this.setSuffix(suffix);
		cache.add(this);
	}

	public static ConcurrentSet<RankFormat> getCache() {
		return cache;
	}

	public static RankFormat getCache(String rank) {
		RankFormat _default = null;

		for (RankFormat data : cache) {
			if (data.getRank().equalsIgnoreCase("default"))
				_default = data;

			if (data.getRank().equalsIgnoreCase(rank))
				return data;
		}

		return _default;
	}

	public String getGroup() {
		return this.group;
	}

	public String getFormat() {
		return this.format;
	}

	public String getPrefix() {
		return this.prefix;
	}

	public String getRank() {
		return this.rank;
	}

	public String getSuffix() {
		return this.suffix;
	}

	public static void reload() {
		try {
			cache.clear();

			Cache.MySQL.query(StringUtil.format("SELECT * FROM `{0}`;", Config.FORMAT_TABLE), new ResultCallback<Void>() {
				@Override
				public Void handle(ResultSet result) throws SQLException {
					while (result.next()) {
						String rank = result.getString("rank");
						String group = result.getString("group");
						String prefix = result.getString("prefix");
						if (StringUtil.isEmpty(prefix)) prefix = null;
						String suffix = result.getString("suffix");
						if (StringUtil.isEmpty(suffix)) suffix = null;
						String format = result.getString("format");
						if (result.wasNull()) format = null;

						new RankFormat(rank, group, format, prefix, suffix);
					}

					return null;
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void removeCache(String rank) {
		for (RankFormat data : cache) {
			if (data.getRank().equals(rank))
				cache.remove(data);
		}
	}

	public void setGroup(String value) {
		this.group = StringUtil.isEmpty(value) ? this.rank : value;
	}

	public void setFormat(String value) {
		String _default = "{displayname}&f: &7{msg}";
		if (StringUtil.isEmpty(value)) value = _default;

		try {
			value = RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN);
		} catch (Exception ex) {
			value = RegexUtil.replaceColor(_default, RegexUtil.REPLACE_ALL_PATTERN);
		}

		value = value.replace("{displayname}", "%1$s");
		value = value.replace("{msg}", "%2$s");
		value = value.replace("{group}", "{0}");
		value = value.replace("{world}", "{1}");
		value = value.replace("{worldletter}", "{2}");
		value = value.replace("{teamprefix}", "{3}");
		value = value.replace("{teamsuffix}", "{4}");
		value = value.replace("{teamname}", "{5}");

		this.format = value;
	}

	public void setPrefix(String value) {
		this.prefix = StringUtil.isEmpty(value) ? "" : RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN);
	}

	public void setSuffix(String value) {
		this.suffix = StringUtil.isEmpty(value) ? "" : RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN);
	}

}