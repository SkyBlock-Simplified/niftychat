package net.netcoding.nifty.chat.cache;

import net.netcoding.nifty.chat.NiftyChat;
import net.netcoding.nifty.core.util.RegexUtil;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.core.util.concurrent.Concurrent;
import net.netcoding.nifty.core.util.concurrent.ConcurrentSet;

public class RankFormat {

	private static final transient ConcurrentSet<RankFormat> CACHE = Concurrent.newSet();
	public static final transient RankFormat DEFAULT = new RankFormat("default", "&7default", "{displayname} &8>&r {msg}", "&7", "", "&7", false);
	private final String rank;
	private String group;
	private String format;
	private String prefix;
	private String suffix;
	private String message;

	public RankFormat(String rank, String group, String format) {
		this(rank, group, format, "", "", "");
	}

	public RankFormat(String rank, String group, String format, String prefix, String suffix, String message) {
		this(rank, group, format, prefix, suffix, message, true);
	}

	private RankFormat(String rank, String group, String format, String prefix, String suffix, String message, boolean addToCache) {
		this.rank = rank;
		this.setGroup(group);
		this.setPrefix(prefix);
		this.setSuffix(suffix);
		this.setMessage(message);
		this.setFormat(format);

		if (addToCache)
			CACHE.add(this);
	}

	public static ConcurrentSet<RankFormat> getCache() {
		return CACHE;
	}

	public static RankFormat getCache(String rank) {
		RankFormat _default = DEFAULT;

		for (RankFormat data : getCache()) {
			if (data.getRank().equalsIgnoreCase(DEFAULT.getRank()))
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
		CACHE.clear();

		NiftyChat.getSQL().queryAsync(StringUtil.format("SELECT * FROM {0};", Config.FORMAT_TABLE), result -> {
			while (result.next()) {
				String rank = result.getString("rank");
				String group = result.getString("_group");
				String prefix = result.getString("_prefix");
				if (result.wasNull()) prefix = "";
				String suffix = result.getString("_suffix");
				if (result.wasNull()) suffix = "";
				String message = result.getString("_message");
				if (result.wasNull()) message= "";
				String format = result.getString("_format");
				if (result.wasNull()) format = "";

				new RankFormat(rank, group, format, prefix, suffix, message);
			}
		});
	}

	public static void removeCache(String rank) {
		getCache().stream().filter(data -> data.getRank().equals(rank)).forEach(CACHE::remove);
	}

	public void setGroup(String value) {
		this.group = StringUtil.isEmpty(value) ? this.rank : RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN);
	}

	public void setFormat(String value) {
		if (StringUtil.isEmpty(value))
			value = DEFAULT.getFormat();

		try {
			value = RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN);
		} catch (Exception ex) {
			value = RegexUtil.replaceColor(DEFAULT.getFormat(), RegexUtil.REPLACE_ALL_PATTERN);
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