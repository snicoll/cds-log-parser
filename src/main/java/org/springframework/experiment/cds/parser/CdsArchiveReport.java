package org.springframework.experiment.cds.parser;

import java.util.List;

import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Stephane Nicoll
 */
public class CdsArchiveReport {

	/**
	 * A curated reason to indicate a class was skipped because one of its interface is
	 * excluded.
	 */
	public static final String INTERFACE_EXCLUDED = "interface is excluded";

	/**
	 * A curated reason to indicate a class was skipped because its super class is
	 * excluded.
	 */
	public static final String SUPER_CLASS_EXCLUDED = "super class is excluded";

	private final MultiValueMap<String, String> skipped;

	private final long total;

	CdsArchiveReport(MultiValueMap<String, String> skipped) {
		this.skipped = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(skipped));
		this.total = this.skipped.values().stream().map(List::size).reduce(0, Integer::sum);
	}

	/**
	 * Return the number of classes that were skipped from the archive.
	 * @return the number of excluded classes
	 */
	public long getSkippedCount() {
		return this.total;
	}

	/**
	 * Return the classes that were excluded, mapped by reasons.
	 * @return a mapping of class names by reason for exclusion
	 */
	public MultiValueMap<String, String> getSkipped() {
		return this.skipped;
	}

}
