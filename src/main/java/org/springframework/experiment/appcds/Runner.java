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
import org.springframework.experiment.appcds.parser.CdsArchiveLogParser;
import org.springframework.experiment.appcds.parser.CdsArchiveReport;
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
			case CREATE -> createCdsArchive(args, workingDirectory);
		}
	}

	private void createCdsArchive(ApplicationArguments args, Path workingDirectory) throws Exception {
		List<String> applicationArguments = detectApplication(args, workingDirectory);
		if (applicationArguments == null) {
			throw new IllegalStateException("No application detected in " + workingDirectory);
		}
		AppRunner appRunner = new AppRunner();
		System.out.println("Creating the CDS archive ...");
		System.out.println();
		System.out.println("Using java version:");
		System.out.println(appRunner.getJavaVersion());
		System.out.println("Starting application using command: java " + String.join(" ", applicationArguments));
		Path cdsArchive = appRunner.createCdsArchive(workingDirectory, applicationArguments);
		CdsArchiveReport report = new CdsArchiveLogParser().parse(new FileSystemResource(cdsArchive));
		new CdsArchiveReportPrinter().print(report, System.out);
		System.out
			.println("To use the archive and collect class loading logs for this application, add the following flag:");
		System.out.println();
		System.out.println("\t-XX:SharedArchiveFile=app-cds.jsa -Xlog:class+load:file=cds.log");
	}

	private List<String> detectApplication(ApplicationArguments args, Path workingDirectory) {
		String jarFile = getValue(args, "jar", null);
		if (jarFile != null) {
			if (!Files.exists(workingDirectory.resolve(jarFile))) {
				throw new IllegalArgumentException(
						"Specified jar file does not exist: " + workingDirectory.resolve(jarFile));
			}
			return List.of("-jar", jarFile);
		}
		else if (Files.exists(workingDirectory.resolve("BOOT-INF"))) {
			return List.of("org.springframework.boot.loader.launch.JarLauncher");
		}
		else if (Files.exists(workingDirectory.resolve("run-app.jar"))) {
			return List.of("-jar", "run-app.jar");
		}
		return null;
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
		new ClassLoadingReportPrinter().print(report, System.out);
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
		PARSE,

		/**
		 * Create the CDS archive for an application and output a report about its
		 * creation.
		 */
		CREATE;

		static Mode from(ApplicationArguments arguments) {
			String value = getValue(arguments, "mode", PARSE.name());
			return Mode.valueOf(value.toUpperCase(Locale.ENGLISH));
		}

	}

}
