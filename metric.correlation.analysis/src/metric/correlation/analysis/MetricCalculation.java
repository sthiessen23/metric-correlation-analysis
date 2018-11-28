package metric.correlation.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.importer.gradle.GradleImport;
import org.gravity.eclipse.importer.gradle.GradleImportException;
import org.gravity.eclipse.importer.gradle.NoGradleRootFolderException;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;

import com.google.common.io.Files;

import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.calculation.impl.AndrolyzeMetrics;
import metric.correlation.analysis.calculation.impl.CVEMetrics;
import metric.correlation.analysis.calculation.impl.HulkMetrics;
import metric.correlation.analysis.calculation.impl.SourceMeterMetrics;
import metric.correlation.analysis.calculation.impl.VersionMetrics;
import metric.correlation.analysis.configuration.ProjectConfiguration;
import metric.correlation.analysis.io.FileUtils;
import metric.correlation.analysis.io.GitCloneException;
import metric.correlation.analysis.io.GitTools;
import metric.correlation.analysis.io.Storage;
import metric.correlation.analysis.statistic.StatisticExecuter;

/**
 * A class for executing different metric calculations of git java projects and
 * performing statistics on the results
 * 
 * @author speldszus
 *
 */
public class MetricCalculation {

	// BEGIN: Configuration variables
	/**
	 * The location where the results should be stored
	 */
	private static final File RESULTS = new File("results");

	/**
	 * The location where the git repositories should be cloned to
	 */
	private static final File REPOSITORIES = new File("repositories");

	/**
	 * The classes of the calculators which should be executed
	 */
	private static final Collection<Class<? extends IMetricCalculator>> METRIC_CALCULATORS = Arrays
			.asList(HulkMetrics.class, SourceMeterMetrics.class, AndrolyzeMetrics.class, CVEMetrics.class);

	// END
	// Don't edit below here

	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = LogManager.getLogger(MetricCalculation.class);

	private final Collection<IMetricCalculator> calculators;

	private final String timestamp;
	private Set<String> errors;
	private Storage storage;

	/**
	 * A mapping from metric names to all values for this metric
	 */
	private final LinkedHashMap<String, List<String>> allMetricResults;

	/**
	 * The folder in which the results are stored
	 */
	private static File outputFolder;

	private File errorFile;

	/**
	 * Initialized the list of calculators
	 * 
	 * @throws IOException If the results file cannot be initialized
	 */
	public MetricCalculation() throws IOException {
		// Initialize the metric calculators
		this.calculators = new ArrayList<IMetricCalculator>(METRIC_CALCULATORS.size() + 1);
		this.calculators.add(new VersionMetrics());
		for (Class<? extends IMetricCalculator> clazz : METRIC_CALCULATORS) {
			try {
				calculators.add(clazz.getConstructor().newInstance());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				LOGGER.log(Level.WARN, e.getMessage(), e);
			}
		}

		// Get the time stamp of this run
		this.timestamp = new SimpleDateFormat("YYYY-MM-dd_HH_mm").format(new Date());

		this.allMetricResults = new LinkedHashMap<>();
		
		// Collect all metric keys
		Set<String> metricKeys = new HashSet<String>();
		for (IMetricCalculator calculator : calculators) {
			metricKeys.addAll(calculator.getMetricKeys());
		}

		// Initialize the metric results
		for (String metricKey : metricKeys) {
			this.allMetricResults.put(metricKey, new LinkedList<>());
		}

		outputFolder = new File(RESULTS, "Results-" + timestamp);
		storage = new Storage(new File(outputFolder, "results.csv"), metricKeys);
		errorFile = new File(outputFolder, "errors.csv");
		Files.write("vendor,product,version,errors\n".getBytes(), errorFile);
	}

	/**
	 * The main method for calculating metrics for multiple versions of multiple
	 * projects. This method has to be called from a running eclipse workspace!
	 * 
	 * @param configurations The project configurations which should be considered
	 * @throws UnsupportedOperationSystemException
	 */
	public boolean calculateAll(Collection<ProjectConfiguration> configurations)
			throws UnsupportedOperationSystemException {
		for (ProjectConfiguration config : configurations) {
			if (!calculate(config)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The main method for calculating metrics for multiple versions of a project.
	 * This method has to be called from a running eclipse workspace!
	 * 
	 * @param configurations The project configuration which should be considered
	 */
	public boolean calculate(ProjectConfiguration config) {
		// Create a project specific file logger
		FileAppender fileAppender = addLogAppender(config);

		// Reset previously recored errors
		errors = new HashSet<>();

		// Clone the project
		boolean success = clone(config);
		if (success) {
			String productName = config.getProductName();
			String vendorName = config.getVendorName();

			File srcLocation = new File(REPOSITORIES, productName);

			// Calculate metrics for each commit of the project configuration
			for (Entry<String, String> entry : config.getVersionCommitIdPairs()) {
				String commitId = entry.getValue();
				LOGGER.log(Level.INFO, "\n\n\n#############################");
				LOGGER.log(Level.INFO, "### " + timestamp + " ###");
				LOGGER.log(Level.INFO, "#############################");
				LOGGER.log(Level.INFO, "Checkingout commit : " + commitId);
				LOGGER.log(Level.INFO, "#############################\n");

				try {
					// Checkout the specific commit
					if (!GitTools.changeVersion(srcLocation, commitId)) {
						LOGGER.log(Level.WARN, "Skipped commit: " + commitId);
						continue;
					}
				} catch (UnsupportedOperationSystemException e) {
					LOGGER.log(Level.ERROR, e.getMessage(), e);
					break;
				}
				FileUtils.recursiveDelete(new File(RESULTS, "SourceMeter"));

				// Calculate all metrics
				LOGGER.log(Level.INFO, "Start metric calculation");
				success &= calculateMetrics(productName, vendorName, entry.getKey(), srcLocation);
			}
		}

		// Drop the project specific file logger
		if (fileAppender != null) {
			Logger.getRootLogger().removeAppender(fileAppender);
		}
		return success;
	}

	/**
	 * Creates a new project specific logger with an new output file for a project
	 * configuration
	 * 
	 * @param config The project configuration
	 * @return The project specific logger
	 */
	private FileAppender addLogAppender(ProjectConfiguration config) {
		FileAppender fileAppender = null;
		try {
			fileAppender = new FileAppender(new PatternLayout("%d %-5p [%c{1}] %m%n"),
					"logs/" + timestamp + '/' + config.getVendorName() + '-' + config.getProductName() + ".txt");
			fileAppender.setThreshold(Level.ALL);
			fileAppender.activateOptions();
			Logger.getRootLogger().addAppender(fileAppender);
		} catch (IOException e) {
			LOGGER.log(Level.WARN, "Adding file appender failed!");
		}
		return fileAppender;
	}

	/**
	 * Clones the repository
	 * 
	 * @param config The project configuration
	 * @return true, iff the repository has been cloned successfully
	 */
	private boolean clone(ProjectConfiguration config) {
		String gitUrl = config.getGitUrl();
		try {
			LOGGER.log(Level.INFO, "Cloning repository: " + gitUrl);
			if (!GitTools.gitClone(gitUrl, REPOSITORIES, true)) {
				errors.add("gitClone()");
				return false;
			}
		} catch (GitCloneException | UnsupportedOperationSystemException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			errors.add(e.getLocalizedMessage());
			return false;
		}
		return true;
	}

	/**
	 * Calculate the correlation metrics
	 *
	 * @param productName The name of the software project
	 * @param vendorName  The name of the projects vendor
	 * @param version     The version which should be inspected
	 * @param src         The location of the source code
	 * @return true if everything went okay, otherwise false
	 */
	private boolean calculateMetrics(String productName, String vendorName, String version, File src) {
		// Import the sourcecode as gradle project
		LOGGER.log(Level.INFO, "Importing Gradle project to Eclipse workspace");
		GradleImport gradleImport;

		try {
			gradleImport = new GradleImport(src);
		} catch (NoGradleRootFolderException | IOException e) {
			errors.add("new GradleImport()");
			return false;
		}

		IJavaProject project;

		try {
			project = gradleImport.importGradleProject(true, new NullProgressMonitor());
		} catch (NoGradleRootFolderException e) {
			errors.add(gradleImport.getClass().getSimpleName());
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		} catch (GradleImportException e) {
			errors.add(gradleImport.getClass().getSimpleName());
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			Thread.currentThread().interrupt();
			return false;
		}
		if (project == null) {
			errors.add("Import as Eclipse project failed");
			return false;
		}

		// Calculate all metrics
		boolean success = true;
		HashMap<String, String> results = new HashMap<>();
		for (IMetricCalculator calc : calculators) {
			LOGGER.log(Level.INFO, "Execute metric calculation: " + calc.getClass().getSimpleName());
			try {
				if (calc.calculateMetric(project, productName, vendorName, version)) {
					results.putAll(calc.getResults());
					success &= plausabilityCheck(calc);
				} else {
					errors.add(calc.getClass().getSimpleName());
					success = false;
				}
			} catch (Exception e) {
				success = false;
				errors.add(calc.getClass().getSimpleName());
				LOGGER.log(Level.ERROR, "A detection failed with an Exception: " + e.getMessage(), e);
			}
		}

		// Store all results in a csv file
		if (!storage.writeCSV(productName, results)) {
			LOGGER.log(Level.ERROR, "Writing results for \"" + productName + "\" failed!");
			errors.add("Writing results");
			success = false;
		}

		// If all metrics have been calculated successfully add them to the metric
		// results
		if (success) {
			for (Entry<String, String> entry : results.entrySet()) {
				allMetricResults.get(entry.getKey()).add(entry.getValue());
			}
		} else {
			try (FileWriter writer = new FileWriter(errorFile, true)) {
				writer.append(vendorName);
				writer.append(',');
				writer.append(productName);
				writer.append(',');
				writer.append(version);
				writer.append(',');
				writer.append(errors.stream().collect(Collectors.joining(" - ")));
				writer.append('\n');
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
			}
		}

		return success;
	}

	/**
	 * Checks if the results of the metric calculator are plausible
	 * 
	 * @param calc The executed metric calculator
	 * @return true iff the results ar plausible
	 */
	private boolean plausabilityCheck(IMetricCalculator calc) {
		for (String value : calc.getResults().values()) {
			if (value == null || Double.toString(Double.NaN).equals(value)) {
				errors.add("Values not plausible: " + calc.getClass().getSimpleName());
				return false;
			}
		}
		return true;
	}

	/**
	 * Cleans the repositories folder
	 * 
	 * @return true, iff everything has been deleted
	 */
	public static boolean cleanupRepositories() {
		return FileUtils.recursiveDelete(REPOSITORIES);
	}

	/**
	 * Returns the errors of the last run
	 * 
	 * @return A Set of error messages
	 */
	public Set<String> getLastErrors() {
		return errors;
	}

	/**
	 * Executes the statistic calculation on all projects discovered with this
	 * instance
	 * 
	 * @return true, iff the results have been stored successfully
	 */
	public boolean performStatistics() {
		LinkedHashMap<String, List<Double>> newestVersionOnly = new LinkedHashMap<>();
		for(Entry<String, List<String>> enty : allMetricResults.entrySet()) {
			//TODO: Only add newest version from metricResults of a project to the newestVersionOnly Map	
		}
		try {
			new StatisticExecuter().calculateStatistics(newestVersionOnly, outputFolder);
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
			return false;
		}
		return true;
	}

}
