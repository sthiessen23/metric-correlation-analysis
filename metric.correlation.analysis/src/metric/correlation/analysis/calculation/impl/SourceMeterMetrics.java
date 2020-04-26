package metric.correlation.analysis.calculation.impl;

import static metric.correlation.analysis.calculation.impl.SourceMeterMetrics.MetricKeysImpl.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.AccessMode;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;

import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.calculation.MetricCalculatorInitializationException;
import metric.correlation.analysis.commands.CommandExecuter;

public class SourceMeterMetrics implements IMetricCalculator {

	private static final String ENV_VARIABLE_NAME = "SOURCE_METER_JAVA"; //$NON-NLS-1$

	private static final Logger LOGGER = Logger.getLogger(SourceMeterMetrics.class);

	private final File sourceMeterExecutable;
	private final File tmpResultDir;

	private String lastProjectName;
	private LinkedHashMap<String, String> lastResults;

	public SourceMeterMetrics() throws MetricCalculatorInitializationException {
		String sourcemeter = System.getenv(ENV_VARIABLE_NAME);
		if (sourcemeter == null) {
			throw new MetricCalculatorInitializationException("SourceMeterJava environment variable not set!");
		} else {
			sourceMeterExecutable = new File(sourcemeter);
			try {
				sourceMeterExecutable.toPath().getFileSystem().provider().checkAccess(sourceMeterExecutable.toPath(),
						AccessMode.EXECUTE);
			} catch (IOException e) {
				throw new MetricCalculatorInitializationException(e);
			}
		}
		try {
			tmpResultDir = Files.createTempDirectory("SourceMeter").toFile();
		} catch (IOException e1) {
			throw new MetricCalculatorInitializationException(e1);
		}
	}

	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version,
			final Map<String, String> map) {
		File projectLocation = project.getProject().getLocation().toFile();

		lastProjectName = project.getProject().getName();
		String cmd = sourceMeterExecutable + " -projectName=" + lastProjectName + //$NON-NLS-1$
				" -projectBaseDir=" + projectLocation.getAbsolutePath() + //$NON-NLS-1$
				" -resultsDir=" + tmpResultDir.getAbsolutePath(); //$NON-NLS-1$

		try {
			if (!CommandExecuter.executeCommand(projectLocation, cmd)) {
				return false;
			}
		} catch (UnsupportedOperationSystemException e) {
			LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
			return false;
		}
		return calculateResults();

	}

	@Override
	public LinkedHashMap<String, String> getResults() {
		if (lastResults == null) {
			throw new IllegalStateException("The calculateMetrics() operation hasn't been executed or failed!");
		}
		return lastResults;
	}

	private boolean calculateResults() {
		File sourceMeterOutputFolder = getOutputFolder();
		DecimalFormat dFormat = getFormatter();

		lastResults = new LinkedHashMap<>();
		List<Map<String, String>> classContent = new LinkedList<>();
		try {
			parseMetricFile(new File(sourceMeterOutputFolder, lastProjectName + "-Class.csv"), classContent);
			parseMetricFile(new File(sourceMeterOutputFolder, lastProjectName + "-Enum.csv"), classContent);
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		}
		Collection<? extends String> metricKeys = getMetricKeys();
		for (String metricName : metricKeys) {
			if (LOC_PER_CLASS.toString().equals(metricName)) {
				// LOC_PER_CLASS accesses the same entry as LLOC
				continue;
			}
			double sum = 0;
			for (Map<String, String> valueMap : classContent) {
				sum += Double.parseDouble(valueMap.get(metricName));
			}
			double average = Double.parseDouble(dFormat.format(sum / classContent.size()));
			if (metricName.equals(LLOC.toString())) {
				lastResults.put(LLOC.toString(), dFormat.format(sum));
				lastResults.put(LOC_PER_CLASS.toString(), Double.toString(average));
			} else {
				lastResults.put(metricName, Double.toString(average));
			}
		}
		return true;
	}

	private void parseMetricFile(File metrics, List<Map<String, String>> content) throws IOException {
		Map<String, Integer> metricIndex = new HashMap<>();
		if (!metrics.exists()) {
			throw new IllegalStateException("File to parse does not exist: " + metrics.getAbsolutePath());
		}
		BufferedReader fileReader = new BufferedReader(new FileReader(metrics));
		initIndex(metricIndex, fileReader.readLine());
		String mLine;
		while ((mLine = fileReader.readLine()) != null) {
			String[] cvsValues = getCsvValues(mLine);
			Map<String, String> metricMap = new HashMap<>();
			for (Entry<String, Integer> entry : metricIndex.entrySet()) {
				metricMap.put(entry.getKey(), cvsValues[entry.getValue()]);
			}
			content.add(metricMap);
		}
		fileReader.close();
	}

	private void initIndex(Map<String, Integer> metricIndex, String line) {
		Collection<String> metricKeys = getMetricKeys();
		String[] sourceMeterKeys = getCsvValues(line);
		for (int i = 0; i < sourceMeterKeys.length; i++) {
			if (metricKeys.contains(sourceMeterKeys[i])) {
				metricIndex.put(sourceMeterKeys[i], i);
			}
		}
	}

	private String[] getCsvValues(String line) {
		return line.substring(1, line.length() - 1).split("\",\"");
	}

	private File getOutputFolder() {
		File[] outputFolderDir = new File(new File(tmpResultDir.getAbsolutePath(), lastProjectName), "java") //$NON-NLS-1$
				.listFiles();
		if (outputFolderDir == null || outputFolderDir.length == 0) {
			throw new IllegalStateException("There are no output files!");
		}
		return outputFolderDir[0];
	}

	private DecimalFormat getFormatter() {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		return new DecimalFormat("0.00", dfs);
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Arrays.asList(MetricKeysImpl.values()).stream().map(Object::toString).collect(Collectors.toSet());
	}

	@Override
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		return Collections.emptySet();
	}

	/**
	 * The keys of the SourceMeter metrics
	 * 
	 * @author speldszus
	 *
	 */
	public enum MetricKeysImpl {
		LDC("LDC"), DIT("DIT"), LCOM("LCOM5"), CBO("CBO"), WMC("WMC"), LLOC("LLOC"), LOC_PER_CLASS("LOCpC");

		private String value;

		private MetricKeysImpl(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}
}
