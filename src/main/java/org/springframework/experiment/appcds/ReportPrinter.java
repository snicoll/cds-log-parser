package org.springframework.experiment.appcds;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.experiment.appcds.parser.ClassLoadingReport;
import org.springframework.util.MultiValueMap;

/**
 * Print statistics of a {@link ClassLoadingReport}.
 *
 * @author Stephane Nicoll
 */
class ReportPrinter {

	void print(ClassLoadingReport report, PrintStream out) {
		out.println("--------------------------------------------------------------------------");
		out.println("Class Loading Report:");
		long loadCount = report.getLoadCount();
		out.printf("%10d classes and JDK proxies loaded%n", loadCount);
		out.printf("%10d (%.2f%%) from cache%n", report.getHits().size(), report.getHitRate() * 100);
		out.printf("%10d (%.2f%%) from classpath%n",
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
		extractTop10(report.getMisses()).forEach((entry) -> {
			out.printf("%10d %s%n", entry.getValue().size(), entry.getKey());
		});
		out.println("--------------------------------------------------------------------------");
	}

	private CategoryDetail filter(ClassLoadingReport report, Predicate<String> classNameFilter) {
		long fromCache = report.getHits().stream().filter(classNameFilter).count();
		long fromClasspath = report.getMisses().values().stream().flatMap(List::stream).filter(classNameFilter).count();
		return new CategoryDetail(fromCache, fromClasspath);
	}

	private Stream<Entry<String, List<String>>> extractTop10(MultiValueMap<String, String> content) {
		return content.entrySet()
			.stream()
			.sorted(Comparator.<Entry<String, List<String>>>comparingInt(o -> o.getValue().size()).reversed())
			.limit(10);
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

		public float missRate() {
			return 1 - hitRate();
		}

		public long total() {
			return fromCache + fromClasspath;
		}

		public String createReport(long classesCount) {
			float share = ((float) total() / (float) classesCount);
			return "%d (%.2f%%): %.2f%% from cache".formatted(total(), share * 100, hitRate() * 100);
		}
	}

}
