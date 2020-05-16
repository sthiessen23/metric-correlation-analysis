package metric.correlation.analysis;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.core.IJavaProject;
import org.junit.Before;
import org.junit.Test;

public class ImportTest {
	private static final String MAVEN_PROJECT_PATH = "resources/maven-simple";
	private static final String GRADLE_PROJECT_PATH = "resources/gradle-simple";
	private MetricCalculation metricCalculation;
	
	@Before
	public void init() {
		try {
			metricCalculation = new MetricCalculation();
		} catch (IOException e) {
			fail("could not create MetricCalculation");
		}
	}
	
	@Test
	public void testGradleImport() {
		File f = new File(GRADLE_PROJECT_PATH);
		if (!f.exists()) {
			fail("project does not exist at expected location");
		}
		// enforces the import to finish without build errors
		IJavaProject importProject = metricCalculation.importProject(new File(GRADLE_PROJECT_PATH), false);
		assertNotNull(importProject);
	}
	
	@Test
	public void testMavenImport() {
		File f = new File(GRADLE_PROJECT_PATH);
		if (!f.exists()) {
			fail("project does not exist at expected location");
		}
		// enforces the import to finish without build errors
		IJavaProject importProject = metricCalculation.importProject(new File(MAVEN_PROJECT_PATH), false);
		assertNotNull(importProject);
	}
	
}
