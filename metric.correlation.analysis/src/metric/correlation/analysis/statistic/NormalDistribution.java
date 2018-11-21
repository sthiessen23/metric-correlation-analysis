package metric.correlation.analysis.statistic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import flanagan.analysis.Normality;

public class NormalDistribution {

	private static final Logger LOGGER = Logger.getLogger(NormalDistribution.class);

	public static double significance = 0.05;

	private DecimalFormat dFormat;

	public NormalDistribution() {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		dFormat = new DecimalFormat("0.00", dfs);
	}
	
	public void testAndStoreNormalDistribution(Map<String, List<Double>> metricValues, File resultFile) {

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile))){
			writer.write(
					"Metric name, W-Value, W-crit, Normal Distribution, P-Value, Significance, Normal Distribution");
			for (String metricName : metricValues.keySet()) {
				List<Double> list = metricValues.get(metricName);
				double[] doubleArray = new double[list.size()];
				for(int i = 0; i < list.size(); i++) {
					doubleArray[i] = list.get(i);
				}
				Normality norm = new Normality(doubleArray);
				final double wValue = norm.shapiroWilkWvalue();
				final double wCritical = norm.shapiroWilkCriticalW();
				final boolean normalDistribution = wValue <= wCritical;
				final double pValue = norm.shapiroWilkPvalue();
				final boolean normalDistribution2 = pValue <= significance;
				
				printNextLine(writer, metricName, wValue, wCritical, normalDistribution, pValue, normalDistribution2);
			}
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	/**
	 * Prints the next line
	 * 
	 * @param writer The writer
	 * @param metricName
	 * @param wValue
	 * @param wCritical
	 * @param normalDistribution
	 * @param pValue
	 * @param normalDistribution2
	 * @throws IOException
	 */
	public void printNextLine(BufferedWriter writer, String metricName, final double wValue, final double wCritical,
			final boolean normalDistribution, final double pValue, final boolean normalDistribution2)
			throws IOException {
		writer.newLine();
		writer.write(metricName);
		writer.write(",");
		writer.write(dFormat.format(wValue));
		writer.write(",");
		writer.write(dFormat.format(wCritical));
		writer.write(",");
		writer.write(Boolean.toString(normalDistribution));
		writer.write(",");
		writer.write(dFormat.format(pValue));
		writer.write(",");
		writer.write(dFormat.format(significance));
		writer.write(",");
		writer.write(Boolean.toString(normalDistribution2));
	}

	/**
	 *  size of the returned matrix: double[][] = [AnzahlMetriken][Anzahl Apps]
	 * 
	 * @param metrics A linked hashmap of metric keys and values
	 * @return a double matrix
	 */
	public double[][] getValues(LinkedHashMap<String, List<Double>> metrics) {
		double[][] results = new double[metrics.size()][];
		int metricIndex = 0;
		for(List<Double> values : metrics.values()) {
			double[] doubleArray = new double[values.size()];
			for(int doubleArrayIndex = 0; doubleArrayIndex < values.size(); doubleArrayIndex++) {
				doubleArray[metricIndex] = values.get(doubleArrayIndex);
			}
			results[metricIndex++] = doubleArray;
		}
		
		return results;
	}

}