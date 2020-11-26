package metric.correlation.analysis.issues;

import org.junit.Test;

import metric.correlation.analysis.calculation.impl.IssueMetrics;

public class CrawlerTest {

	@Test
	public void testRetrieve() {
		IssueMetrics.getContributorCount("journeyapps", "zxing-android-embedded");
		GithubIssueCrawler crawler = new GithubIssueCrawler();
		crawler.getIssues("journeyapps", "zxing-android-embedded", "v3.5.0");
	}
}
