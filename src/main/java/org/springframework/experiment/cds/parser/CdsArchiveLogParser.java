package org.springframework.experiment.cds.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
 * -Xlog:cds=warning:file=cds-warnings.log:tags
 * </pre>
 * 
 * @author Stephane Nicoll
 */
public class CdsArchiveLogParser {

	private static final Log logger = LogFactory.getLog(CdsArchiveLogParser.class);

	public CdsArchiveReport parse(Resource resource) throws IOException {
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

		private static final String SKIPPING_TAG = "Skipping";

		private final MultiValueMap<String, String> skipped = new LinkedMultiValueMap<>();

		@Override
		public void accept(String content) {
			LogLine logLine = LogLine.parse(content);
			if (!logLine.containTags("cds")) {
				return;
			}
			String message = logLine.message();
			if (!message.startsWith(SKIPPING_TAG)) {
				logger.debug("Could not process " + message);
				return;
			}
			String classNameAndReason = message.substring(SKIPPING_TAG.length());
			int separator = classNameAndReason.indexOf(":");
			if (separator == -1) {
				logger.warn("Separator not found " + message);
				return;
			}
			String className = classNameAndReason.substring(0, separator).trim().replace('/', '.');
			String reason = classNameAndReason.substring(separator + 1).trim();
			if (reason.contains("is excluded") && reason.contains("interface ")) {
				skipped.add(CdsArchiveReport.INTERFACE_EXCLUDED, className);
			}
			else if (reason.contains("is excluded") && reason.contains("super class ")) {
				skipped.add(CdsArchiveReport.SUPER_CLASS_EXCLUDED, className);
			}
			else {
				skipped.add(reason, className);
			}
		}

		public CdsArchiveReport toReport() {
			return new CdsArchiveReport(this.skipped);
		}

	}

}
