package metric.correlation.analysis.projectSelection;

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public abstract class FileBasedGithubprojectSelector implements IGithubProjectSelector {

	private static final Logger LOGGER = Logger.getLogger(FileBasedGithubprojectSelector.class);

	private final String FILE_NAME;

	public FileBasedGithubprojectSelector(String fileName) {
		this.FILE_NAME = fileName;
	}

	/**
	 * Tests if a repository has the specified path in its root directory
	 * 
	 * @param repositoryName the name of the repository to be tested
	 * @param OAuthToken
	 * @return true if it contains the file in its root directory
	 */
	@Override
	public boolean accept(String repositoryName, String OAuthToken) {
		String searchUrl;
		try {
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			if (ProjectSelector.GIT_REQUESTS++ % 20 == 0) {
				TimeUnit.MINUTES.sleep(1);
			}
			searchUrl = "https://github.com/" + repositoryName + "/blob/master/" + FILE_NAME;
			HttpGet request = new HttpGet(searchUrl);
			request.addHeader("content-type", "application/json");
			request.addHeader("Authorization", "Token " + ProjectSelector.oAuthToken);
			HttpResponse result = httpClient.execute(request);
			if (result.getStatusLine().getStatusCode() != 404) {
				return true;
			}
			httpClient.close();
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "Could not check if repository is a Gradle repository.");
			LOGGER.log(Level.INFO, e.getStackTrace());
		}

		return false;
	}

}