package org.springframework.experiment.appcds;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
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
		out.println("Class Loading Report");
		out.printf("%10d classes and JDK proxies loaded%n", report.getLoadCount());
		out.printf("%10d (%.2f%%) from cache%n", report.getHits().size(), report.getHitRate() * 100);
		out.printf("%10d (%.2f%%) from classpath%n",
				report.getMisses().values().stream().map(List::size).reduce(0, Integer::sum),
				report.getMissRate() * 100);
		out.println();
		out.println("Top 10 locations from classpath:");
		extractTop10(report.getMisses()).forEach((entry) -> {
			out.printf("%10d %s%n", entry.getValue().size(), entry.getKey());
		});
		out.println("--------------------------------------------------------------------------");
	}

	private Stream<Entry<String, List<String>>> extractTop10(MultiValueMap<String, String> content) {
		return content.entrySet()
			.stream()
			.sorted(Comparator.<Entry<String, List<String>>>comparingInt(o -> o.getValue().size()).reversed())
			.limit(10);
	}

}
