package org.springframework.experiment.cds;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.springframework.experiment.cds.parser.CdsArchiveReport;
import org.springframework.util.MultiValueMap;

/**
 * Print statistics of a {@link CdsArchiveReport}.
 *
 * @author Stephane Nicoll
 */
public class CdsArchiveReportPrinter {

	void print(CdsArchiveReport report, PrintStream out) {
		out.println("--------------------------------------------------------------------------");
		out.println("CDS Archive Report:");
		long skippedCount = report.getSkippedCount();
		out.printf("%10d classes were skipped%n", skippedCount);
		out.println();
		out.println("Top Reasons:");
		extractTop10Reasons(report.getSkipped())
			.forEach((entry) -> out.printf("%10d %s%n", entry.getValue().size(), entry.getKey()));
		out.println();
		out.println("Top Packages:");
		extractTop10Packages(report.getSkipped())
			.forEach((entry) -> out.printf("%10d %s%n", entry.getValue(), entry.getKey()));
		out.println("--------------------------------------------------------------------------");
	}

	private Stream<Entry<String, List<String>>> extractTop10Reasons(MultiValueMap<String, String> content) {
		return content.entrySet()
			.stream()
			.sorted(Comparator.<Entry<String, List<String>>>comparingInt(o -> o.getValue().size()).reversed())
			.limit(10);
	}

	private Stream<Entry<String, Integer>> extractTop10Packages(MultiValueMap<String, String> content) {
		Map<String, Integer> packageSkipped = new HashMap<>();
		content.values().stream().flatMap(Collection::stream).forEach((className) -> {
			String packageName = extractPackageName(className);
			packageSkipped.merge(packageName, 1, Integer::sum);
		});
		return packageSkipped.entrySet()
			.stream()
			.sorted(Comparator.<Entry<String, Integer>>comparingInt(Entry::getValue).reversed())
			.limit(10);
	}

	private String extractPackageName(String className) {
		String[] split = className.split("\\.");
		if (split.length > 2) {
			return "%s.%s".formatted(split[0], split[1]);
		}
		return className;
	}

}