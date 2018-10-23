package metric.correlation.analysis.statistic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class StatisticExecuter {

	private static final Logger LOGGER = Logger.getLogger(StatisticExecuter.class);
	private static final String RESULTS = "C:\\Users\\Biggi\\Documents\\strategie2\\";

	private static StatisticExecuter executer;

	public static void main(String[] args) {
		File dataFile = new File(new File(new File(RESULTS), "results"), "ResultsOk.csv");
		executer = new StatisticExecuter();
		try {
			executer.calculateStatistics(dataFile);
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	private Correlation correlation;
	private NormalDistribution normalityTest;

	public void calculateStatistics(File dataFile) throws IOException {
		normalityTest = new NormalDistribution();
		executer = new StatisticExecuter();
		correlation = new Correlation();

		String[] metricNames = executer.getMetricNames(dataFile);

		File boxplotFoder = new File(RESULTS + "Boxplotauswahl");
		File statisticOutputFolder = new File(new File(RESULTS), "StatisticResults");
		
		new BoxAndWhiskerMetric("Box-and-Whisker Project's Metrics", boxplotFoder, new File(statisticOutputFolder, "Boxplot.jpeg"));

		double[][] d = correlation.createMatrix(dataFile, metricNames);
		RealMatrix pearsonMatrix = new PearsonsCorrelation().computeCorrelationMatrix(d);
		File pearsonMatrixFile = new File(statisticOutputFolder, "PearsonCorrelationMatrix.csv");
		correlation.storeMatrix(pearsonMatrix, metricNames, pearsonMatrixFile);

		RealMatrix spearmanMatrix = new SpearmansCorrelation().computeCorrelationMatrix(d);
		correlation.printMatrix(spearmanMatrix, metricNames);
		File spearmanMatrixFile = new File(statisticOutputFolder, "SpearmanCorrelationMatrix.csv");
		correlation.storeMatrix(spearmanMatrix, metricNames, spearmanMatrixFile);

		double[][] metricValues = normalityTest.getValues(dataFile, metricNames);
		File normalityTestResult = new File(statisticOutputFolder, "shapiroWilkTestAll.csv");
		normalityTest.testNormalDistribution(metricValues, metricNames, normalityTestResult);

		// Normality norm = new Normality(LOCpC);
		// norm.fullAnalysis();
	}

	public String[] getMetricNames(File dataFile) {
		String[] metricNames = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
			String line = reader.readLine();
			metricNames = line.substring(0, line.length()).split(",");
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
		String[] metricNames2 = Arrays.copyOfRange(metricNames, 2, metricNames.length);
		return metricNames2;
	}
}
