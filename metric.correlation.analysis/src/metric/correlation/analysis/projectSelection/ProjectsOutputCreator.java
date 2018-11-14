package metric.correlation.analysis.projectSelection;

import java.io.FileWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import metric.correlation.analysis.io.FileUtils;

public class ProjectsOutputCreator {

	private static final Logger LOGGER = Logger.getLogger(ProjectsOutputCreator.class);

	public static String projectsDataOutputFilePath = "Resources/projectsReleaseData.json";

	/**
	 * @author Antoniya Ivanova - prepares the JSON output for the repository
	 *         search, includes the releases for each repository and what
	 *         version/commit they relate to.
	 *
	 */
	@Test
	public void getProjectReleases() {
		int apiTimeOutcounter = 1;

		HashSet<SearchHit> repositoriesWithCVEs = new ProjectSelector().getProjectsWithAtLeastOneVulnerability();
		// HashSet<SearchHit> repositoriesWithCVEsInitial = new
		// ProjectSelector().getProjectsWithAtLeastOneVulnerability();
		// HashSet<SearchHit> repositoriesWithCVEs = new HashSet<SearchHit>();
		//
		// int a = 0;
		// for (SearchHit searchHit : repositoriesWithCVEsInitial) {
		// if(a == 10)
		// break;
		// repositoriesWithCVEs.add(searchHit);
		// a++;
		// }

		JsonObject resultJSON = new JsonObject();
		JsonArray resultArray = new JsonArray();
		resultJSON.add("projects", resultArray);

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

			// Iterate the vulnerable projects
			for (SearchHit repository : repositoriesWithCVEs) {
				JsonObject projectJSON = new JsonObject();

				Map<String, Object> map = repository.getSourceAsMap();

				String productName = map.get("Product").toString();
				String vendorName = map.get("Vendor").toString();
				String URL = "http://www.github.com/" + vendorName + "/" + productName + ".git";

				projectJSON.addProperty("productName", productName);
				projectJSON.addProperty("vendorName", vendorName);
				projectJSON.addProperty("url", URL);

				JsonArray commits = new JsonArray();
				projectJSON.add("commits", commits);

				String gitURL;

				// Iterate over the project release pages
				for (int i = 0; i < 100; i++) {
					// Respect 30 request limit of GitHub API
					if (apiTimeOutcounter % 30 == 0) {
						LOGGER.log(Level.INFO, apiTimeOutcounter);
						TimeUnit.MINUTES.sleep(2);
					}

					gitURL = "https://api.github.com/repos/" + vendorName + "/" + productName + "/tags?page=" + i
							+ "&per_page=100&access_token=" + ProjectSelector.oAuthToken;

					apiTimeOutcounter++;

					HttpGet request = new HttpGet(gitURL);
					request.addHeader("content-type", "application/json");

					HttpResponse result = httpClient.execute(request);
					String json = EntityUtils.toString(result.getEntity(), "UTF-8");
					JsonArray jarray = new JsonParser().parse(json).getAsJsonArray();

					if (jarray.size() != 0) {

						// Iterate over the project releases on the page
						for (int j = 0; j < jarray.size(); j++) {
							JsonObject commit = new JsonObject();
							JsonObject jo = (JsonObject) jarray.get(j);

							commit.addProperty("commitId",
									jo.get("commit").getAsJsonObject().get("sha").toString().replace("\"", ""));
							commit.addProperty("version", jo.get("name").toString().replace("\"", ""));

							commits.add(commit);
						}
					} else {
						break;
					}
				}

				if (commits.size() == 0) {
					continue;
				} else {
					resultArray.add(projectJSON);
				}

			}

			FileUtils fileUtils = new FileUtils();
			fileUtils.createDirectory("Resources");

			try (FileWriter fileWriter = new FileWriter(projectsDataOutputFilePath)) {
				fileWriter.write(resultJSON.toString());
			}
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

	}

}
