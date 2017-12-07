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
	protected static String result_dir ="/home/bpede/Dokumente/ProgramData";
	private List<String> apks_not_built = new ArrayList<String>();
	private final String env_variable_name_mongod = "MONGOD";
	private Storage storage;
	private SourceMeter srcmeter;
	private GradleBuild build;
	private Androlyze androlyze;
	private Download downloader;
	private Hulk hulk;

	@Test
	public void execute() {
		storage = new Storage();
		srcmeter = new SourceMeter("SOURCE_METER_JAVA");
		build = new GradleBuild();
		androlyze = new Androlyze("ANDROLYZE");
		downloader = new Download();
		hulk = new Hulk();

//		File src_location = new File(result_dir + File.separator + "Sourcecode");
		File result_file = new File(result_dir + File.separator + "results" + File.separator + "Results-"
				+ new SimpleDateFormat("YYYY-MM-dd_HH_mm").format(new Date()) + ".csv");
//		mainProcess(src_location, result_file);
		String gitUrl = "https://github.com/velazcod/Tinfoil-Facebook.git";

		mainProcess(gitUrl, result_file);

	}

	protected void mainProcess(String gitUrl, File result_file) {

		startDatabase();
		result_dir = result_dir + File.separator + "VersionTemp";
		File version_ids = new File(result_dir + File.separator + "VersionIds.csv");

		downloader.gitClone(gitUrl);
		File temp1[] = new File(result_dir + File.separator + "Sourcecode").listFiles();
		File src_location = temp1[0];

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
				String name = "Tinfoil-Facebook";
				calculateMetrics(result_file, name, src_location);

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void mainProcess(File src_location, File result_file) {

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
					calculateMetrics(result_file, s, project_src);

				}
			} else
				System.err.println("Sourcecode-Directory is empty!");
		}
		if (apks_not_built.isEmpty())
			System.out.println("No Problems occured!");
		else
			System.out.println("APKs not built: " + apks_not_built);
	}

	private void calculateMetrics(File result_file, String s, File project_src) {
		if (!hulk.calculateMetric(project_src)) {
			System.err.println("Calculation Hulk Metric failed");
		}
		LinkedHashMap<String, Double> hulk_results = hulk.getResults(project_src);

		File srcmeter_out = new File(result_dir + File.separator + "SourceMeter" + File.separator + s);
		if (!srcmeter_out.exists()) {
			srcmeter.calculateMetric(hulk.getLocation());
		}
		File src_meter_folder = new File(srcmeter_out, "SrcMeter");
		System.out.println(src_meter_folder);
		LinkedHashMap<String, Double> oo_metrics = srcmeter.getResults(src_meter_folder);
		System.out.println("After SourceMeter: " + oo_metrics);
		if (oo_metrics == null) {
			for (int i = 0; i < 7; i++)
				metric_results.putAll(oo_metrics);
		}
		metric_results.putAll(oo_metrics);
		metric_results.putAll(hulk_results);

		File andro_results = new File(result_dir + File.separator + "androlyze" + File.separator + s);

		if (!andro_results.exists() && build.buildApk(project_src)) {
			build.getApk(project_src);
			System.out.println(GradleBuild.compiled_apk);
			if (GradleBuild.compiled_apk != null) {
				androlyze.calculateMetric(andro_results);

				System.out.println(GradleBuild.compiled_apk);
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

		if (!result_file.exists())
			storage.initCSV(result_file);
		storage.writeCSV(result_file, s);
		metric_results.clear();
	}

	private void startDatabase() {
		
		String mongod = System.getenv(env_variable_name_mongod);
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
				return;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
