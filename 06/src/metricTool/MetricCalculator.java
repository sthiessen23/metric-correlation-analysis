package metricTool;

import java.io.File;
import java.util.HashMap;

public interface MetricCalculator {

	public boolean windows = System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0;
	public boolean linux = System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0;
	
	public boolean calculateMetric(File in);
	
	public HashMap<String, Double> getResults(File in);
	
}
