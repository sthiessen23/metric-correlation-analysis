package metric.correlation.analysis.projectSelection;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import metric.correlation.analysis.io.FileUtils;

public class ProjectsOutputCreator {

	private static final Logger LOGGER = Logger.getLogger(ProjectsOutputCreator.class);

	public static String projectsDataOutputFilePath = "Resources/projectsReleaseData.json";
	public static String normalizedProjectsDataOutputFilePath = "Resources/projectsReleaseData-normalized.json";

	/**
	 * @author Antoniya Ivanova - prepares the JSON output for the repository
	 *         search, includes the releases for each repository and what
	 *         version/commit they relate to.
	 *
	 */
	public void getProjectReleases() {
		int apiTimeOutcounter = 1;

		HashSet<SearchHit> repositoriesWithCVEs = new ProjectSelector().getProjectsWithAtLeastOneVulnerability();

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

	@Test
	public void cleanUpProjectVersions() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// Reading the unnormalized file
		try (JsonReader reader = new JsonReader(new FileReader(projectsDataOutputFilePath))) {
			JsonObject jsonTree = gson.fromJson(reader, JsonObject.class);
			// Getting all the projects
			JsonArray projects = jsonTree.get("projects").getAsJsonArray();

			// Setting up the normalized file
			JsonObject resultJSON = new JsonObject();
			JsonArray resultArray = new JsonArray();
			resultJSON.add("projects", resultArray);

			// Iterate the unnormalized file
			for (int i = 0; i < projects.size(); i++) {
				// Get a project
				JsonObject jo = (JsonObject) projects.get(i);

				// Create a project object for the new file
				JsonObject projectJSON = new JsonObject();

				// Set the new properties
				projectJSON.addProperty("productName", jo.get("productName").getAsString());
				projectJSON.addProperty("vendorName", jo.get("vendorName").getAsString());
				projectJSON.addProperty("url", jo.get("url").getAsString());

				// New commit data
				JsonArray newCommits = new JsonArray();
				projectJSON.add("commits", newCommits);

				JsonArray commits = jo.get("commits").getAsJsonArray();

				for (int j = 0; j < commits.size(); j++) {
					JsonObject commitAndVersion = (JsonObject) commits.get(j);

					// Get version
					String version = commitAndVersion.get("version").getAsString();

					// Normalize the version
					version = version.toLowerCase();

					if (version.matches("(\\.|-|_)(snapshot|pre|alpha|beta|rc|m|prototype)(?![a-z])(\\.|-|_)?[0-9]*")) {
						break;
					}

					else {
						// Add it to the new json
						final Pattern p = Pattern.compile("[0-9]+((\\.|_|-)[0-9]+)+");
						final Matcher m = p.matcher(version);
						while (m.find()) {
							version = m.group();
							version = version.replaceAll("(_|-)", ".");
							JsonObject newCommit = new JsonObject();
							newCommit.addProperty("commitId", commitAndVersion.get("commitId").getAsString());
							newCommit.addProperty("version", version);

							newCommits.add(newCommit);
						}

					}
				}

				if (newCommits.size() == 0) {
					continue;
				} else {
					resultArray.add(projectJSON);
				}
				
				//Write the new file
				FileUtils fileUtils = new FileUtils();
				fileUtils.createDirectory("Resources");

				try (FileWriter fileWriter = new FileWriter(normalizedProjectsDataOutputFilePath)) {
					fileWriter.write(gson.toJson(resultJSON));
				} catch (Exception e) {
					LOGGER.log(Level.ERROR, "Couldn't write normalized file", e);
				}

			}
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, "Could not read old projects output file", e);
		}
	}
}
