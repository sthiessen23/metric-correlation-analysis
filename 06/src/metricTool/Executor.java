package metricTool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;

public class Executor {

	public static boolean windows = System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0;
	public static boolean linux = System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0;
	public static List<String> timelog = new ArrayList<String>();
	public static LinkedHashMap<String, Double> metric_results = new LinkedHashMap<String, Double>();
	public static String result_dir = "C:\\Users\\Biggi\\Documents\\Strategie3\\";
	public List<String> apks_not_built = new ArrayList<String>();
	public final String env_variable_name_mongod = "MONGOD";
	public List<String> downloadURLs = new ArrayList<String>();

	public static void main(String[] args) {
		Executor exe = new Executor();
		Download d = new Download();
		String url = "https://github.com/siacs/Conversations.git";
		File src_location = new File(result_dir + "VersionTemp\\Sourcecode\\Conversations");
		File version_ids = new File(result_dir + "VersionTemp\\VersionIds.csv");
		File result_file = new File(result_dir + "VersionTemp\\results\\Test3.csv");
		d.changeVersion(src_location, version_ids);
//		if (d.gitClone(url)) {
//			System.out.println("Successfully cloned");
//
//		}
//		exe.mainProcess(src_location, 1, result_file);
	}

	@Test
	public void execute() {
		File src_location = new File(result_dir + "Sourcecode");
		File result_file = new File(result_dir + "results\\Test3.csv");
		mainProcess(src_location, result_file);

	}

	public void mainProcess(File src_location, int id, File result_file) {
		System.out.println("Started");
		Storage store = new Storage();
		SourceMeter srcmeter = new SourceMeter("SOURCE_METER_JAVA");
		GradleBuild build = new GradleBuild();
		Androlyze andro = new Androlyze("ANDROLYZE");
		startDatabase();
		System.out.println(src_location.toPath());
		if (src_location.exists()) {
			File src_meter_out = new File(result_dir + "VersionTemp\\SourceMeter\\" + src_location.getName());
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
			File andro_results = new File(result_dir + "VersionTemp\\androlyze\\" + src_location.getName() + "id");
			
			
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

	public void mainProcess(File src_location, File result_file) {
		System.out.println("Started");
		Storage store = new Storage();
		SourceMeter srcmeter = new SourceMeter("SOURCE_METER_JAVA");
		GradleBuild build = new GradleBuild();
		Androlyze andro = new Androlyze("ANDROLYZE");
		startDatabase();
		System.out.println(src_location.toPath());
		if (src_location.isDirectory()) {
			String[] app_string = src_location.list();
			if (app_string[0] != null) {
				Arrays.stream(app_string).forEach(System.out::println);
				for (String s : app_string) {
					System.out.println(s);
					File project_src = new File(src_location, s);
					File srcmeter_out = new File(result_dir + "SourceMeter\\" + s);
					if (!srcmeter_out.exists()) {
						srcmeter.calculateMetric(project_src);
					}
					File src_meter_folder = new File(srcmeter_out, "SrcMeter");
					System.out.println(src_meter_folder);
					LinkedHashMap<String, Double> temp = srcmeter.getResults(src_meter_folder);
					System.out.println("After SourceMeter: " + temp);
					if (temp == null) {
						for (int i = 0; i < 7; i++)
							metric_results.putAll(temp);
					}
					metric_results.putAll(temp);
					File andro_results = new File(result_dir + "androlyze\\" + s);

					if (!andro_results.exists() && build.buildApk(project_src)) {
						build.getApk(project_src);

						if (GradleBuild.compiled_apk != null && andro.calculateMetric(andro_results)) {
							System.out.println(GradleBuild.compiled_apk);
							temp = andro.getResults(andro_results);
							metric_results.putAll(temp);
						} else {
							System.err.println(s + ": Build failed!");
							apks_not_built.add(s);
							metric_results.put("PERMISSIONS", -1.0);
						}

					} else {
						temp = andro.getResults(andro_results);
						metric_results.putAll(temp);
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

	public void startDatabase() {
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

	public static void clear(File file) {
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
