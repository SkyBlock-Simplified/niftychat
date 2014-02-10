package net.netcoding.niftychat.managers;

import net.netcoding.niftybukkit.utilities.RegexUtil;

public class RankData {

	private String group;
	private String format;
	private String prefix;
	private String suffix;

	public RankData(String group, String format) {
		this(group, format, "", "");
	}

	public RankData(String group, String format, String prefix, String suffix) {
		this.setGroup(group);
		this.setFormat(format);
		this.setPrefix(prefix);
		this.setSuffix(suffix);
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

	public String getSuffix() {
		return this.suffix;
	}

	public void setGroup(String value) {
		this.group = value;
	}

	public void setFormat(String value) {
		String _default = "{displayname}&f: &7{msg}";
		if (value == null) value = _default;

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
		this.prefix = (value == null ? "" : RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN));
	}

	public void setSuffix(String value) {
		this.suffix = (value == null ? "" : RegexUtil.replaceColor(value, RegexUtil.REPLACE_ALL_PATTERN));
	}

}