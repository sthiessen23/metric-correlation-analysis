package metric.correlation.analysis.statistic;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.chart.ChartUtils;

/**
 * 
 * This class can be used to visualize the distribution of different metrics per
 * class calculated by source meter
 *
 */
public class SourceMeterBoxAndWhiskerMetric extends ApplicationFrame {
	
	
	/**
	 * The location of the SourceMeter outputs which should be considered
	 */
	private static final File INPUT = new File("sourcemeter");
	
	/**
	 * The folder to which the results should be written
	 */
	private static final File OUTPUT = new File(new File("statisics"), "boxplots");

	
	/**
	 * The generated serial version ID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 *  Access to logging facilities. 
	 */
	private static final Logger LOGGER = Logger.getLogger(SourceMeterBoxAndWhiskerMetric.class);

	
	public static void main(String[] args) throws IOException {
		if(!INPUT.exists()) {
			final String message = "The input folder doesn't exist!";
			LOGGER.log(Level.ERROR, message);
			throw new IOException(message);
		}
		if(!OUTPUT.exists()) {
			OUTPUT.mkdirs();
		}
		new SourceMeterBoxAndWhiskerMetric("Box-and-Whisker Project's Metrics", INPUT, new File(OUTPUT, "Boxplot.jpeg"));
	}
	

	public SourceMeterBoxAndWhiskerMetric(final String title, File folder, File result) {

		super(title);

		final BoxAndWhiskerCategoryDataset dataset = createDatasetForSourceMeterOutput(folder);

		final CategoryAxis xAxis = new CategoryAxis("");
		final NumberAxis yAxis = new NumberAxis("Value");
		yAxis.setAutoRangeIncludesZero(false);
		final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		renderer.setFillBox(false);
		renderer.setDefaultToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
		final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);

		final JFreeChart chart = new JFreeChart("Class Metrics of each Project", new Font("SansSerif", Font.BOLD, 20),
				plot, true);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(900, 450));
		setContentPane(chartPanel);

		try {
			ChartUtils.saveChartAsJPEG(result, chart, 900, 450);
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

	}

	/**
	 * Creates a dataset.
	 * 
	 * @return a dataset.
	 */
	private BoxAndWhiskerCategoryDataset createDatasetForSourceMeterOutput(File sourceMeterOutputFolder) {

		final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		File[] resultList = sourceMeterOutputFolder.listFiles();

		for (File r : resultList) {
			String apkName = r.getName().replaceAll("SrcMeter", "");
			File[] javaFolder = new File(r, "SrcMeter" + File.separator + "java").listFiles();
			if (javaFolder.length > 0) {
				File metrics = new File(javaFolder[0], "SrcMeter-Class.csv"); // $NON-NLS-1$

				String firstLine;
				try (BufferedReader reader = new BufferedReader(new FileReader(metrics))) {
					firstLine = reader.readLine();
				} catch (IOException e) {
					LOGGER.log(Level.ERROR, e.getMessage(), e);
					return null;
				}

				String[] names = firstLine.substring(1, firstLine.length() - 1).split("\",\""); //$NON-NLS-1$
				String[] metricNames = { "WMC", "CBO", "LCOM5", "DIT", "LDC" };

				for (String s : metricNames) {
					List<Double> classValues = new ArrayList<Double>();
					int metric_index = Arrays.asList(names).indexOf(s);
					try {
						String[] files = { "SrcMeter-Class.csv", "SrcMeter-Enum.csv" };
						for (String f : files) {
							metrics = new File(javaFolder[0], f); // $NON-NLS-1$
							try (BufferedReader reader = new BufferedReader(new FileReader(metrics))) {
								String line = reader.readLine();
								while ((line = reader.readLine()) != null) {
									String[] values = line.substring(1, line.length() - 1).split("\",\""); //$NON-NLS-1$
									classValues.add(Double.parseDouble(values[metric_index]));
								}
							}
						}
					} catch (IOException e) {
						LOGGER.log(Level.ERROR, e.getMessage(), e);
					}

					LOGGER.debug("Adding series " + r);
					LOGGER.debug(classValues.toString());
					dataset.add(classValues, s, apkName);
				}
			} else {
				LOGGER.log(Level.ERROR, "SourceMeter Metric File is empty!");
			}
		}
		return dataset;

	}

	public List<Double> normalize(List<Double> list) {
		double maxValue = Collections.max(list);
		List<Double> normalizedList = new ArrayList<Double>();
		for (double d : list) {
			d = (d / maxValue);
			normalizedList.add(d);
		}
		return normalizedList;
	}

}