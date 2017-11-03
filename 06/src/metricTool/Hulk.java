package metricTool;


import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.importer.GradleImport;
import org.gravity.hulk.HulkAPI;
import org.gravity.hulk.HulkAPI.AntiPatternNames;
import org.gravity.hulk.antipatterngraph.HAntiPattern;

public class Hulk implements MetricCalculator{
	
	public List<HAntiPattern> blob_result = null;
	public List<HAntiPattern> igam_result = null;
	public List<HAntiPattern> igat_result = null;
	public LinkedHashMap<String, Double> metric_results = new LinkedHashMap<String, Double>();
	
	public boolean calculateMetric(File in){
		
	String gradle = "C:\\Users\\Biggi\\.gradle";
	String android = "\"C:\\Program Files\\sdk-tools-windows-3859397\"";
	GradleImport gradleImport = new GradleImport(gradle, android);
	try {
		IJavaProject project = gradleImport.importGradleProject(in, in.getName(), new NullProgressMonitor());
		
		blob_result = HulkAPI.detect(project, new NullProgressMonitor(), AntiPatternNames.Blob);
		blob_result = HulkAPI.detect(project, new NullProgressMonitor(), AntiPatternNames.IGAM);
		blob_result = HulkAPI.detect(project, new NullProgressMonitor(), AntiPatternNames.IGAT);
		
		System.out.println("Antipatterns: " + blob_result.size());		
		System.out.println("IGAM: " + igam_result.size());		
		System.out.println("IGAT: " + igat_result.size());
		
	} catch (Exception e) {
		e.printStackTrace();
		return false;
	}		
		return false;
	}

	@Override
	public HashMap<String, Double> getResults(File in) {
		
		metric_results.put("ANTIPATTERNS", (double)(blob_result.size()));
		metric_results.put("IGAM", (double)(igam_result.size()));
		metric_results.put("IGAT", (double)(igat_result.size()));
		
		return metric_results;
	}

}
