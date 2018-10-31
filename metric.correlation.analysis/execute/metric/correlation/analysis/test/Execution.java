package metric.correlation.analysis.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;

import metric.correlation.analysis.MetricCalculation;
import metric.correlation.analysis.configuration.ProjectConfiguration;
import metric.correlation.analysis.projectSelection.ProjectsOutputCreator;

@RunWith(Parameterized.class)
public class Execution {

	// BEGIN CONSTANTS -->

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

	/**
	 * The level at which all loggers should log
	 */
	private static final Level LOG_LEVEL = Level.ALL;

	private static final String[] EXCLUDES = new String[] { "cSploit-android", "powermock-powermock", "alibaba-atlas",
			"Netflix-zuul", "apache-beam", "apache-kafka", "orhanobut-logger", "apache-groovy", "orhanobut-hawk"};
	private static final String[] INCLUDES = new String[] {};

	/**
	 * Path to a file containing one exclude per line. Successfully processed
	 * projects are automatically appended to this file!
	 */
	private static final File EXCLUDES_FILE = new File("excludes.txt");
	// <-- END CONSTANTS

	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = Logger.getLogger(Execution.class);

	private static MetricCalculation calculator;

	private ProjectConfiguration config;

	public Execution(String projectName, ProjectConfiguration config) {
		this.config = config;
	}

	@Parameters(name = "Analyze: {0}")
	public static Collection<Object[]> collectProjects() throws IOException, ProcessingException {
		Collection<String> excludedProjectNames = getExcludes();

		File projectsReleaseDataJSON = new File(ProjectsOutputCreator.projectsDataOutputFilePath);

		JsonNode projectsJsonData = JsonLoader.fromFile(projectsReleaseDataJSON);
		if (VALIDATE_JSON) {
			if (!checkDocument(projectsJsonData)) {
				throw new IllegalArgumentException("The given JSON file doesn't comply with the JSON Schema!");
			}
		}

		ArrayNode projects = (ArrayNode) projectsJsonData.get("projects");
		if (projects.size() == 0) {
			throw new IllegalArgumentException("There are no projects in the JSON documnet!");
		}

		int projectCounter = 0;
		int skipedProjects = 0;
		List<Object[]> configs = new ArrayList<>(Math.min(MAX_NUMBER_OF_PROJECTS, projects.size()));
		for (JsonNode project : projects) {
			projectCounter++;
			if (OFFSET_FOR_PROJECTS >= projectCounter) {
				continue;
			}
			if (projectCounter > MAX_NUMBER_OF_PROJECTS + OFFSET_FOR_PROJECTS + skipedProjects) {
				break;
			}

			String productName = project.get("productName").asText();
			String vendorName = project.get("vendorName").asText();

			boolean skip = false;
			String name = vendorName + "-" + productName;
			for (String exclude : excludedProjectNames) {
				if (exclude.equals(name)) {
					skip = true;
					break;
				}
			}
			if (skip) {
				skipedProjects++;
				continue;
			}

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
	 * Searches all excluded projects
	 * 
	 * @return The names of the excluded projects
	 * @throws IOException
	 */
	private static Collection<String> getExcludes() throws IOException {
		Collection<String> excludes = new HashSet<>();
		if (EXCLUDES_FILE.exists()) {
			excludes.addAll(Files.readAllLines(EXCLUDES_FILE.toPath(), Charset.defaultCharset()));
		}
		excludes.removeAll(Arrays.asList(INCLUDES));
		excludes.addAll(Arrays.asList(EXCLUDES));
		return excludes;
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

		JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();
		report = validator.validate(schemaNode, projectsJsonData);
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
		boolean success = calculator.calculate(config);
		if (!success) {
			fail(calculator.getLastErrors().stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]")));
		} else {
			addToExcludeFile();
		}
		assertTrue(success);
	}

	/**
	 * Adds the current project to the excludes file
	 * 
	 * @return true, iff the project has been added
	 */
	private boolean addToExcludeFile() {
		final String name = config.getVendorName() + "-" + config.getProductName();
		try {
			Files.write(EXCLUDES_FILE.toPath(), (name + '\n').getBytes(),
					EXCLUDES_FILE.exists() ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		} catch (IOException e) {
			LOGGER.log(Level.WARN, "Couldn't append project to excludes: " + name);
			return false;
		}
		return true;
	}

	/**
	 * Clean the repository folder before test execution and initialize metric
	 * calculation
	 * 
	 * @throws InitializationError If the cleanup or initialization failed
	 */
	@BeforeClass
	public static void initialize() throws InitializationError {
		Logger.getRootLogger().setLevel(LOG_LEVEL);
		if (!MetricCalculation.cleanupRepositories()) {
			throw new InitializationError("Couldn't clean repositories");
		}
		try {
			calculator = new MetricCalculation();
		} catch (IOException e) {
			throw new InitializationError(e);
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
