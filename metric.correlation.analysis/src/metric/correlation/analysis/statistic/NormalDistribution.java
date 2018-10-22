package metric.correlation.analysis.statistic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
	public double[][] getValues(File dataFile, String[] metricNames) {
		double[][] d = new double[metricNames.length][50];
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			String line = reader.readLine();
			reader.close();

			String[] names = line.substring(0, line.length()).split(",");
			List<Double> metric_values = new ArrayList<Double>();
			int j = 0;
			for (String nam : metricNames) {
				int metric_index = Arrays.asList(names).indexOf(nam);

				try {
					BufferedReader metric_reader = new BufferedReader(new FileReader(dataFile));
					String m_line = metric_reader.readLine();
					while ((m_line = metric_reader.readLine()) != null) {
						String[] values = m_line.substring(0, m_line.length()).split(",");
						metric_values.add(Double.parseDouble(values[metric_index]));

					}
					metric_reader.close();

					for (int i = 0; i < metric_values.size(); i++) {
						d[j][i] = metric_values.get(i);
					}

					// class_values = normalize(class_values);
				} catch (FileNotFoundException e) {
					LOGGER.log(Level.ERROR, e.getMessage(), e);
				} catch (IOException e) {
					LOGGER.log(Level.ERROR, e.getMessage(), e);
				}
				j++;
				metric_values.clear();
			}

			return d;
		} catch (IOException e) {

			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
		return new double[0][0];
	}

}