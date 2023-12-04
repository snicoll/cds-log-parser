package org.springframework.experiment.cds.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.offset;

/**
 * Tests for {@link ClassLoadingReport}.
 *
 * @author Stephane Nicoll
 */
class ClassLoadingLogParserTests {

	private final ClassLoadingLogParser parser = new ClassLoadingLogParser(Path.of("/tmp/test-app/target/app/"));

	@Test
	void parseJdkArchiveOnly() {
		ClassLoadingReport report = parseSampleLog("jdk-archive-only");
		assertThat(report.getLoadCount()).isEqualTo(14);
		assertThat(report.getHitRate()).isEqualTo(1.0f);
		assertThat(report.getMissRate()).isEqualTo(0.0f);
		assertThat(report.getHits()).contains("java.lang.Object", "java.lang.Cloneable");
		assertThat(report.getMisses()).isEmpty();
	}

	@Test
	void parseJarsOnly() {
		ClassLoadingReport report = parseSampleLog("jars-only");
		assertThat(report.getLoadCount()).isEqualTo(10);
		assertThat(report.getHitRate()).isEqualTo(0.0f);
		assertThat(report.getMissRate()).isEqualTo(1.0f);
		assertThat(report.getMisses().get("BOOT-INF/lib/spring-context-6.1.0-RC2.jar"))
			.contains("org.springframework.context.ApplicationListener");
		assertThat(report.getMisses().get("BOOT-INF/lib/spring-jcl-6.1.0-RC2.jar")).contains(
				"org.apache.commons.logging.LogFactory", "org.apache.commons.logging.LogFactory$1",
				"org.apache.commons.logging.LogAdapter");
		assertThat(report.getHits()).isEmpty();
	}

	@Test
	void parseClasspathOnly() {
		ClassLoadingReport report = parseSampleLog("classpath-only");
		assertThat(report.getLoadCount()).isEqualTo(2);
		assertThat(report.getHitRate()).isEqualTo(0.0f);
		assertThat(report.getMissRate()).isEqualTo(1.0f);
		assertThat(report.getMisses().get("")).contains("org.springframework.boot.loader.net.protocol.jar.Handler",
				"org.springframework.boot.loader.net.protocol.jar.JarUrlConnection");
	}

	@Test
	void parseJarsAndJdkArchive() {
		ClassLoadingReport report = parseSampleLog("jars-and-jdk-archive");
		assertThat(report.getLoadCount()).isEqualTo(5);
		assertThat(report.getHitRate()).isEqualTo(0.6f, offset(0.01f));
		assertThat(report.getMissRate()).isEqualTo(0.4f, offset(0.01f));
		assertThat(report.getMisses().get("BOOT-INF/lib/spring-context-6.1.0-RC2.jar"))
			.contains("org.springframework.context.ApplicationListener");
		assertThat(report.getMisses().get("BOOT-INF/lib/spring-jcl-6.1.0-RC2.jar"))
			.contains("org.apache.commons.logging.LogFactory");
		assertThat(report.getHits()).contains("java.lang.Object", "java.io.Serializable", "java.lang.Comparable");
	}

	@Test
	void parseLogWithAdditionalCategories() {
		ClassLoadingReport report = parseSampleLog("default-log-format");
		assertThat(report.getLoadCount()).isEqualTo(3);
		assertThat(report.getHitRate()).isEqualTo(1.0f);
		assertThat(report.getMissRate()).isEqualTo(0.0f);
		assertThat(report.getHits()).contains("java.lang.Object", "java.io.Serializable", "java.lang.Comparable");
	}

	@Test
	void parseLogWithJdk17Format() {
		ClassLoadingReport report = parseSampleLog("jdk17-log-format");
		assertThat(report.getLoadCount()).isEqualTo(4);
		assertThat(report.getHits()).contains("java.io.FilePermission$1", "sun.security.util.FilePermCompat",
				"sun.security.util.SecurityProperties");
		assertThat(report.getMisses()).containsKey("BOOT-INF/lib/spring-boot-3.2.0-SNAPSHOT.jar");
		assertThat(report.getMisses().get("BOOT-INF/lib/spring-boot-3.2.0-SNAPSHOT.jar"))
			.contains("org.springframework.boot.ExitCodeEvent");
	}

	@Test
	void parseLogWithTemurinFormat() {
		ClassLoadingReport report = parseSampleLog("temurin-format");
		assertThat(report.getLoadCount()).isEqualTo(5);
		assertThat(report.getHits()).contains("java.lang.Object", "java.io.Serializable", "java.lang.Comparable");
		assertThat(report.getMisses()).containsOnly(entry("java.lang.Shutdown", List
			.of("java.lang.invoke.DelegatingMethodHandle$Holder", "java.lang.invoke.DirectMethodHandle$Holder")));
	}

	private ClassLoadingReport parseSampleLog(String name) {
		ClassPathResource resource = new ClassPathResource("sample/logs/%s.log".formatted(name));
		try {
			return this.parser.parser(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
