package metric.correlation.analysis.calculation.impl;

import static metric.correlation.analysis.calculation.impl.SourceMeterMetrics.MetricKeysImpl.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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

	public SourceMeterMetrics() throws MetricCalculatorInitializationException {
		String sourcemeter = System.getenv(ENV_VARIABLE_NAME);
		if (sourcemeter == null) {
			throw new MetricCalculatorInitializationException("SourceMeterJava environment variable not set!");
		} else {
			sourceMeterExecutable = new File(sourcemeter);
			if (!sourceMeterExecutable.exists()) {
				throw new MetricCalculatorInitializationException(
						"SourceMeterJava executable not found at: \"" + sourcemeter + "\"!");
			}
		}
		try {
			tmpResultDir = Files.createTempDirectory("SourceMeter").toFile();
		} catch (IOException e1) {
			throw new MetricCalculatorInitializationException(e1);
		}
	}

	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version) {
		File projectLocation = project.getProject().getLocation().toFile();

		lastProjectName = project.getProject().getName();
		String cmd = sourceMeterExecutable + " -projectName=" + lastProjectName + //$NON-NLS-1$
				" -projectBaseDir=" + projectLocation.getAbsolutePath() + //$NON-NLS-1$
				" -resultsDir=" + tmpResultDir.getAbsolutePath(); //$NON-NLS-1$

		try {
			return CommandExecuter.executeCommand(projectLocation, cmd);
		} catch (UnsupportedOperationSystemException e) {
			return false;
		}
	}

	@Override
	public LinkedHashMap<String, Double> getResults() {
		if (lastProjectName == null) {
			throw new IllegalStateException("The calculateMetrics() operation hasn't been executed!");
		}
		File[] sourcemeterOutputFolder = new File(new File(tmpResultDir.getAbsolutePath(), lastProjectName), "java") //$NON-NLS-1$
				.listFiles();
		if (sourcemeterOutputFolder == null || sourcemeterOutputFolder.length == 0) {
			throw new IllegalStateException("There are no output files!");
		} else {
			DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
			dfs.setDecimalSeparator('.');
			DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
			
			LinkedHashMap<String, Double> metricMap = new LinkedHashMap<String, Double>();
			List<String> names = null;
			File metrics = new File(sourcemeterOutputFolder[0], lastProjectName + "-Class.csv"); // $NON-NLS-1$
			try (BufferedReader fileReader = new BufferedReader(new FileReader(metrics))) {
				String line = fileReader.readLine();
				if (line == null) {
					throw new IllegalStateException("Sourcemeter metric file is empty");
				}
				names = Arrays.asList(line.substring(1, line.length() - 1).split("\",\"")); //$NON-NLS-1$
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, e.getMessage(), e);
				throw new IllegalStateException("Sourcemeter metric file cannot be read");
			}
			
			Collection<? extends String> metricKeys = getMetricKeys();
			for (String metricName : metricKeys) {
				if(LOC_PER_CLASS.toString().equals(metricName)) {
					//LOC_PER_CLASS accesses the same entry as LLOC
					continue;
				}
				List<Double> classValues = new ArrayList<Double>();
				int metricIndex = names.indexOf(metricName);
				try {
					String[] files = { lastProjectName + "-Class.csv", lastProjectName + "-Enum.csv" };
					for (String f : files) {
						metrics = new File(sourcemeterOutputFolder[0], f); // $NON-NLS-1$
						try (BufferedReader metricReader = new BufferedReader(new FileReader(metrics))) {
							String mLine = metricReader.readLine(); // We want to skip the first line
							while ((mLine = metricReader.readLine()) != null) {
								String value = mLine.substring(1, mLine.length() - 1).split("\",\"")[metricIndex]; //$NON-NLS-1$
								classValues.add(Double.parseDouble(value));
							}
						}
					}

					double sum = 0;
					for (int i = 0; i < classValues.size(); i++) {
						sum += classValues.get(i);
					}
					double average = Double.parseDouble(dFormat.format(sum / classValues.size()));
					if (LLOC.toString().equals(metricName)) {
						metricMap.put(LLOC.toString(), Double.parseDouble(dFormat.format(sum)));
						metricMap.put(LOC_PER_CLASS.toString(), average);
					}
					else {
						metricMap.put(metricName, average);
					}

				} catch (IOException e) {
					LOGGER.log(Level.ERROR, e.getMessage(), e);
				}
			}

			return metricMap;
		}
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Arrays.asList(MetricKeysImpl.values()).stream().map(Object::toString).collect(Collectors.toList());
	}

	/**
	 * The keys of the SourceMeter metrics
	 * 
	 * @author speldszus
	 *
	 */
	enum MetricKeysImpl {
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
