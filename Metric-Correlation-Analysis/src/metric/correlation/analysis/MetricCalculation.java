package metric.correlation.analysis;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.importer.GradleImport;
import org.gravity.eclipse.importer.NoGradleRootFolderException;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;
import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.calculation.MetricCalculatorInitializationException;
import metric.correlation.analysis.calculation.impl.AndrolyzeMetrics;
import metric.correlation.analysis.calculation.impl.HulkMetrics;
import metric.correlation.analysis.calculation.impl.SourceMeterMetrics;
import metric.correlation.analysis.configuration.ProjectConfiguration;
import metric.correlation.analysis.io.FileUtils;
import metric.correlation.analysis.io.GitCloneException;
import metric.correlation.analysis.io.GitTools;
import metric.correlation.analysis.io.Storage;

public class MetricCalculation {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final File RESULTS = new File("results");
	private static final File REPOSITORIES = new File("repositories");
	// Add fourth one here
	private static final Collection<IMetricCalculator> METRIC_CALCULATORS = new ArrayList<>(3);

	public MetricCalculation() {
		METRIC_CALCULATORS.add(new HulkMetrics());
		
		try {
			METRIC_CALCULATORS.add(new SourceMeterMetrics());
		} catch (MetricCalculatorInitializationException e) {
			LOGGER.log(Level.WARN, e.getMessage(), e);
		}
		
		try {
			METRIC_CALCULATORS.add(new AndrolyzeMetrics());
		} catch (MetricCalculatorInitializationException e) {
			LOGGER.log(Level.WARN, e.getMessage(), e);
		}
	}

	/**
	 * The main method for calculating metrics for multiple versions of multiple
	 * projects. This method has to be called from a running eclipse workspace!
	 * 
	 * @param configurations
	 *            - The project configurations which should be considered
	 * @throws UnsupportedOperationSystemException
	 */
	public void calculateAll(Collection<ProjectConfiguration> configurations)
			throws UnsupportedOperationSystemException {
		
		String timestamp = new SimpleDateFormat("YYYY-MM-dd_HH_mm").format(new Date());
		
		for (ProjectConfiguration config : configurations) {
			String resultFileName = "Results-" + timestamp + ".csv";
			File resultFile = new File(RESULTS, resultFileName);

			String gitUrl = config.getGitUrl();
			
			try {
				if (!GitTools.gitClone(gitUrl, REPOSITORIES, true)) {
					return;
				}
			} catch (GitCloneException e1) {
				e1.printStackTrace();
				return;
			}

			String productName = config.getProductName();
			String vendorName = config.getVendorName();

			File srcLocation = new File(REPOSITORIES, productName);
			
			for (Entry<String, String> entry : config.getVersionCommitIdPairs()) {
				String commitId = entry.getValue();
				LOGGER.log(Level.INFO, "\n\n\n#############################");
				LOGGER.log(Level.INFO, "### " + timestamp + " ###");
				LOGGER.log(Level.INFO, "#############################");
				LOGGER.log(Level.INFO, commitId);
				LOGGER.log(Level.INFO, "#############################\n");

				GitTools.changeVersion(srcLocation, commitId);
				FileUtils.recursiveDelete(new File(RESULTS, "SourceMeter"));
				calculateMetrics(resultFile, productName, vendorName, entry.getKey(), srcLocation);
			}
		}
	}

	
	/**
	 * Calculate the correlation metrics
	 * @param results_dir
	 * @param productName 
	 * @param vendorName
	 * @param version
	 * @param src
	 * @return true if everything went okay, otherwise false
	 */
	private boolean calculateMetrics(File results_dir, String productName, String vendorName, String version,
			File src) {
		
		GradleImport gradleImport;
		
		try {
			gradleImport = new GradleImport(src);
		} catch (NoGradleRootFolderException e) {
			return false;
		}
		
		IJavaProject project;
		
		try {
			project = gradleImport.importGradleProject(new NullProgressMonitor());
		} catch (IOException | CoreException | InterruptedException | NoGradleRootFolderException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		}
		if (project == null) {
			return false;
		}

		Hashtable<String, Double> results = new Hashtable<>();
		
		for (IMetricCalculator calc : METRIC_CALCULATORS) {
			if (calc.calculateMetric(project, productName, vendorName, version)) {
				results.putAll(calc.getResults());
			}
		}

		try {
			new Storage(new File(results_dir, "results.csv"), results.keySet()).writeCSV(productName, results);
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		}
		
		results.clear();
		return true;
	}

}
