package metricTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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

public class SourceMeter implements IMetricCalculator {

	private static final String env_variable_name_srcmeter = "SOURCE_METER_JAVA"; //$NON-NLS-1$

	private final File sourceMeterExecutable;
	private final File tmpResultDir;

	public SourceMeter() throws MetricCalculatorInitializationException {
		String src_meter = System.getenv(env_variable_name_srcmeter);
		if (src_meter == null) {
			throw new MetricCalculatorInitializationException("SourceMeterJava environment variable not set!");
		} else {
			sourceMeterExecutable = new File(src_meter);
			if (!sourceMeterExecutable.exists()) {
				throw new MetricCalculatorInitializationException(
						"SourceMeterJava executable not found at: \"" + src_meter + "\"!");
			}
		}
		try {
			tmpResultDir = Files.createTempDirectory("SourceMeter").toFile();
		} catch (IOException e1) {
			throw new MetricCalculatorInitializationException(e1);
		}
	}

	@Override
	public boolean calculateMetric(IJavaProject project) {
		File in = project.getProject().getLocation().toFile();

		String cmd = sourceMeterExecutable + " -projectName=" + project.getProject().getName() + //$NON-NLS-1$
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
		LinkedHashMap<String, Double> metric_results = new LinkedHashMap<String, Double>();

		String[] metric_names = { "LLOC", "WMC", "CBO", "LCOM5", "DIT", "LDC" };

		File[] java_folder = new File(tmpResultDir.getAbsolutePath(), "java").listFiles(); //$NON-NLS-1$
		if (java_folder == null) {
			for (String name : metric_names) {
				metric_results.put(name, -1.0);
			}
			return metric_results;
		}

		if (java_folder.length > 0) {
			try {

				File metrics = new File(java_folder[0], "SrcMeter-Class.csv"); // $NON-NLS-1$
				BufferedReader file_reader = new BufferedReader(new FileReader(metrics));
				String line = file_reader.readLine();
				if (line == null) {
					file_reader.close();
					System.err.println("Sourcemeter metric file is empty");
				}
				String[] names = line.substring(1, line.length() - 1).split("\",\""); //$NON-NLS-1$

				List<Double> class_values = new ArrayList<Double>();
				int lloc_index = 0;
				int lloc_all = 0;
				for (String s : metric_names) {

					int metric_index = Arrays.asList(names).indexOf(s);

					try {
						String[] files = { "SrcMeter-Class.csv", "SrcMeter-Enum.csv" };
						boolean temp = false;
						for (String f : files) {
							metrics = new File(java_folder[0], f); // $NON-NLS-1$
							BufferedReader metric_reader = new BufferedReader(new FileReader(metrics));
							String m_line = metric_reader.readLine();
							if ((m_line = metric_reader.readLine()) == null && !temp) {
								for (String name : metric_names) {
									metric_results.put(name, -1.0);
								}
								metric_reader.close();
								return metric_results;
							}
							temp = true;
							while (m_line != null) {
								String[] values = m_line.substring(1, m_line.length() - 1).split("\",\""); //$NON-NLS-1$
								if (s == "LLOC") {
									class_values.add(Double.parseDouble(values[metric_index]));

								} else {
									double lloc = Double.parseDouble(values[lloc_index]);
									class_values.add(Double.parseDouble(values[metric_index]) * lloc);
								}
								m_line = metric_reader.readLine();
							}
							metric_reader.close();
						}
						double sum = 0;
						for (int i = 0; i < class_values.size(); i++) {
							sum += class_values.get(i);
						}
						DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
						dfs.setDecimalSeparator('.');
						DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
						if (s == "LLOC") {
							metric_results.put(s, Double.parseDouble(dFormat.format(sum)));

							double average = sum / class_values.size();
							metric_results.put("LOCpC", Double.parseDouble(dFormat.format(average)));
							lloc_index = metric_index;
							lloc_all = (int) sum;

						} else {
							double average = sum / lloc_all;
							metric_results.put(s, Double.parseDouble(dFormat.format(average)));
						}

						class_values.clear();

					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				file_reader.close();
				return metric_results;

			} catch (IOException e) {
				System.err.println("critical error");
				for (String s : metric_names) {
					metric_results.put(s, -1.0);
				}
				return metric_results;
			}
		} else {
			for (String s : metric_names) {
				metric_results.put(s, -1.0);
			}
		}
		return metric_results;
	}
}
