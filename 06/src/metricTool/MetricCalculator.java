package metricTool;

import java.io.File;
import java.util.HashMap;

public interface MetricCalculator {

	public boolean calculateMetric(File in);
	
	public HashMap<String, Double> getResults(File in);
	
}
