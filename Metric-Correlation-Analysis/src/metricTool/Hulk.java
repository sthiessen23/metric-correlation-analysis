package metricTool;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
import org.gravity.typegraph.basic.TypeGraph;

public class Hulk implements MetricCalculator {

	private List<HAnnotation> hulk_results = null;
	private boolean hulk_ok = false;
	private File location = null;

	public boolean calculateMetric(File in) {

		String gradle = System.getenv("GRADLE_HOME");
		String android = System.getenv("ANDROID_HOME");
		GradleImport gradleImport = new GradleImport(gradle, android);
		try {
			IJavaProject project = gradleImport.importGradleProject(in, in.getName(), new NullProgressMonitor());
			if(project==null) {
				return false;
			}
			location  = project.getProject().getLocation().toFile();
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
		double igam = 0.0;
		double igat = 0.0;
		
		if(!hulk_ok){
			metric_results.put("BLOB-Antipattern", -1.0);
			metric_results.put("IGAM", -1.0);
			metric_results.put("IGAT", -1.0);
			return metric_results;
		}		
		double blob = 0.0;
		
		for(HAnnotation ha : hulk_results){

			if(ha instanceof HBlobAntiPattern)
				blob++;	
			
			if(ha instanceof HInappropriateGenerosityWithAccessibilityOfMethodMetric){
				if (ha.getTAnnotated() instanceof TypeGraph) {
					igam = ((HMetric) ha).getValue();
					System.out.println(igam);
				}
			}				
			if(ha instanceof HInappropriateGenerosityWithAccessibilityOfTypesMetric){
				if (ha.getTAnnotated() instanceof TypeGraph) {
					igat = ((HMetric) ha).getValue();
					System.out.println(igat);
				}
			}
			
		}	
		metric_results.put("BLOB-Antipattern", blob);
		metric_results.put("IGAM", roundDouble(igam));
		metric_results.put("IGAT", roundDouble(igat));

		return metric_results;
	}
	
	private double roundDouble(double d) {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
		
		return Double.parseDouble(dFormat.format(d));
	}

	public File getLocation() {
		return location;
	}
}
