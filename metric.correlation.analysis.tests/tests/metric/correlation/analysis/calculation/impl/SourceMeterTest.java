package metric.correlation.analysis.calculation.impl;

import static org.junit.Assert.fail;

import org.eclipse.jdt.core.IJavaProject;
import org.junit.Before;
import org.junit.Test;

public class SourceMeterTest {
	private SourceMeterMetrics sm;

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
		IJavaProject test = new IJavaProjectMock();
	}

}
