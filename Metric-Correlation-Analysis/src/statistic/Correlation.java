package statistic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.apache.commons.math.linear.RealMatrix;

public class Correlation {

	public void printMatrix(RealMatrix matrix, String[] metricNames) {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
		for (int i = 0; i < metricNames.length; i++) {
			double[] row = matrix.getRow(i);
			System.out.println();
			for (double r : row) {
				System.out.print(dFormat.format(r));
				System.out.print("\t");
			}
		}
	}

	public void storeMatrix(RealMatrix matrix, String[] metricNames, File resultFile) {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile));
			writer.newLine();
			writer.write(",");
			for (int j = 0; j < metricNames.length; j++) {
				writer.write(metricNames[j] + ",");
			}
			for (int i = 0; i < metricNames.length; i++) {
				double[] row = matrix.getRow(i);
				writer.newLine();
				writer.write(metricNames[i]);
				for (double r : row) {
					writer.write(", ");
					writer.write(dFormat.format(r));				
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public double[][] createMatrix(File dataFile, String[] metricNames) {
		double[][] values = new double[50][metricNames.length];

		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			String line = reader.readLine();
			int row = 0;
			while ((line = reader.readLine()) != null) {
				String[] s = line.substring(0, line.length()).split(","); //$NON-NLS-1$
				for (int col = 0; col < metricNames.length; col++) {
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
}
