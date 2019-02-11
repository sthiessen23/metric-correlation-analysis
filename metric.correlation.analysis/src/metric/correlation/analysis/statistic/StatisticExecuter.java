package metric.correlation.analysis.statistic;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.Test;

import metric.correlation.analysis.calculation.impl.VersionMetrics;

public class StatisticExecuter {

	private static final String INPUT_SERIES = "Results-2019-01-19_17_59";
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
		final Set<String> keySet = map.keySet();
		final ArrayList<String> metricNames = new ArrayList<>(keySet);
		
		RealMatrix matrix = createMatrix(map);
		
//		RealMatrix pearsonMatrix = new PearsonsCorrelation().computeCorrelationMatrix(matrix);
//		CorreltationMatrixPrinter.storeMatrix(pearsonMatrix, metricNames, new File(out, "PearsonCorrelationMatrix.csv"));

		RealMatrix spearmanMatrix = new SpearmansCorrelation().computeCorrelationMatrix(matrix);
		CorreltationMatrixPrinter.storeMatrix(spearmanMatrix, metricNames, new File(out, "SpearmanCorrelationMatrix.csv"));
		
		
//	    XYSeries series = new XYSeries("Random");
//	    for (int i = 0; i <= 100; i++) {
//	        double x = r.nextDouble();
//	        double y = r.nextDouble();
//	        series.add(x, y);
//	    }
//	    result.addSeries(series);
//	    return result;
		
		for(int i = 0; i < keySet.size(); i++) {
			
			String xMetric = metricNames.get(i);
			List<Double> xValues = map.get(xMetric);
			
			for(int j = i + 1; j < keySet.size() ; j++) {		
				String yMetric = metricNames.get(j);
				List<Double> yValues = map.get(yMetric);
				
				XYSeriesCollection scatterPlotResult = new XYSeriesCollection();
				XYSeries ySeries = new XYSeries(yMetric);
				
				for (int counter = 0; counter < xValues.size(); counter++) {
					ySeries.add(xValues.get(counter), yValues.get(counter));
				}
			
				scatterPlotResult.addSeries(ySeries);
				JFreeChart chart = ChartFactory.createScatterPlot(
				       (xMetric + " vs " + yMetric), 
				       xMetric, yMetric, scatterPlotResult);
				BufferedImage chartImage = chart.createBufferedImage(600, 400);
				ImageIO.write(chartImage, "png", new FileOutputStream(new File(out, xMetric + "vs" + yMetric + ".png")));
			}
		}
		
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
		Set<Integer> skipIndex = new HashSet<>();
		for (int i = 0; i < keys.length; i++) {
			String value = keys[i];
			if (VersionMetrics.MetricKeysImpl.VENDOR.toString().equals(value) || VersionMetrics.MetricKeysImpl.PRODUCT.toString().equals(value) || VersionMetrics.MetricKeysImpl.VERSION.toString().equals(value)) {
				skipIndex.add(i);
			} else {
				metrics.put(value, new ArrayList<>(lines.size() -1));
			}
		}
		if (skipIndex.size() == 0) {
			throw new IllegalStateException("Project name not found");
		}
		for (String line : lines.subList(1, lines.size())) {
			String[] values = line.split(",");
			
			boolean valid = true;
			for (int i = 0; i < values.length; i++) {
				if(skipIndex.contains(i)) {
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
					if(skipIndex.contains(i)) {
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
	
	/*
	 * Version statistics
	 */
	@Test
	public void test() {
		try {
			for (ProductMetricData metric : getProductMetricData(new File("input/versions-results.csv")))
			generateCSVLinesFrom(metric);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private ArrayList<ProductMetricData> getProductMetricData(File dataFile) throws IOException{
		List<String> lines = Files.readAllLines(dataFile.toPath());
		lines.remove(0);
		
		ArrayList<String> productNames = new ArrayList<String>();
		ArrayList<String> vendors = new ArrayList<String>();

		ArrayList<Double> locpcs = new ArrayList<Double>();
		ArrayList<String> versions = new ArrayList<String>();
		ArrayList<Double> igams = new ArrayList<Double>();
		ArrayList<Double> ldcs = new ArrayList<Double>();
		ArrayList<Double> wmcs = new ArrayList<Double>();
		ArrayList<Double> dits = new ArrayList<Double>();
		ArrayList<Double> lcom5s = new ArrayList<Double>();
		ArrayList<Double> cbos = new ArrayList<Double>();
		ArrayList<Double> igats = new ArrayList<Double>();
		ArrayList<Double> blobAntiPatterns = new ArrayList<Double>();
		ArrayList<Double> llocs = new ArrayList<Double>();
		
		for (String line : lines) {
			String[] split = line.split(",");
			locpcs.add(Double.valueOf(split[0]));
			productNames.add(split[1]);
			vendors.add(split[10]);
			versions.add(split[5]);
			igams.add(Double.valueOf(split[2]));
			ldcs.add(Double.valueOf(split[3]));
			wmcs.add(Double.valueOf(split[4]));
			dits.add(Double.valueOf(split[6]));
			lcom5s.add(Double.valueOf(split[7]));
			cbos.add(Double.valueOf(split[8]));
			igats.add(Double.valueOf(split[9]));
			blobAntiPatterns.add(Double.valueOf(split[11]));
			llocs.add(Double.valueOf(split[12]));
		}
		
		ArrayList<ProductMetricData> metrics = new ArrayList<ProductMetricData>();
		ProductMetricData metric = new ProductMetricData();
		metric.productName = productNames.get(0);
		metric.vendor = vendors.get(0);
		metrics.add(metric);
		for (int i = 0; i < lines.size(); i++) {
			String product = productNames.get(i);
			if (!product.equals(metric.productName)) {
				metric = new ProductMetricData();
				metrics.add(metric);
				metric.productName = product;
				metric.vendor = vendors.get(i);
			}
			metric.locpcs.add(locpcs.get(i));
			metric.versions.add(versions.get(i));
			metric.igams.add(igams.get(i));
			metric.ldcs.add(ldcs.get(i));
			metric.wmcs.add(wmcs.get(i));
			metric.dits.add(dits.get(i));
			metric.lcom5s.add(lcom5s.get(i));
			metric.cbos.add(cbos.get(i));
			metric.igats.add(igats.get(i));
			metric.blobAntiPatterns.add(blobAntiPatterns.get(i));
			metric.llocs.add(llocs.get(i));
		}
		return metrics;	
	}
	
	private List<String> generateCSVLinesFrom(ProductMetricData metric) {
		ArrayList<String> lines = new ArrayList<String>();
		ArrayList<ArrayList<Double>> columns = new ArrayList<ArrayList<Double>>();
		columns.add(metric.locpcs);
		columns.add(metric.igams);
		columns.add(metric.ldcs);
		columns.add(metric.wmcs);
		columns.add(metric.dits);
		columns.add(metric.lcom5s);
		columns.add(metric.cbos);
		columns.add(metric.igats);
		columns.add(metric.blobAntiPatterns);
		columns.add(metric.llocs);
		
		//lines.add(metric.productName);
		System.out.println(metric.productName);

		for (int i = 1; i < metric.versions.size(); i++) {
			String line = metric.versions.get(i-1) + "->" + metric.versions.get(i);
			
			for (ArrayList<Double> column : columns ) {
				line = line + "," + ProductMetricData.diff(column.get(i-1), column.get(i));
			}
			
			lines.add(line);
			System.out.println(line);

		}
		return lines;
	}
}
