package metric.correlation.analysis.calculation.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.os.OperationSystem;

import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.calculation.MetricCalculatorInitializationException;

public class SourceMeterMetrics implements IMetricCalculator {

	private static final String LDC = "LDC";

	private static final String DIT = "DIT";

	private static final String LCOM = "LCOM5";

	private static final String CBO = "CBO";

	private static final String WMC = "WMC";

	private static final String LLOC = "LLOC";

	private static final String ENV_VARIABLE_NAME = "SOURCE_METER_JAVA"; //$NON-NLS-1$

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
		File in = project.getProject().getLocation().toFile();

		lastProjectName = project.getProject().getName();
		String cmd = sourceMeterExecutable + " -projectName=" + lastProjectName + //$NON-NLS-1$
				" -projectBaseDir=" + in.getAbsolutePath() + //$NON-NLS-1$
				" -resultsDir=" + tmpResultDir.getAbsolutePath(); //$NON-NLS-1$

		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			switch (OperationSystem.getCurrentOS()) {
			case WINDOWS:
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
				break;
			case LINUX:
				process = run.exec(cmd);
				break;
			default:
				System.err.println("Program is not compatibel with the Operating System");
				return false;
			}

			BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = stream_reader.readLine()) != null) {
				System.out.println("> " + line); //$NON-NLS-1$
			}
			stream_reader.close();
			process.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public LinkedHashMap<String, Double> getResults() {
		if (lastProjectName == null) {
			throw new IllegalStateException("The calculateMetrics() operation hasn't been executed!");
		}
		LinkedHashMap<String, Double> metricMap = new LinkedHashMap<String, Double>();

		String[] metricNames = { LLOC, WMC, CBO, LCOM, DIT, LDC };

		File[] sourcemeterOutputFolder = new File(new File(tmpResultDir.getAbsolutePath(), lastProjectName), "java") //$NON-NLS-1$
				.listFiles();
		if (sourcemeterOutputFolder == null) {
			for (String name : metricNames) {
				metricMap.put(name, -1.0);
			}
			return metricMap;
		}

		if (sourcemeterOutputFolder.length > 0) {
			File metrics = new File(sourcemeterOutputFolder[0], lastProjectName+"-Class.csv"); // $NON-NLS-1$
			try (BufferedReader fileReader = new BufferedReader(new FileReader(metrics))){
				String line = fileReader.readLine();
				if (line == null) {
					fileReader.close();
					System.err.println("Sourcemeter metric file is empty");
				}
				String[] names = line.substring(1, line.length() - 1).split("\",\""); //$NON-NLS-1$

				List<Double> classValues = new ArrayList<Double>();
				int llocIndex = 0;
				int llocAll = 0;
				for (String metricName : metricNames) {

					int metricIndex = Arrays.asList(names).indexOf(metricName);

					try {
						String[] files = { lastProjectName+"-Class.csv", lastProjectName+"-Enum.csv" };
						boolean temp = false;
						for (String f : files) {
							metrics = new File(sourcemeterOutputFolder[0], f); // $NON-NLS-1$
							try (BufferedReader metricReader = new BufferedReader(new FileReader(metrics))) {
								String mLine = metricReader.readLine();
								if ((mLine = metricReader.readLine()) == null && !temp) {
									for (String name : metricNames) {
										metricMap.put(name, -1.0);
									}
									metricReader.close();
									return metricMap;
								}

								temp = true;
								while (mLine != null) {
									String[] values = mLine.substring(1, mLine.length() - 1).split("\",\""); //$NON-NLS-1$
									if (metricName == LLOC) {
										classValues.add(Double.parseDouble(values[metricIndex]));

									} else {
										double lloc = Double.parseDouble(values[llocIndex]);
										classValues.add(Double.parseDouble(values[metricIndex]) * lloc);
									}
									mLine = metricReader.readLine();
								}
							}
						}
						double sum = 0;
						for (int i = 0; i < classValues.size(); i++) {
							sum += classValues.get(i);
						}
						DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
						dfs.setDecimalSeparator('.');
						DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
						if (metricName == LLOC) {
							metricMap.put(metricName, Double.parseDouble(dFormat.format(sum)));

							double average = sum / classValues.size();
							metricMap.put("LOCpC", Double.parseDouble(dFormat.format(average)));
							llocIndex = metricIndex;
							llocAll = (int) sum;

						} else {
							double average = sum / llocAll;
							metricMap.put(metricName, Double.parseDouble(dFormat.format(average)));
						}

						classValues.clear();

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return metricMap;

			} catch (IOException e) {
				System.err.println("critical error");
				for (String s : metricNames) {
					metricMap.put(s, -1.0);
				}
				return metricMap;
			}
		} else {
			for (String s : metricNames) {
				metricMap.put(s, -1.0);
			}
		}
		return metricMap;
	}
}
