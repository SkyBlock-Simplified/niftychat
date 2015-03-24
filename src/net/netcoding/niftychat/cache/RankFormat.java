package net.netcoding.niftychat.cache;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.netcoding.niftybukkit.database.factory.AsyncResultCallback;
import net.netcoding.niftybukkit.util.RegexUtil;
import net.netcoding.niftybukkit.util.StringUtil;
import net.netcoding.niftybukkit.util.concurrent.ConcurrentSet;
import net.netcoding.niftychat.NiftyChat;

public class RankFormat {

	private static final transient ConcurrentSet<RankFormat> CACHE = new ConcurrentSet<>();
	private String rank;
	private String group;
	private String format;
	private String prefix;
	private String suffix;
	private String message;

	public RankFormat(String rank, String group, String format) {
		this(rank, group, format, "", "", "");
	}

	public RankFormat(String rank, String group, String format, String prefix, String suffix, String message) {
		this.rank = rank;
		this.setGroup(group);
		this.setMessage(message);
		this.setPrefix(prefix);
		this.setSuffix(suffix);
		this.setFormat(format);
		CACHE.add(this);
	}

	public static ConcurrentSet<RankFormat> getCache() {
		return CACHE;
	}

	public static RankFormat getCache(String rank) {
		RankFormat _default = null;

		for (RankFormat data : getCache()) {
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

	public String getMessage() {
		return this.message;
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
			CACHE.clear();

			NiftyChat.getSQL().queryAsync(StringUtil.format("SELECT * FROM `{0}`;", Config.FORMAT_TABLE), new AsyncResultCallback() {
				@Override
				public void handle(ResultSet result) throws SQLException {
					while (result.next()) {
						String rank = result.getString("rank");
						String group = result.getString("group");
						String prefix = result.getString("prefix");
						if (StringUtil.isEmpty(prefix)) prefix = null;
						String suffix = result.getString("suffix");
						if (StringUtil.isEmpty(suffix)) suffix = null;
						String message = result.getString("message");
						if (StringUtil.isEmpty(message)) message = null;
						String format = result.getString("format");
						if (result.wasNull()) format = null;

						new RankFormat(rank, group, format, prefix, suffix, message);
					}
				}
			});
		} catch (Exception ex) { }
	}

	public static void removeCache(String rank) {
		for (RankFormat data : getCache()) {
			if (data.getRank().equals(rank))
				CACHE.remove(data);
		}
	}

	public void setGroup(String value) {
		this.group = StringUtil.isEmpty(value) ? this.rank : RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN);
	}

	public void setFormat(String value) {
		String _default = "{displayname} &8>&r {msg}";
		if (StringUtil.isEmpty(value)) value = _default;

		try {
			value = RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN);
		} catch (Exception ex) {
			value = RegexUtil.replaceColor(_default, RegexUtil.REPLACE_ALL_PATTERN);
		}

		// Standard
		value = value.replace("{displayname}", "%1$s");
		value = value.replace("{name}", "%1$s");
		value = value.replace("{d}", "%1$s");
		value = value.replace("{message}", (this.getMessage() + "%2$s"));
		value = value.replace("{msg}", (this.getMessage() + "%2$s"));
		value = value.replace("{m}", (this.getMessage() + "%2$s"));
		value = value.replace("{group}", "{0}");
		value = value.replace("{g}", "{0}");
		value = value.replace("{world}", "{1}");
		value = value.replace("{w}", "{1}");
		value = value.replace("{worldletter}", "{2}");
		value = value.replace("{wl}", "{2}");
		value = value.replace("{team}", "{3}");
		value = value.replace("{t}", "{3}");
		value = value.replace("{teamprefix}", "{4}");
		value = value.replace("{tp}", "{4}");
		value = value.replace("{teamsuffix}", "{5}");
		value = value.replace("{ts}", "{5}");
		value = value.replaceAll("[ ]{2,}", " ");

		// Private
		value = value.replace("{sender}", "{0}");
		value = value.replace("{s}", "{0}");
		value = value.replace("{receiver}", "{1}");
		value = value.replace("{r}", "{1}");
		value = value.replace("{privatemessage}", (this.getMessage() + "{2}"));
		value = value.replace("{privatemsg}", (this.getMessage() + "{2}"));
		value = value.replace("{pmessage}", (this.getMessage() + "{2}"));
		value = value.replace("{message}", (this.getMessage() + "{2}"));
		value = value.replace("{pmsg}", (this.getMessage() + "{2}"));
		value = value.replace("{msg}", (this.getMessage() + "{2}"));

		this.format = value;
	}

	public void setMessage(String value) {
		this.message = StringUtil.isEmpty(value) ? "" : RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN);
	}

	public void setPrefix(String value) {
		this.prefix = StringUtil.isEmpty(value) ? "" : RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN);
	}

	public void setSuffix(String value) {
		this.suffix = StringUtil.isEmpty(value) ? "" : RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN);
	}

}