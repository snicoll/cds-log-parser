package org.springframework.experiment.appcds.parser;

import java.util.List;

import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Report of class loading.
 *
 * @author Stephane Nicoll
 */
public class ClassLoadingReport {

	/**
	 * The location for dynamic generated lambdas.
	 */
	public static final String DYNAMIC_GENERATED_LAMBDA = "__JVM_LookupDefineClass__";

	/**
	 * The location for dynamic proxies.
	 */
	public static final String DYNAMIC_PROXY = "__dynamic_proxy__";

	/**
	 * TODO.
	 */
	public static final String CLASS_DEFINER = "__ClassDefiner__";


	private final List<String> hits;

	private final MultiValueMap<String, String> misses;

	private final long total;

	ClassLoadingReport(List<String> hits, MultiValueMap<String, String> misses) {
		this.hits = List.copyOf(hits);
		this.misses = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(misses));
		this.total = hits.size() + misses.values().stream().map(List::size).reduce(0, Integer::sum);
	}

	/**
	 * Return the class names that were loaded from the cache.
	 * @return the hits
	 */
	public List<String> getHits() {
		return this.hits;
	}

	/**
	 * Return the classes names that were not loaded from the cache, mapped by location.
	 * @return a map from location to class names that were not loaded from the cache
	 */
	public MultiValueMap<String, String> getMisses() {
		return this.misses;
	}

	/**
	 * Return the total number of classes that were loaded.
	 * @return the classes loaded count
	 */
	public long getLoadCount() {
		return this.total;
	}

	/**
	 * Returns the ratio of classes that were loaded from the cache.
	 * @return the hit rate
	 */
	public float getHitRate() {
		return ((float) this.hits.size() / (float) this.total);
	}

	/**
	 * Returns the ratio of classes that were loaded from the classpath.
	 * @return the miss rate
	 */
	public float getMissRate() {
		return 1 - getHitRate();
	}

}
