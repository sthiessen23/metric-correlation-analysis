package metricTool;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.importer.GradleImport;
import org.gravity.hulk.HulkAPI;
import org.gravity.hulk.HulkAPI.AntiPatternNames;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HMetric;
import org.gravity.hulk.antipatterngraph.antipattern.HBlobAntiPattern;
import org.gravity.hulk.antipatterngraph.metrics.HInappropriateGenerosityWithAccessibilityOfMethodMetric;
import org.gravity.hulk.antipatterngraph.metrics.HInappropriateGenerosityWithAccessibilityOfTypesMetric;

public class Hulk implements MetricCalculator {

	private List<HAnnotation> hulk_results = null;
	private List<Double> igam_values = new ArrayList<Double>();
	private List<Double> igat_values = new ArrayList<Double>();
	private boolean hulk_ok = false;

	public boolean calculateMetric(File in) {

		String gradle = "C:\\Users\\Biggi\\.gradle";
		String android = "C:\\Programme\\sdk-tools-windows-3859397";
		GradleImport gradleImport = new GradleImport(gradle, android);
		try {
			IJavaProject project = gradleImport.importGradleProject(in, in.getName(), new NullProgressMonitor());
			hulk_results = HulkAPI.detect(project, new NullProgressMonitor(), AntiPatternNames.Blob, AntiPatternNames.IGAM, AntiPatternNames.IGAT);
			hulk_ok = true;
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public LinkedHashMap<String, Double> getResults(File in) {
		
		LinkedHashMap<String, Double> metric_results = new LinkedHashMap<String, Double>();
		
		if(!hulk_ok){
			metric_results.put("BLOB", -1.0);
			metric_results.put("IGAM", -1.0);
			metric_results.put("IGAT", -1.0);
			return metric_results;
		}		
		double blob = 0.0;
		
		for(HAnnotation ha : hulk_results){
			
			if(ha instanceof HBlobAntiPattern)
				blob++;	
			
			if(ha instanceof HInappropriateGenerosityWithAccessibilityOfMethodMetric){
				double temp = ((HMetric) ha).getValue();
				if(!Double.isNaN(temp))
					igam_values.add(temp);
			}				
			if(ha instanceof HInappropriateGenerosityWithAccessibilityOfTypesMetric){
				double temp = ((HMetric) ha).getValue();
				if(!Double.isNaN(temp))
					igat_values.add(temp);
			}
		}	
		metric_results.put("BLOB", blob);
		metric_results.put("IGAM", calculateAverage(igam_values));
		metric_results.put("IGAT", calculateAverage(igat_values));

		return metric_results;
	}
	
	private double calculateAverage(List<Double> list){
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
		double sum = 0.0;
		for(double d : list)
			sum += d;
		sum = sum / list.size();
		return Double.parseDouble(dFormat.format(sum));
	}

}
