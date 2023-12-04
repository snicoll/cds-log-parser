package org.springframework.experiment.cds.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Simple log parser that expects only tags to be specified as decorators of the JVM logs,
 * something like: <pre>
 * -Xlog:class+load:file=cds.log:tags
 * </pre>
 *
 * @author Stephane Nicoll
 */
public class ClassLoadingLogParser {

	private static final Log logger = LogFactory.getLog(ClassLoadingLogParser.class);

	private final Path workingDir;

	public ClassLoadingLogParser(Path workingDir) {
		this.workingDir = workingDir;
	}

	public ClassLoadingReport parser(Resource resource) throws IOException {
		if (!resource.exists()) {
			throw new IllegalAccessError("Resource " + resource + " does not exist");
		}
		LogLineParser lineParser = new LogLineParser();
		process(resource, lineParser);
		return lineParser.toReport();
	}

	private void process(Resource resource, Consumer<String> line) throws IOException {
		try (Scanner scanner = new Scanner(resource.getInputStream(), StandardCharsets.UTF_8)) {
			while (scanner.hasNextLine()) {
				String nextLine = scanner.nextLine();
				if (StringUtils.hasText(nextLine)) {
					line.accept(nextLine);
				}
			}
		}
	}

	private class LogLineParser implements Consumer<String> {

		private static final String SOURCE_TAG = "source: ";

		private static final String HIT_SOURCE = "shared objects file";

		private static final String FILE_URI_PREFIX = "file:";

		private final List<String> hits = new ArrayList<>();

		private final MultiValueMap<String, String> misses = new LinkedMultiValueMap<>();

		@Override
		public void accept(String content) {
			LogLine logLine = LogLine.parse(content);
			if (!logLine.containTags("class", "load")) {
				return;
			}
			String message = logLine.message();
			int sourceIndex = message.indexOf(SOURCE_TAG);
			if (sourceIndex == -1) {
				logger.debug("No source found in " + message);
				return;
			}
			String source = message.substring(sourceIndex + SOURCE_TAG.length()).trim();
			String className = message.substring(0, sourceIndex).trim();
			if (source.startsWith(HIT_SOURCE)) {
				hits.add(className);
			}
			else if (source.startsWith(FILE_URI_PREFIX)) {
				Path path = Path.of(source.substring(FILE_URI_PREFIX.length()));
				Path pathToUse = path.startsWith(workingDir) ? workingDir.relativize(path) : path;
				misses.add(pathToUse.toString(), className);
			}
			else if (source.startsWith("jar:nested:")) {
				int start = source.indexOf("!");
				int end = source.indexOf("!", start + 1);
				if (start == -1 || end == -1) {
					throw new IllegalArgumentException("Nested jar not found in " + source);
				}
				misses.add(source.substring(start + 1, end), className);
			}
			else if (source.equals(ClassLoadingReport.CLASS_DEFINER)
					|| source.equals(ClassLoadingReport.DYNAMIC_GENERATED_LAMBDA)
					|| source.equals(ClassLoadingReport.DYNAMIC_PROXY)) {
				misses.add(source, className);
			}
			else if (className.startsWith(source)) { // Lambda
				misses.add(source, className);
			}
			else if (source.startsWith("jrt:/")) { // Java Runtime Image
				misses.add(source, className);
			}
			else if (source.startsWith("instance of ")) {
				misses.add(source.substring("instance of ".length()), className);
			}
			else {
				logger.warn("Fallback on default source for " + logLine);
				misses.add(source, className);
			}
		}

		public ClassLoadingReport toReport() {
			return new ClassLoadingReport(this.hits, this.misses);
		}

	}

}
