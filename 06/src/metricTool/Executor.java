package metricTool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

public class Executor {

	protected static boolean windows = System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0;
	protected static boolean linux = System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0;
	protected static List<String> timelog = new ArrayList<String>();
	protected static LinkedHashMap<String, Double> metric_results = new LinkedHashMap<String, Double>();
	protected static String result_dir = "C:\\Users\\Biggi\\Documents\\Strategie3\\";
	private List<String> apks_not_built = new ArrayList<String>();
	private final String env_variable_name_mongod = "MONGOD";
//	private final String workspace = "C:\\Users\\Biggi\\junit-workspace\\";

	@Test
	public void execute() {
		File src_location = new File(result_dir + "Sourcecode");
		File result_file = new File(result_dir + "results\\MetricResults.csv");
		mainProcess(src_location, result_file);

	}

	protected void mainProcess(File src_location, int id, File result_file) {
		System.out.println("Started");
		Storage store = new Storage();
		SourceMeter srcmeter = new SourceMeter("SOURCE_METER_JAVA");
		GradleBuild build = new GradleBuild();
		Androlyze andro = new Androlyze("ANDROLYZE");
		startDatabase();
		System.out.println(src_location.toPath());
		if (src_location.exists()) {
			File src_meter_out = new File(result_dir + "SourceMeter\\" + src_location.getName());
			File src_meter_folder = new File(src_meter_out, "SrcMeter");
			System.out.println(src_meter_folder);

			if (src_meter_folder.exists())
				clear(src_meter_folder);

			srcmeter.calculateMetric(src_location, src_meter_out);

			LinkedHashMap<String, Double> temp = srcmeter.getResults(src_meter_folder);
			System.out.println("After SourceMeter: " + temp);
			if (temp == null) {
				for (int i = 0; i < 7; i++)
					metric_results.putAll(temp);
			}
			metric_results.putAll(temp);
			File andro_results = new File(result_dir + "androlyze\\" + src_location.getName() + id);

			if (build.buildApk(src_location)) {
				build.getApk(src_location);

				if (GradleBuild.compiled_apk != null && andro.calculateMetric(andro_results)) {
					System.out.println(GradleBuild.compiled_apk);
					temp = andro.getResults(andro_results);
					metric_results.putAll(temp);
				} else {
					System.err.println(src_location.getName() + ": Build failed!");
					apks_not_built.add(src_location.getName());
					metric_results.put("PERMISSIONS", -1.0);
				}

			} else {
				temp = andro.getResults(andro_results);
				metric_results.putAll(temp);
			}

			if (!result_file.exists())
				store.initCSV(result_file);
			store.writeCSV(result_file, src_location.getName() + id);
			metric_results.clear();

		} else
			System.err.println("Sourcecode-Directory is empty!");
		if (apks_not_built.isEmpty())
			System.out.println("No Problems occured!");
		else
			System.out.println("APKs not built: " + apks_not_built);

	}

	private void mainProcess(File src_location, File result_file) {

		Hulk hulk = new Hulk();
		Storage store = new Storage();
		SourceMeter srcmeter = new SourceMeter("SOURCE_METER_JAVA");
		GradleBuild build = new GradleBuild();
		Androlyze andro = new Androlyze("ANDROLYZE");
		
		startDatabase();

		if (src_location.isDirectory()) {
			String[] app_string = src_location.list();
			if (app_string[0] != null) {
				Arrays.stream(app_string).forEach(System.out::println);
				for (String s : app_string) {
					System.out.println(s);
					File project_src = new File(src_location, s);

					hulk.calculateMetric(project_src);
					LinkedHashMap<String, Double> hulk_results = hulk.getResults(project_src);

					File srcmeter_out = new File(result_dir + "SourceMeter\\" + s);
					if (!srcmeter_out.exists()) {
//						File imported_project = new File(workspace + s);
						srcmeter.calculateMetric(project_src);
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

					File andro_results = new File(result_dir + "androlyze\\" + s);

					if (!andro_results.exists() && build.buildApk(project_src)) {
						build.getApk(project_src);

						if (GradleBuild.compiled_apk != null && andro.calculateMetric(andro_results)) {
							System.out.println(GradleBuild.compiled_apk);
							LinkedHashMap<String, Double> permission = andro.getResults(andro_results);
							metric_results.putAll(permission);
						} else {
							System.err.println(s + ": Build failed!");
							apks_not_built.add(s);
							metric_results.put("Permissions", -1.0);
						}

					} else {
						LinkedHashMap<String, Double> permission = andro.getResults(andro_results);
						metric_results.putAll(permission);
					}

					if (!result_file.exists())
						store.initCSV(result_file);
					store.writeCSV(result_file, s);
					metric_results.clear();

				}
			} else
				System.err.println("Sourcecode-Directory is empty!");
		}
		if (apks_not_built.isEmpty())
			System.out.println("No Problems occured!");
		else
			System.out.println("APKs not built: " + apks_not_built);
	}

	private void startDatabase() {
		String mongod = System.getenv(this.env_variable_name_mongod);
		String cmd = "cd " + mongod + " && mongod";
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (windows) {
				process = run.exec("cmd /c \"" + cmd);
				process.waitFor();
			} else if (linux) {
				process = run.exec(cmd);
				process.waitFor();
			} else
				System.err.println("Program is not compatibel with the Operating System");

		} catch (IOException | InterruptedException e) {
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
