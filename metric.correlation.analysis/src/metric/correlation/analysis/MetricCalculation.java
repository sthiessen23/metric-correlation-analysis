package metric.correlation.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.importer.ImportException;
import org.gravity.eclipse.importer.NoRootFolderException;
import org.gravity.eclipse.importer.ProjectImport;
import org.gravity.eclipse.importer.gradle.GradleImport;
import org.gravity.eclipse.importer.maven.MavenImport;
import org.gravity.eclipse.io.FileUtils;
import org.gravity.eclipse.io.GitCloneException;
import org.gravity.eclipse.io.GitTools;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;

import com.google.common.io.Files;

import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.calculation.impl.AndrolyzeMetrics;
import metric.correlation.analysis.calculation.impl.CVEMetrics;
import metric.correlation.analysis.calculation.impl.HulkMetrics;
import metric.correlation.analysis.calculation.impl.SourceMeterMetrics;
import metric.correlation.analysis.calculation.impl.VersionMetrics;
import metric.correlation.analysis.calculation.impl.VulnerabilitiesPerKLOCMetrics;
import metric.correlation.analysis.configuration.ProjectConfiguration;
import metric.correlation.analysis.database.MongoDBHelper;
import metric.correlation.analysis.io.Storage;
import metric.correlation.analysis.io.VersionHelper;
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
	private static final Collection<Class<? extends IMetricCalculator>> METRIC_CALCULATORS = Arrays.asList(
			HulkMetrics.class, 
			SourceMeterMetrics.class, VulnerabilitiesPerKLOCMetrics.class, AndrolyzeMetrics.class, CVEMetrics.class,
			VersionMetrics.class);

	// END
	// Don't edit below here

	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = LogManager.getLogger(MetricCalculation.class);

	private final SortedSet<IMetricCalculator> calculators;

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
	private File outputFolder;

	private File errorFile;

	private List<String> successFullVersions;
	private List<String> notApplicibleVersions;
	/**
	 * Initialized the list of calculators
	 * 
	 * @throws IOException If the results file cannot be initialized
	 */
	public MetricCalculation() throws IOException {

		// Get the time stamp of this run
		this.timestamp = new SimpleDateFormat("yyyy-MM-dd_HH_mm").format(new Date());

		// Initialize the metric calculators
		FileAppender initLogger = addLogAppender("initialization");
		this.calculators = new TreeSet<IMetricCalculator>();
		for (Class<? extends IMetricCalculator> clazz : METRIC_CALCULATORS) {
			try {
				calculators.add(clazz.getConstructor().newInstance());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				LOGGER.log(Level.WARN, e.getMessage(), e);
			}
		}

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
		dropLogAppender(initLogger);
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
		successFullVersions = new ArrayList<>();
		notApplicibleVersions = new ArrayList<>();
		
		// Create a project specific file logger
		FileAppender fileAppender = addLogAppender(config);

		// Reset previously recored errors
		errors = new HashSet<>();

		// Clone the project
		boolean success = true;
		try (GitTools git = new GitTools(config.getGitUrl(), REPOSITORIES, true, true)){
			String productName = config.getProductName();
			String vendorName = config.getVendorName();

			File srcLocation = new File(REPOSITORIES, productName);

			// Calculate metrics for each commit of the project configuration
			for (Entry<String, String> entry : config.getVersionCommitIdPairs()) {
				String commitId = entry.getValue();
				String version = entry.getKey();
				LOGGER.log(Level.INFO, "\n\n\n#############################");
				LOGGER.log(Level.INFO, "### " + timestamp + " ###");
				LOGGER.log(Level.INFO, "#############################");
				LOGGER.log(Level.INFO, "Checkingout commit : " + commitId);
				LOGGER.log(Level.INFO, "#############################\n");

				// Checkout the specific commit
				if (!git.changeVersion(commitId)) {
					success = false;
					errors.add("change commit");
					LOGGER.log(Level.WARN, "Skipped commit: " + commitId);
					continue;
				}
				FileUtils.recursiveDelete(new File(RESULTS, "SourceMeter"));

				// Calculate all metrics
				LOGGER.log(Level.INFO, "Start metric calculation");
				try {
					success &= calculateMetrics(productName, vendorName, version, srcLocation);
				} catch (Exception e) {
					LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
					success = false;
				}
				if(success) {
					successFullVersions.add(version);
				}
				else {
					LOGGER.log(Level.ERROR, "\n### METRIC CALCULATION FAILED ###\nproject: "+productName+"\nvendor: "+vendorName+"\nversion: "+version+"\n### ###");
				}
			}
		} catch (GitCloneException | IOException e) {
			LOGGER.log(Level.ERROR, e);
			success = false;
		}

		// Drop the project specific file logger
		dropLogAppender(fileAppender);
		return success;
	}

	public IJavaProject importProject(File src, boolean ignoreBuildErrors) {
		LOGGER.log(Level.INFO, "Importing  project to Eclipse workspace");
		if (!src.exists()) {
			errors.add("src folder does not exist");
			return null;
		}
		ProjectImport projectImport;
		if (Arrays.stream(src.listFiles()).anyMatch(f -> f.getName().contentEquals("build.gradle"))) {
			try {
				projectImport = new GradleImport(src, ignoreBuildErrors);
			} catch (IOException | ImportException e) {
				errors.add("new GradleImport()");
				return null;
			}
		} else if (Arrays.stream(src.listFiles()).anyMatch(f -> f.getName().contentEquals("pom.xml"))) {
			try {
				projectImport = new MavenImport(src, ignoreBuildErrors);
			} catch (ImportException e) {
				errors.add("new MavenImport()");
				return null;
			}
		} else {
			errors.add("not maven or gradle project");
			return null;
		}

		IJavaProject project;

		try {
			project = projectImport.importProject(new NullProgressMonitor());
		} catch (NoRootFolderException e) {
			errors.add(projectImport.getClass().getSimpleName());
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return null;
		} catch (ImportException e) {
			errors.add(projectImport.getClass().getSimpleName());
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			Thread.currentThread().interrupt();
			return null;
		}
		return project;
	}
	/**
	 * Creates a new project specific logger with an new output file for a project
	 * configuration
	 * 
	 * @param config The project configuration
	 * @return The project specific logger
	 */
	private FileAppender addLogAppender(ProjectConfiguration config) {
		return addLogAppender(config.getVendorName() + '-' + config.getProductName());
	}

	/**
	 * Creates a log appender which appends to a file with the given name
	 * 
	 * @param name The file name
	 * @return The logger
	 */
	private FileAppender addLogAppender(String name) {
		FileAppender fileAppender = null;
		try {
			fileAppender = new FileAppender(new PatternLayout("%d %-5p [%c{1}] %m%n"),
					"logs/" + timestamp + '/' + name + ".txt");
			fileAppender.setThreshold(Level.ALL);
			fileAppender.activateOptions();
			Logger.getRootLogger().addAppender(fileAppender);
		} catch (IOException e) {
			LOGGER.log(Level.WARN, "Adding file appender failed!");
		}
		return fileAppender;
	}

	/**
	 * Drops the log appender
	 * 
	 * @param fileAppender the logger
	 */
	private void dropLogAppender(FileAppender fileAppender) {
		if (fileAppender != null) {
			Logger.getRootLogger().removeAppender(fileAppender);
		}
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
		// Import the sourcecode as maven or gradle project
		IJavaProject project = importProject(src, true);
		if (project == null) {
			return false;
		}

		// Calculate all metrics
		boolean success = true;
		HashMap<String, String> results = new HashMap<>();
		for (IMetricCalculator calc : calculators) {
			LOGGER.log(Level.INFO, "Execute metric calculation: " + calc.getClass().getSimpleName());
			try {
				if (calc.calculateMetric(project, productName, vendorName, version,
						Collections.unmodifiableMap(results))) {
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
			try (MongoDBHelper dbHelper = new MongoDBHelper()) {
				dbHelper.storeMetrics(results);
			} catch (Exception e) {
				LOGGER.error("could not store results in database");
				LOGGER.log(Level.ERROR, e.getStackTrace());
			}
			for (Entry<String, String> entry : results.entrySet()) {
				if (!allMetricResults.containsKey(entry.getKey())) {
					allMetricResults.put(entry.getKey(), new LinkedList<>());
				}
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
	 * @return true iff the results are plausible
	 */
	private boolean plausabilityCheck(IMetricCalculator calc) {
		for (String value : calc.getResults().values()) {
			if (value == null || value.isEmpty() || Double.toString(Double.NaN).equals(value)) {
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

		// Find indexes of all newest versions
		List<String> productNames = allMetricResults
				.get(metric.correlation.analysis.calculation.impl.VersionMetrics.MetricKeysImpl.PRODUCT.toString());
		List<String> versions = allMetricResults
				.get(metric.correlation.analysis.calculation.impl.VersionMetrics.MetricKeysImpl.VERSION.toString());

		Map<String, Integer> productToNewestIndex = new HashMap<>();
		Map<String, String> productToNewestVersion = new HashMap<>();

		for (int index = 0; index < versions.size(); index++) {

			String product = productNames.get(index);
			String newVersion = versions.get(index);
			if (productToNewestVersion.containsKey(product)) {

				String prevVersion = productToNewestVersion.get(product);

				if (VersionHelper.compare(prevVersion, newVersion) == -1) {
					productToNewestVersion.put(product, newVersion);
					productToNewestIndex.put(product, index);
				}

			} else {
				productToNewestVersion.put(product, newVersion);
				productToNewestIndex.put(product, index);
			}
		}

		// Remove the version key completely
		// auch hier die andere liste nutzen
		allMetricResults.remove(VersionMetrics.MetricKeysImpl.PRODUCT.toString());
		allMetricResults.remove(VersionMetrics.MetricKeysImpl.VENDOR.toString());
		allMetricResults.remove(VersionMetrics.MetricKeysImpl.VERSION.toString());

		// Add them to newestVersionOnly
		for (Entry<String, List<String>> entry : allMetricResults.entrySet()) {
			List<Double> newestMetrics = new LinkedList<Double>();
			for (Integer newVersionIndex : productToNewestIndex.values()) {
				newestMetrics.add(Double.valueOf(entry.getValue().get(newVersionIndex.intValue())));
			}
			newestVersionOnly.put(entry.getKey(), newestMetrics);
		}

		try {
			if (newestVersionOnly.size() > 1) {
				new StatisticExecuter().calculateStatistics(newestVersionOnly, outputFolder);
			} else {
				LOGGER.log(Level.WARN, "Skipped calculation of correlation matrix");
			}
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
			return false;
		}
		return true;
	}

	public List<String> getSuccessFullVersions() {
		return successFullVersions;
	}

	public List<String> getNotApplicibleVersions() {
		return notApplicibleVersions;
	}
}
