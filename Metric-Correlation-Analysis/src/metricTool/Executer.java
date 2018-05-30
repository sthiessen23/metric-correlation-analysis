package metricTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

public class Executer {

	public static boolean windows = System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0;
	public static boolean linux = System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0;
	protected static LinkedHashMap<String, Double> metric_results = new LinkedHashMap<String, Double>();
	protected static String result_dir;
	private List<String> apks_not_built = new ArrayList<String>();
	private final String env_variable_name_mongod = "MONGOD";
	private Storage storage;
	private SourceMeter srcmeter;
	private GradleBuild build;
	private Androlyze androlyze;
	private Download downloader;
	private Hulk hulk;
	private boolean dbRunning;

	@Test
	public void execute() {
		storage = new Storage();
		srcmeter = new SourceMeter("SOURCE_METER_JAVA");
		build = new GradleBuild();
		androlyze = new Androlyze("ANDROLYZE");
		downloader = new Download();
		hulk = new Hulk();

//		mainProcess("https://github.com/mozilla/rhino.git", new File("commitIds.txt"), new File("results"));
		mainProcess(new File("repositories"), new File("results"));
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
	protected void mainProcess(String gitUrl, File version_ids, File result_dir) {
		Executer.result_dir = result_dir.getAbsolutePath();
		
		dbRunning = startDatabase();

		File result_file = new File(result_dir, "results" + File.separator + "Results-"
				+ new SimpleDateFormat("YYYY-MM-dd_HH_mm").format(new Date()) + ".csv");

		if (!downloader.gitClone(gitUrl)) {
			return;
		}

		String name = gitUrl.substring(gitUrl.lastIndexOf('/')+1, gitUrl.length() - 4);
		File src_location = new File(new File(result_dir + File.separator + "Sourcecode"), name);

		calculateMetrics(result_file, name, src_location);
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(version_ids));
			String line = reader.readLine();
			String[] ids = line.substring(0, line.length()).split(",");
			reader.close();
			for (String id : ids) {
				System.out.println("\n\n\n#############################");
				System.out.println("### " + new SimpleDateFormat().format(new Date()) + " ###");
				System.out.println("#############################");
				System.out.println(id);
				System.out.println("#############################\n");

				downloader.changeVersion(src_location, id);

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
		Executer.result_dir = result_dir.getAbsolutePath();

		startDatabase();

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
		if (apks_not_built.isEmpty())
			System.out.println("No Problems occured!");
		else
			System.err.println("APKs not built: " + apks_not_built);
	}

	private void calculateMetrics(File results_dir, String s, File project_src) {
		if (!hulk.calculateMetric(project_src)) {
			System.err.println("Calculation Hulk Metric failed");
			return;
		}
		LinkedHashMap<String, Double> hulk_results = hulk.getResults(project_src);

		File srcmeter_out = new File(results_dir, "SourceMeter" + File.separator + s);
		if (!srcmeter_out.exists()) {
			srcmeter.calculateMetric(hulk.getLocation());
		}
		File src_meter_folder = new File(srcmeter_out, "SrcMeter");
		System.out.println(src_meter_folder);
		LinkedHashMap<String, Double> oo_metrics = srcmeter.getResults(src_meter_folder);

		if (oo_metrics == null) {
			for (int i = 0; i < 7; i++)
				metric_results.putAll(oo_metrics);
		}
		metric_results.putAll(oo_metrics);
		metric_results.putAll(hulk_results);

		File andro_results = new File(results_dir, "androlyze" + File.separator + s);

		if (dbRunning) {
			if (!andro_results.exists() && build.buildApk(project_src)) {
				build.getApk(project_src);

				if (GradleBuild.compiled_apk != null) {
					androlyze.calculateMetric(andro_results);
					LinkedHashMap<String, Double> permission = androlyze.getResults(andro_results);
					metric_results.putAll(permission);
				} else {
					System.err.println(s + ": Build failed!");
					apks_not_built.add(s);
					metric_results.put("Permissions", -1.0);
				}

			} else {
				LinkedHashMap<String, Double> permission = androlyze.getResults(andro_results);
				metric_results.putAll(permission);
			}
		} else {
			System.err.println("Skipped Androlize as MongoDB is not running");
		}
		/*
		 * Here the tool can be extended by further metrics or metric-tools.
		 * 
		 * For this purpose you have to call: newClass.calculateMetric();
		 * 
		 * and: newClass.getResults();
		 * 
		 * The new Metric results must be added to class variable LinkedHashMap<String,
		 * Double> metric_results
		 * 
		 */

		File csv = new File(results_dir, "results.csv");
		if (!csv.exists())
			storage.initCSV(csv);
		storage.writeCSV(csv, s);
		metric_results.clear();
	}

	private boolean startDatabase() {

		String mongod = System.getenv(env_variable_name_mongod);
		if (mongod == null) {
			System.err.println("Environment variable \"" + env_variable_name_mongod + "\" not set.");
			return false;
		}
		String cmd = mongod + (mongod.endsWith(File.separator) ? "" : File.separator) + "mongod -f " + mongod
				+ (mongod.endsWith(File.separator) ? "" : File.separator) + "../mongod.cfg";
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (windows) {
				process = run.exec("cmd /c \"" + cmd);
			} else if (linux) {
				process = run.exec(cmd);
			} else {
				System.err.println("Program is not compatibel with the Operating System");
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
