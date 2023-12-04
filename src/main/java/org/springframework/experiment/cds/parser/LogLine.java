package org.springframework.experiment.cds.parser;

import java.util.List;

/**
 * A line of JVM log.
 *
 * @author Stephane Nicoll
 */
record LogLine(List<String> tags, String message) {

	/**
	 * Specify if the log matches the specified tags. All specified tags must match.
	 * @param tags the tags to check
	 * @return true if this instance contains the specified tags
	 */
	boolean containTags(String... tags) {
		for (String tag : tags) {
			if (!this.tags.contains(tag)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Parse a line of log.
	 * @param line the line
	 * @return a {@code LogLin} instance.
	 */
	static LogLine parse(String line) {
		int tagStart = line.lastIndexOf("[");
		int tagEnd = line.indexOf("]", tagStart);
		if (tagStart == -1 || tagEnd == -1) {
			throw new IllegalArgumentException("Tag delimiter not found in " + line);
		}
		String[] tags = line.substring(tagStart + 1, tagEnd).split(",");
		String msg = line.substring(tagEnd + 1).trim();
		return new LogLine(List.of(tags), msg);
	}

}
