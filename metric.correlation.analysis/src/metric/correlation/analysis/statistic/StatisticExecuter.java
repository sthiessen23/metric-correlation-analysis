package metric.correlation.analysis.statistic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class StatisticExecuter {

	private static final String INPUT_SERIES = "Results-2018-11-21_13_54";
	private static final File DATA_FILE = new File(new File("results", INPUT_SERIES), "results.csv");
	
	private static final Logger LOGGER = Logger.getLogger(StatisticExecuter.class);

	private static final File RESULTS = new File("statistics");

	public static void main(String[] args) {
		StatisticExecuter executer = new StatisticExecuter();
		try {
			executer.calculateStatistics(DATA_FILE, new File(RESULTS, INPUT_SERIES));
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	/**
	 * Calculates correlations for the given metric values and saves them at the given location
	 * 
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public void calculateStatistics(File in, File out) throws IOException {
		if(!out.exists()) {
			out.mkdirs();
		}
		calculateStatistics(getMetricMap(in), out);
	}

	/**
	 * Calculates correlations for the given metric values and saves them at the given location
	 * 
	 * @param map The mapping from metric names to values
	 * @param out The output file
	 * @throws IOException
	 */
	public void calculateStatistics(final LinkedHashMap<String, List<Double>> map, File out) throws IOException {
		final ArrayList<String> metricNames = new ArrayList<>(map.keySet());
		
		RealMatrix matrix = createMatrix(map);
		
		RealMatrix pearsonMatrix = new PearsonsCorrelation().computeCorrelationMatrix(matrix);
		CorreltationMatrixPrinter.storeMatrix(pearsonMatrix, metricNames, new File(out, "PearsonCorrelationMatrix.csv"));

		RealMatrix spearmanMatrix = new SpearmansCorrelation().computeCorrelationMatrix(matrix);
		CorreltationMatrixPrinter.storeMatrix(spearmanMatrix, metricNames, new File(out, "SpearmanCorrelationMatrix.csv"));

		new NormalDistribution().testAndStoreNormalDistribution(map, new File(out, "shapiroWilkTestAll.csv"));
	}

	/**
	 * Creates a map from a stored metric csv file
	 * 
	 * @param dataFile The file
	 * @return The map
	 * @throws IOException If there is an exception reading the file
	 */
	private LinkedHashMap<String, List<Double>> getMetricMap(File dataFile) throws IOException {
		List<String> lines = Files.readAllLines(dataFile.toPath());
		String[] keys = lines.get(0).split(",");
		LinkedHashMap<String, List<Double>> metrics = new LinkedHashMap<>(keys.length - 1);
		int projectNameIndex = -1;
		for (int i = 0; i < keys.length; i++) {
			String value = keys[i];
			if ("Application-Name".equals(value)) {
				projectNameIndex = i;
			} else {
				metrics.put(value, new ArrayList<>(lines.size() -1));
			}
		}
		if (projectNameIndex == -1) {
			throw new IllegalStateException("Project name not found");
		}
		for (String line : lines.subList(1, lines.size())) {
			String[] values = line.split(",");
			
			boolean valid = true;
			for (int i = 0; i < values.length; i++) {
				if(i == projectNameIndex) {
					continue;
				}
				String s = values[i];
				if (s == null || "null".equals(s) || "NaN".equals(s)) {
					valid = false;
					break;
				}
			}
			
			if (valid) {
				for(int i = 0; i < values.length; i++) {
					if(i == projectNameIndex) {
						continue;
					}
					metrics.get(keys[i]).add(Double.parseDouble(values[i]));
				}
			}
		}
		return metrics;
	}

	public RealMatrix createMatrix(Map<String, List<Double>> metricValues) {
		double[][] results = new double[metricValues.size()][];
		int col = 0;
		for (List<Double> s : metricValues.values()) {
			double[] d = new double[s.size()];
			for (int i = 0; i < s.size(); i++) {
				d[i] = s.get(i);
			}
			results[col++] = d;
		}
		return new Array2DRowRealMatrix(results).transpose();
	}
}
