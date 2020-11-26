package metric.correlation.analysis.issues;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IssueCrawler {

	@Test
	public void testRetrieve() {
		GithubIssueCrawler crawler = new GithubIssueCrawler();
		crawler.getIssues("journeyapps", "zxing-android-embedded", "v3.5.0");
	}
	
	@Test
	public void testWhatever() {
		assertTrue(true);
	}
}
