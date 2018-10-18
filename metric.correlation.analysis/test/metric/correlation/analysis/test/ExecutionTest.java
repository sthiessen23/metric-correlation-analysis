package metric.correlation.analysis.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.InitializationError;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingReport;

import metric.correlation.analysis.MetricCalculation;
import metric.correlation.analysis.configuration.ProjectConfiguration;
import metric.correlation.analysis.projectSelection.ProjectsOutputCreator;

@RunWith(Parameterized.class)
public class ExecutionTest {

	/**
	 * The maximum amount of projects which should be considered
	 */
	private static final int MAX_NUMBER_OF_PROJECTS = 100;

	/**
	 * From which project should be started
	 */
	private static final int OFFSET_FOR_PROJECTS = 0;

	/**
	 * The maximum amount of versions per projects which should be considered
	 */
	private static final int MAX_VERSIONS_OF_PROJECTS = 1;

	/**
	 * If all data should be cleaned after an execution
	 */
	private static final boolean CLEAN = false;

	/**
	 * If the JSON project file should be validated against the given Schema
	 */
	private static final boolean VALIDATE_JSON = false;

	private static final Logger LOGGER = Logger.getLogger(ExecutionTest.class);
	private static final MetricCalculation METRIC_CALCULATION = new MetricCalculation();


	private ProjectConfiguration config;

	public ExecutionTest(String projectName, ProjectConfiguration config) {
		this.config = config;
	}

	@Parameters(name = "Analyze: {0}")
	public static Collection<Object[]> collectProjects() throws IOException, ProcessingException {
		File projectsReleaseDataJSON = new File(ProjectsOutputCreator.projectsDataOutputFilePath);

		JsonNode projectsJsonData = JsonLoader.fromFile(projectsReleaseDataJSON);
		if (VALIDATE_JSON) {
			if (!checkDocument(projectsJsonData)) {
				throw new IllegalArgumentException("The given JSON file doesn't comply with the JSON Schema!");
			}
		}

		ArrayNode projects = (ArrayNode) projectsJsonData.get("projects");
		if(projects.size() == 0) {
			throw new IllegalArgumentException("There are no projects in the JSON documnet!");
		}

		int projectCounter = 0;
		List<Object[]> configs = new ArrayList<>(Math.min(MAX_NUMBER_OF_PROJECTS, projects.size()));
		for (JsonNode project : projects) {
			projectCounter++;
			if (OFFSET_FOR_PROJECTS >= projectCounter) {
				continue;
			}
			if (projectCounter > MAX_NUMBER_OF_PROJECTS + OFFSET_FOR_PROJECTS) {
				break;
			}

			String productName = project.get("productName").asText();
			String vendorName = project.get("vendorName").asText();
			String gitURL = project.get("url").asText();
			ArrayNode commits = (ArrayNode) project.get("commits");

			Hashtable<String, String> commitsAndVersions = new Hashtable<String, String>();

			int commitCounter = 0;
			for (JsonNode commit : commits) {
				if (commitCounter++ >= MAX_VERSIONS_OF_PROJECTS) {
					break;
				}
				String commitId = commit.get("commitId").asText();
				String commitVersion = commit.get("version").asText();

				commitsAndVersions.put(commitVersion, commitId);
			}

			ProjectConfiguration projectConfiguration = new ProjectConfiguration(productName, vendorName, gitURL,
					commitsAndVersions);
			configs.add(new Object[] { vendorName + "-" + productName, projectConfiguration });

		}
		return configs;
	}

	/**
	 * Checks the JSON document against the schema used by this application
	 * 
	 * @param projectsJsonData The JSON document the schema
	 * @return true, iff the JSON document complies with the schema
	 * @throws IOException         Iff the JSON Schema cannot be read
	 * @throws ProcessingException Iff processing the validation had an error
	 */
	static boolean checkDocument(JsonNode projectsJsonData) throws IOException, ProcessingException {
		ProcessingReport report = null;
		JsonNode schemaNode = JsonLoader.fromFile(new File("schema.json"));

		report = JsonSchemaFactory.byDefault().getValidator().validate(schemaNode, projectsJsonData);
		if (!report.isSuccess()) {
			LOGGER.log(Level.WARN, "The project configuration is not valid!");
			return false;
		}
		return true;
	}

	@Test
	public void execute() throws UnsupportedOperationSystemException {
		if (config.getGitCommitIds().isEmpty()) {
			fail("No commits available");
		}
		boolean success = METRIC_CALCULATION.calculate(config);
		if (!success) {
			fail(METRIC_CALCULATION.getLastErrors().stream().map(Object::toString)
					.collect(Collectors.joining(", ", "[", "]")));
		}
		assertTrue(success);
	}

	/**
	 * Clean the repository folder before test execution
	 * 
	 * @throws InitializationError
	 */
	@BeforeClass
	public static void cleanupBefore() throws InitializationError {
		if (!MetricCalculation.cleanupRepositories()) {
			throw new InitializationError("Couldn't clean repositories");
		}
	}

	/**
	 * Clean the repository folder after test execution if CLEAN == true
	 * 
	 * @throws InitializationError
	 */
	@AfterClass
	public static void cleanupAfter() throws InitializationError {
		if (!CLEAN) {
			return;
		}
		if (!MetricCalculation.cleanupRepositories()) {
			throw new InitializationError("Couldn't clean repositories");
		}
	}
}
