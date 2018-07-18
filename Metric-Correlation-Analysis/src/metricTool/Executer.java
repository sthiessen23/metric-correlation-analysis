package metricTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.importer.GradleImport;
import org.gravity.eclipse.importer.NoGradleRootFolderException;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;
import org.junit.Test;

public class Executer {

	private static final Collection<IMetricCalculator> calculators = new ArrayList<>(3);

	public Executer() {
		calculators.add(new Hulk());
		try {
			calculators.add(new SourceMeter());
		} catch (MetricCalculatorInitializationException e) {
			e.printStackTrace();
		}
		try {
			calculators.add(new Androlyze());
		} catch (MetricCalculatorInitializationException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void execute() throws MetricCalculatorInitializationException, UnsupportedOperationSystemException {

		 mainProcess("https://github.com/prey/prey-android-client.git", new
		 File("commitIds.txt"), new File("repositories"), new File("results"));
//		mainProcess("https://github.com/apache/groovy.git", new File("commitIds.txt"), new File("repositories"),
//				new File("results"));
		// mainProcess(new File("repositories"), new File("results"));
		/*
		 * for calculating one version of many projects you have to
		 * 
		 * define the class variable String result_dir,
		 * 
		 * define the file with the git-urls of all projects you want to analyze: File
		 * downloadURLs = new File("...");
		 * 
		 * download all projects you want to analyze: organizeDownloads(downloadURLs)
		 * 
		 * define the location where all projects are downloaded: File src_location =
		 * new File(result_dir + File.separator + "Sourcecode");
		 * 
		 * define the location where the results should be stored: File result_file =
		 * new File(result_dir + File.separator + "results");
		 * 
		 * and call: mainProcess(src_location, result_file);
		 */

		/*
		 * for calculating multiple version you have to
		 * 
		 * change result_dir in: result_dir = result_dir + File.separator +
		 * "VersionTemp";
		 * 
		 * define the git-url of that project you want to analyze: String gitUrl =
		 * "...";
		 * 
		 * define the file with the commit-ids of that project you want to check: File
		 * version_ids = new File(result_dir + File.separator + "VersionIds.csv");
		 * 
		 * and call: mainProcess(gitUrl, version_ids);
		 * 
		 */

	}

	// main process for calculating metrics for multiple versions of one project
	protected void mainProcess(String gitUrl, File version_ids, File sourcecode_dir, File result_dir)
			throws UnsupportedOperationSystemException {
		File result_file = new File(result_dir, "results" + File.separator + "Results-"
				+ new SimpleDateFormat("YYYY-MM-dd_HH_mm").format(new Date()) + ".csv");

		try {
			if (!Download.gitClone(gitUrl, sourcecode_dir, true)) {
				return;
			}
		} catch (GitCloneException e1) {
			e1.printStackTrace();
			return;
		}

		String name = gitUrl.substring(gitUrl.lastIndexOf('/') + 1, gitUrl.length() - 4);

		File src_location = new File(sourcecode_dir, name);
		calculateMetrics(result_file, name, src_location);

		if (version_ids != null && version_ids.exists())
			try (BufferedReader reader = new BufferedReader(new FileReader(version_ids))) {
				String line = reader.readLine();
				String[] ids = line.substring(0, line.length()).split(",");
				reader.close();
				for (String id : ids) {
					System.out.println("\n\n\n#############################");
					System.out.println("### " + new SimpleDateFormat().format(new Date()) + " ###");
					System.out.println("#############################");
					System.out.println(id);
					System.out.println("#############################\n");

					Download.changeVersion(src_location, id);

					File srcmeter_out = new File(result_dir + File.separator + "SourceMeter");
					clear(srcmeter_out);
					calculateMetrics(result_file, name, src_location);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

	}

	// main process for calculating metrics for one version of many projects
	private void mainProcess(File src_location, File result_dir) {
		if (src_location.isDirectory()) {
			String[] app_string = src_location.list();
			if (app_string[0] != null) {
				Arrays.stream(app_string).forEach(System.out::println);
				for (String s : app_string) {
					System.out.println("\n\n\n#############################");
					System.out.println("### " + new SimpleDateFormat().format(new Date()) + " ###");
					System.out.println("#############################");
					System.out.println(s);
					System.out.println("#############################\n");

					File project_src = new File(src_location, s);
					calculateMetrics(result_dir, s, project_src);
				}
			} else
				System.err.println("Sourcecode-Directory is empty!");
		}
	}

	private boolean calculateMetrics(File results_dir, String name, File src) {
		GradleImport gradleImport;
		try {
			gradleImport = new GradleImport(src);
		} catch (NoGradleRootFolderException e1) {
			return false;
		}
		IJavaProject project;
		try {
			project = gradleImport.importGradleProject(new NullProgressMonitor());
		} catch (IOException | CoreException | InterruptedException | NoGradleRootFolderException e1) {
			e1.printStackTrace();
			return false;
		}
		if (project == null) {
			return false;
		}

		Hashtable<String, Double> results = new Hashtable<>();
		for (IMetricCalculator calc : calculators) {
			if (calc.calculateMetric(project)) {
				results.putAll(calc.getResults());
			}
		}

		try {
			new Storage(new File(results_dir, "results.csv"), results.keySet()).writeCSV(name, results);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		results.clear();

		return true;

	}

	protected static void clear(File file) {
		if (file.exists()) {
			for (File f : file.listFiles()) {
				if (f.isDirectory()) {
					clear(f);
					f.delete();
				} else {
					f.delete();
				}
			}
		}
	}

}
