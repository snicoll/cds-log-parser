package org.springframework.experiment.appcds.parser;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CdsArchiveLogParser}.
 *
 * @author Stephane Nicoll
 */
class CdsArchiveLogParserTests {

	private final CdsArchiveLogParser parser = new CdsArchiveLogParser();

	@Test
	void parseSuperClassExcluded() {
		CdsArchiveReport report = parseSampleLog("cds-archive-super-class-excluded");
		assertThat(report.getSkippedCount()).isEqualTo(2);
		assertThat(report.getSkipped()).containsOnlyKeys(CdsArchiveReport.SUPER_CLASS_EXCLUDED);
		assertThat(report.getSkipped().get(CdsArchiveReport.SUPER_CLASS_EXCLUDED)).containsExactly(
				"net.bytebuddy.utility.visitor.MetadataAwareClassVisitor",
				"net.bytebuddy.dynamic.scaffold.TypeWriter$Default$ForCreation$CreationClassVisitor");
	}

	@Test
	void parseInterfaceExcluded() {
		CdsArchiveReport report = parseSampleLog("cds-archive-interface-excluded");
		assertThat(report.getSkippedCount()).isEqualTo(2);
		assertThat(report.getSkipped()).containsOnlyKeys(CdsArchiveReport.INTERFACE_EXCLUDED);
		assertThat(report.getSkipped().get(CdsArchiveReport.INTERFACE_EXCLUDED)).containsExactly(
				"net.bytebuddy.description.type.RecordComponentDescription",
				"net.bytebuddy.description.type.RecordComponentDescription$InDefinedShape");
	}

	@Test
	void parseJfrEvent() {
		CdsArchiveReport report = parseSampleLog("cds-archive-jfr-event");
		assertThat(report.getSkippedCount()).isEqualTo(1);
		assertThat(report.getSkipped()).containsOnlyKeys("JFR event class");
		assertThat(report.getSkipped().get("JFR event class")).containsExactly("jdk.internal.event.ThreadSleepEvent");
	}

	private CdsArchiveReport parseSampleLog(String name) {
		ClassPathResource resource = new ClassPathResource("sample/logs/%s.log".formatted(name));
		try {
			return this.parser.parse(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
