package org.springframework.experiment.cds;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.experiment.cds.parser.ClassLoadingReport;
import org.springframework.util.MultiValueMap;

/**
 * Print statistics of a {@link ClassLoadingReport}.
 *
 * @author Stephane Nicoll
 */
class ClassLoadingReportPrinter {

	void print(ClassLoadingReport report, PrintStream out) {
		out.println("--------------------------------------------------------------------------");
		out.println("Class Loading Report:");
		long loadCount = report.getLoadCount();
		out.printf("%10d classes and JDK proxies loaded%n", loadCount);
		out.printf("%10d (%5.2f%%) from cache%n", report.getHits().size(), report.getHitRate() * 100);
		out.printf("%10d (%5.2f%%) from classpath%n",
				report.getMisses().values().stream().map(List::size).reduce(0, Integer::sum),
				report.getMissRate() * 100);
		out.println();
		out.println("Categories:");
		out.printf("%10s %s%n", "Lambdas", filter(report, lambda()).createReport(loadCount));
		out.printf("%10s %s%n", "Proxies", filter(report, proxy()).createReport(loadCount));
		out.printf("%10s %s%n", "Classes",
				filter(report, Predicate.not(lambda()).and(Predicate.not(proxy()))).createReport(loadCount));
		out.println();
		out.println("Top 10 locations from classpath:");
		extractTop10Locations(report.getMisses())
			.forEach((entry) -> out.printf("%10d %s%n", entry.getValue().size(), entry.getKey()));
		out.println();
		out.println("Top 10 packages:");
		extractTop10Packages(report).forEach((entry) -> out.printf("%10d %s (%.2f%% from cache)%n",
				entry.getValue().total(), entry.getKey(), entry.getValue().hitRate() * 100));
		out.println("--------------------------------------------------------------------------");
	}

	private CategoryDetail filter(ClassLoadingReport report, Predicate<String> classNameFilter) {
		long fromCache = report.getHits().stream().filter(classNameFilter).count();
		long fromClasspath = report.getMisses().values().stream().flatMap(List::stream).filter(classNameFilter).count();
		return new CategoryDetail(fromCache, fromClasspath);
	}

	private Stream<Entry<String, List<String>>> extractTop10Locations(MultiValueMap<String, String> content) {
		return content.entrySet()
			.stream()
			.sorted(Comparator.<Entry<String, List<String>>>comparingInt(o -> o.getValue().size()).reversed())
			.limit(10);
	}

	private Stream<Entry<String, CategoryDetail>> extractTop10Packages(ClassLoadingReport report) {
		Map<String, Integer> packageHits = new HashMap<>();
		report.getHits().forEach((className) -> {
			String packageName = extractPackageName(className);
			packageHits.merge(packageName, 1, Integer::sum);
		});
		Map<String, Integer> packageMisses = new HashMap<>();
		report.getMisses().values().stream().flatMap(Collection::stream).forEach((className) -> {
			String packageName = extractPackageName(className);
			packageMisses.merge(packageName, 1, Integer::sum);
		});
		Map<String, CategoryDetail> mappings = new LinkedHashMap<>();
		packageHits.forEach((className, hitCount) -> {
			long missRate = packageMisses.getOrDefault(className, 0);
			mappings.put(className, new CategoryDetail(hitCount, missRate));
		});
		packageMisses
			.forEach((className, missCount) -> mappings.putIfAbsent(className, new CategoryDetail(0, missCount)));
		return mappings.entrySet()
			.stream()
			.sorted(Comparator.<Entry<String, CategoryDetail>>comparingLong(o -> o.getValue().total()).reversed())
			.limit(10);
	}

	private String extractPackageName(String className) {
		String[] split = className.split("\\.");
		if (split.length > 2) {
			return "%s.%s".formatted(split[0], split[1]);
		}
		return className;
	}

	private Predicate<String> lambda() {
		return (className) -> className.contains("$$Lambda");
	}

	private Predicate<String> proxy() {
		return (className) -> className.contains("$Proxy");
	}

	private record CategoryDetail(long fromCache, long fromClasspath) {

		public float hitRate() {
			return ((float) fromCache / (float) total());
		}

		public long total() {
			return fromCache + fromClasspath;
		}

		public String createReport(long classesCount) {
			float share = ((float) total() / (float) classesCount);
			return String.format("%" + String.valueOf(classesCount).length() + "d (%5.2f%%): %.2f%% from cache",
					total(), share * 100, hitRate() * 100);
		}
	}

}
