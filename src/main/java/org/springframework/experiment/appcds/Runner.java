package org.springframework.experiment.appcds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.experiment.appcds.parser.ClassLoadingLogParser;
import org.springframework.experiment.appcds.parser.ClassLoadingReport;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * {@link ApplicationRunner} implementation that parses and print a class loading report.
 *
 * @author Stephane Nicoll
 */
@Component
class Runner implements ApplicationRunner {

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String target = getValue(args, "target", System.getProperty("user.dir"));
		Path workingDirectory = Paths.get(target);
		Mode mode = Mode.from(args);
		switch (mode) {
			case PARSE -> parseJvmLogs(args, workingDirectory);
		}
	}

	private void parseJvmLogs(ApplicationArguments args, Path workingDirectory) throws IOException {
		String fileName = getValue(args, "logFile", "cds.log");
		Path logFile = workingDirectory.resolve(fileName);
		if (!Files.exists(logFile)) {
			throw new IllegalArgumentException(
					"JVM log file does not exist: '" + logFile.toAbsolutePath() + "' Set --target or --logFile");
		}
		ClassLoadingLogParser parser = new ClassLoadingLogParser(workingDirectory);
		ClassLoadingReport report = parser.parser(new FileSystemResource(logFile));
		new ReportPrinter().print(report, System.out);
	}

	private static String getValue(ApplicationArguments args, String option, String defaultValue) {
		List<String> values = args.getOptionValues(option);
		if (CollectionUtils.isEmpty(values)) {
			return defaultValue;
		}
		if (values.size() > 1) {
			throw new IllegalArgumentException(
					"Only one value should be specified for '" + option + "', got " + values);
		}
		return values.get(0);
	}

	enum Mode {

		/**
		 * Parse an existing {@code cds.log} file and output the statistics.
		 */
		PARSE;

		static Mode from(ApplicationArguments arguments) {
			String value = getValue(arguments, "mode", PARSE.name());
			return Mode.valueOf(value.toUpperCase(Locale.ENGLISH));
		}

	}

}
