package net.netcoding.niftychat.managers;

import java.util.regex.Pattern;

public class CompiledCensor {

	public static final String DEFAULT_REPLACE = "***";

	private Pattern pattern;
	private String replace;

	public CompiledCensor(String badword) {
		this(badword, DEFAULT_REPLACE);
	}

	public CompiledCensor(String badword, String replace) {
		this.pattern = Pattern.compile(String.format("(?i)\\b(%1$s)\\b", badword.replaceAll("%", "[\\\\S-]*")));
		this.replace = (replace == null ? DEFAULT_REPLACE : replace);
	}

	public Pattern getPattern() {
		return this.pattern;
	}

	public String getReplace() {
		return this.replace;
	}

	public void setReplace(String replace) {
		this.replace = replace;
	}

}