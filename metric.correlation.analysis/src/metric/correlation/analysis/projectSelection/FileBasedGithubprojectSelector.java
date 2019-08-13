package metric.correlation.analysis.projectSelection;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public abstract class FileBasedGithubprojectSelector implements IGithubProjectSelector {

	private static final Logger LOGGER = Logger.getLogger(FileBasedGithubprojectSelector.class);

	private final String FILE_NAME;

	public FileBasedGithubprojectSelector(String fileName) {
		this.FILE_NAME = fileName;
	}

	/**
	 * Tests if a repository is a Gradle repository. Checks for file "build.gradle"
	 * in its contents.
	 * 
	 * @param repositoryName the name of the repository to be tested
	 * @param OAuthToken
	 * @return true if it is a Gradle repository, false otherwise.
	 */
	@Override
	public boolean accept(String repositoryName, String OAuthToken) {
		boolean result = false;
		String searchUrl;
		try {
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();

			searchUrl = "https://api.github.com/search/code?q=repo:" + repositoryName + "+filename:" + FILE_NAME
					+ "&access_token=" + OAuthToken;

			HttpGet requestGradleRepository = new HttpGet(searchUrl);
			requestGradleRepository.addHeader("content-type", "application/json");

			HttpResponse resultResponse = httpClient.execute(requestGradleRepository);
			String json = EntityUtils.toString(resultResponse.getEntity(), "UTF-8");

			JsonElement jelement = new JsonParser().parse(json);

			try {
				result = jelement.getAsJsonObject().get("total_count").getAsInt() != 0 ? true : false;

			} catch (Exception e) {
				LOGGER.log(Level.INFO, e.toString());
			}

			httpClient.close();

		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "Could not check if repository is a Gradle repository.");
			LOGGER.log(Level.INFO, e.getStackTrace());
		}

		return result;
	}

}
