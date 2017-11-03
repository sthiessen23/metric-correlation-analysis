package statistic;

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

import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math.linear.Array2DRowRealMatrix;

import flanagan.analysis.Normality;
import flanagan.analysis.ProbabilityPlot;

public class NormalDistribution {

	public static String[] metric_names = { "LOCpC,", "WMC,", "CBO,", "LCOM,", "DIT,", "LDC,", "Permissions," };
	public static double significance = 0.05;

	public static void main(String[] args) {

		NormalDistribution s = new NormalDistribution();
		
		File dataFile = new File("C:\\Users\\Biggi\\Documents\\strategie2\\results\\NewMetricResults.csv");
		
		// metricValues = [AnzahlMetriken][Anzahl Apps]
		double[][] metricValues = s.getValues(dataFile);
		
//		s.testNormalDistribution(metricValues);
		double[] LOCpC = metricValues[0];
		ProbabilityPlot p = new ProbabilityPlot(LOCpC);
		p.frechetProbabilityPlot();
	

//		Normality norm = new Normality(LOCpC);
//		norm.fullAnalysis();
//
//		RealMatrix matrix = new PearsonsCorrelation().computeCorrelationMatrix(metricValues);
//		s.storeMatrix(matrix);

	}

	public void testNormalDistribution(double[][] d) {
		File normality_test = new File("C:\\Users\\Biggi\\Documents\\strategie2\\results\\shapiroWilkTest1.csv");
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(normality_test));
			writer.newLine();
			writer.write(
					"Metric name, W-Value, W-crit, Normal Distribution, P-Value, Significance, Normal Distribution");
			for (int i = 0; i < 7; i++) {
				Normality norm = new Normality(d[i]);
				writer.newLine();
				writer.write(metric_names[i] + dFormat.format(norm.shapiroWilkWvalue()) + ","
						+ dFormat.format(norm.shapiroWilkCriticalW()) + ",");
				if (norm.shapiroWilkWvalue() <= norm.shapiroWilkCriticalW())
					writer.write("Yes,");
				else
					writer.write("No,");
				writer.write(dFormat.format(norm.shapiroWilkPvalue()) + "," + significance + ",");
				if (norm.shapiroWilkPvalue() >= significance)
					writer.write("Yes");
				else
					writer.write("No");
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void printMatrix(RealMatrix matrix) {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
		for (int i = 0; i < 7; i++) {
			double[] row = matrix.getRow(i);
			System.out.println();
			for (double r : row) {
				System.out.print(dFormat.format(r));
				System.out.print("\t");
			}
		}
	}

	public void storeMatrix(RealMatrix matrix) {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
		File matrix_file = new File("C:\\Users\\Biggi\\Documents\\strategie2\\results\\CorrelationMatrix.csv");

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(matrix_file));
			writer.newLine();
			writer.write("," + "LOCpC," + "WMC," + "CBO," + "LCOM," + "DIT," + "LDC," + "Permissions");
			for (int i = 0; i < 7; i++) {
				double[] row = matrix.getRow(i);
				writer.newLine();
				writer.write(metric_names[i]);
				for (double r : row) {
					writer.write(dFormat.format(r));
					writer.write(", ");
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public double[][] createMatrix(File dataFile) {
		double[][] values = new double[30][7];

		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			String line = reader.readLine();
			int row = 0;
			while ((line = reader.readLine()) != null) {
				String[] s = line.substring(0, line.length()).split(","); //$NON-NLS-1$
				for (int col = 0; col < 7; col++) {
					values[row][col] = Double.parseDouble(s[col + 2]);
				}
				row++;
			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return values;
	}

	public double[][] getValues(File dataFile) {
		double[][] d = new double[7][26];
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			String line = reader.readLine();
			reader.close();

			String[] names = line.substring(0, line.length()).split(","); //$NON-NLS-1$
			List<Double> metric_values = new ArrayList<Double>();
			String[] s = {"LOCpC", "WMC", "CBO", "LCOM5", "DIT", "LDC", "Permissions"};
			int j = 0;
			for(String nam : s){
				int metric_index = Arrays.asList(names).indexOf(nam);

				try {
					BufferedReader metric_reader = new BufferedReader(new FileReader(dataFile));
					String m_line = metric_reader.readLine();
					while ((m_line = metric_reader.readLine()) != null) {
						String[] values = m_line.substring(0, m_line.length()).split(","); //$NON-NLS-1$
						// Arrays.stream(values).forEach(System.out::println);
						metric_values.add(Double.parseDouble(values[metric_index]));

					}
					metric_reader.close();

					for (int i = 0; i < metric_values.size(); i++) {
						d[j][i] = metric_values.get(i);
					}
					
					// class_values = normalize(class_values);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				j++;
				metric_values.clear();
			}
			

			return d;
		} catch (IOException e) {

			e.printStackTrace();
		}
		return new double[0][0];
	}

	

}