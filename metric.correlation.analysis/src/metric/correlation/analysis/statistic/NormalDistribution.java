package metric.correlation.analysis.statistic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import flanagan.analysis.Normality;

public class NormalDistribution {

	private static final Logger LOGGER = Logger.getLogger(NormalDistribution.class);

	public static double significance = 0.05;

	public void testNormalDistribution(double[][] d, String[] metricNames, File resultFile) {

		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile));
			writer.newLine();
			writer.write(
					"Metric name, W-Value, W-crit, Normal Distribution, P-Value, Significance, Normal Distribution");
			for (int i = 0; i < metricNames.length; i++) {
				Normality norm = new Normality(d[i]);
				writer.newLine();
				writer.write(metricNames[i] + "," + dFormat.format(norm.shapiroWilkWvalue()) + ","
						+ dFormat.format(norm.shapiroWilkCriticalW()) + ",");
				if (norm.shapiroWilkWvalue() <= norm.shapiroWilkCriticalW())
					writer.write("Yes,");
				else
					writer.write("No,");
				writer.write(dFormat.format(norm.shapiroWilkPvalue()) + "," + significance + ",");
				if (norm.shapiroWilkPvalue() <= significance)
					writer.write("Yes");
				else
					writer.write("No");
			}

			writer.close();
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	// double[][] = [AnzahlMetriken][Anzahl Apps]
	public double[][] getValues(File dataFile, String[] metricNames) throws IOException {
		double[][] d = new double[metricNames.length][50];
		String firstLine;
		try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
			firstLine = reader.readLine();
		} 

		String[] names = firstLine.substring(0, firstLine.length()).split(",");
		List<Double> metricValues = new ArrayList<Double>();
		int j = 0;
		for (String nam : metricNames) {
			int metricIndex = Arrays.asList(names).indexOf(nam);

			try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))){
				String line = reader.readLine();
				while ((line = reader.readLine()) != null) {
					String[] values = line.substring(0, line.length()).split(",");
					metricValues.add(Double.parseDouble(values[metricIndex]));

				}
				reader.close();

				for (int i = 0; i < metricValues.size(); i++) {
					d[j][i] = metricValues.get(i);
				}
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, e.getMessage(), e);
			}
			j++;
			metricValues.clear();
		}

		return d;
	}

}