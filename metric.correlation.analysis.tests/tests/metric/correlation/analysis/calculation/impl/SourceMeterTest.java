package metric.correlation.analysis.calculation.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.Before;
import org.junit.Test;

public class SourceMeterTest {
	private SourceMeterMetrics sm;
	private static final String PROJECT_PATH = "resources/testProject";
	@Before
	public void init() {
		try {
			sm = new SourceMeterMetrics();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void sampleProject() {
		Map<String, String> expected = new HashMap<>();
		expected.put("DIT", "0.00");
		expected.put("CBO", "0.00");
		expected.put("LDC", "0.00");
		expected.put("LLOC", "5.00");
		expected.put("WMC", "1.00");
		expected.put("LCOM5", "1.00");
		expected.put("LOCpC", "5.00");
		IProject project = new IProjectMock(new Path(PROJECT_PATH), "testProject");
		IJavaProject test = new IJavaProjectMock(project);
		Map<String, String> results = new HashMap<>();
		boolean success = sm.calculateMetric(test, "testProject", "testVender", "1.0.0", results);
		assertTrue(success);
		assertEquals(expected, sm.getResults());
	}
	
	@Test(expected = IllegalStateException.class)
	public void getResultWithoutCalcuationError() {
		sm.getResults();
	}

}
