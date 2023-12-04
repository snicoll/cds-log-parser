package org.springframework.experiment.cds;

import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.experiment.cds.parser.CdsArchiveLogParser;

/**
 * Helper class to run an application.
 *
 * @author Stephane Nicoll
 */
class AppRunner {

	/**
	 * Run the application specified by the {@code processArguments}, adding the necessary
	 * flags to enable the creation of a CDS archive.
	 * @param workingDirectory the working directory to use
	 * @param processArguments the arguments to pass to the {@code java} process
	 * @return a file that can be parsed by {@link CdsArchiveLogParser}.
	 */
	Path createCdsArchive(Path workingDirectory, List<String> processArguments) throws Exception {
		Path cdsArchiveLogFile = Files.createTempFile("cds-archive-warnings", ".log");
		List<String> allArguments = new ArrayList<>();
		allArguments.add("java");
		allArguments.add("-Xlog:cds=off:stdout"); // disable logging of CDS in the console
		allArguments.add("-Xlog:cds=warning:file=%s:tags".formatted(cdsArchiveLogFile.toString()));
		allArguments.add("-XX:ArchiveClassesAtExit=application.jsa");
		allArguments.add("-Dspring.context.exit=onRefresh"); // Exit automatically
		allArguments.addAll(processArguments);
		Path out = Files.createTempFile("cds-archive-run", ".log");
		int exit = configureOutput(new ProcessBuilder(), out).command(allArguments)
			.directory(workingDirectory.toFile())
			.start()
			.waitFor();
		if (exit != 0) {
			System.out.println(Files.readString(out));
			throw new IllegalArgumentException("Failed to run application, see log above");
		}
		return cdsArchiveLogFile;
	}

	/**
	 * Return the output of {@code java --version}.
	 * @return the output of the current java version
	 */
	String getJavaVersion() throws Exception {
		Path tempFile = Files.createTempFile("cds-java-version", ".log");
		int exit = configureOutput(new ProcessBuilder(), tempFile).command("java", "--version").start().waitFor();
		if (exit != 0) {
			throw new IllegalStateException("Failed to invoke java, make sure it is in your path");
		}
		return Files.readString(tempFile);
	}

	private ProcessBuilder configureOutput(ProcessBuilder processBuilder, Path out) {
		return processBuilder.redirectOutput(Redirect.appendTo(out.toFile()))
			.redirectError(Redirect.appendTo(out.toFile()));
	}

}
