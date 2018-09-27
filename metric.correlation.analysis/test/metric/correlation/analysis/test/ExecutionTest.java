package metric.correlation.analysis.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import metric.correlation.analysis.MetricCalculation;
import metric.correlation.analysis.configuration.ProjectConfiguration;

public class ExecutionTest {

	private static final Logger LOGGER = LogManager.getLogger();

	@Test
	public void execute() throws UnsupportedOperationSystemException {
		try {
			File projectsReleaseDataJSON = new File(
					metric.projectSelection.ProjectsOutputCreator.projectsDataOutputFilePath);

			JsonNode configurationNode = JsonLoader.fromFile(projectsReleaseDataJSON);
			JsonNode schemaNode = JsonLoader.fromFile(new File("schema.json"));

			if (!JsonSchemaFactory.byDefault().getValidator().validate(schemaNode, configurationNode).isSuccess()) {
				LOGGER.log(Level.WARN,
						"The project configuration is not valid: " + projectsReleaseDataJSON.getAbsolutePath());
			} else {
				System.out.println(configurationNode.getNodeType());
			}

		} catch (IOException | ProcessingException e) {
			e.printStackTrace();
			return;
		}

		String projectsReleaseData = metric.projectSelection.ProjectsOutputCreator.projectsDataOutputFilePath;
		Gson gson = new Gson();
		JsonReader reader;

		try {
			reader = new JsonReader(new FileReader(projectsReleaseData));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		JsonObject projectsObject = gson.fromJson(reader, JsonObject.class);
		JsonArray projects = projectsObject.get("projects").getAsJsonArray();

		List<ProjectConfiguration> configs = new ArrayList<>(projects.size());
		for (JsonElement project : projects) {

			String productName = project.getAsJsonObject().get("productName").getAsString();
			String vendorName = project.getAsJsonObject().get("vendorName").getAsString();
			String gitURL = project.getAsJsonObject().get("url").getAsString();
			JsonArray commits = project.getAsJsonObject().get("commits").getAsJsonArray();

			Hashtable<String, String> commitsAndVersions = new Hashtable<String, String>();

			for (JsonElement commit : commits) {
				String commitId = commit.getAsJsonObject().get("commitId").getAsString();
				String commitVersion = commit.getAsJsonObject().get("version").getAsString();

				commitsAndVersions.put(commitVersion, commitId);
			}

			configs.add(new ProjectConfiguration(productName, vendorName, gitURL, commitsAndVersions));
			
		}
		new MetricCalculation().calculateAll(configs);

	}
}
