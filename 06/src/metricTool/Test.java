package metricTool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.gravity.eclipse.exceptions.NoConverterRegisteredException;
import org.gravity.hulk.HulkAPI;
import org.gravity.hulk.HulkAPI.AntiPatternNames;
import org.gravity.hulk.antipatterngraph.HAntiPattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.gravity.eclipse.importer.GradleImport;

public class Test {
	public final String env_variable_name_jadx = "JADX"; //$NON-NLS-1$
	public final String env_variable_name_srcmeter = "SOURCE_METER_JAVA"; //$NON-NLS-1$
	public final String env_variable_name_androlyze = "ANDROLYZE";
	public final String env_variable_name_mongod = "MONGOD";
	public List<String> results = new ArrayList<String>();
	public List<String> timelog = new ArrayList<String>();
	public List<String> apks_not_built = new ArrayList<String>();
	private static final String SRC_METER_FOLDER = "SrcMeter"; //$NON-NLS-1$
	public static List<IJavaProject> apk_project = new ArrayList<IJavaProject>();
	public static File compiled_apk;
	public static final String result_dir = "C:\\Users\\Biggi\\Documents\\strategie2\\";

	@org.junit.Test
	public void execute() {

		// File src_location = new File(result_dir + "Sourcecode");
		// File metric_results = new File(result_dir +
		// "results\\NewMetricResults.csv");
		// mainProcess(src_location, metric_results);
		importProject();
	}

	public static void main(String[] args) {

		Test t = new Test();
		File srcCode = new File(result_dir + "Test");
		File metric_results = new File(result_dir + "results\\TestResults1.csv");
//		t.mainProcess(srcCode, metric_results);
//		t.startDatabase();
		System.out.println(srcCode.getParent());

	}

	static boolean isWindowsSystem() {
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.indexOf("windows") >= 0;
	}

	static boolean isLinuxSystem() {
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.indexOf("linux") >= 0;
	}

	public void startDatabase() {
		String mongod = System.getenv(this.env_variable_name_mongod);
		String cmd = "cd " + mongod + " && mongod";
		System.out.println("MONGOD: " + mongod);
		Runtime run = Runtime.getRuntime();
		try {
			Process process = run.exec("cmd /c \"" + cmd);		
			process.waitFor();
			process.destroy();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void importProject() {

		String gradle = "C:\\Users\\Biggi\\.gradle";
		String android = "\"C:\\Program Files\\sdk-tools-windows-3859397\"";

		GradleImport gradleImport = new GradleImport(gradle, android);
		File location = new File(result_dir + "Test\\iosched");
		IJavaProject p;
		try {
			p = gradleImport.importGradleProject(location, "Iosched", new NullProgressMonitor());
			getAntipatterns(p);
		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	public void mainProcess(File src_location, File metric_results) {
		initCSV(metric_results);
		if (src_location.isDirectory()) {
			String[] app_string = src_location.list();
			if (app_string[0] != null) {
				Arrays.stream(app_string).forEach(System.out::println);
				for (String s : app_string) {
					System.out.println(s);
					File app_file = new File(src_location, s);
					File srcmeter_out = new File(result_dir + "SourceMeter\\" + s);
					if (!srcmeter_out.exists()) {
						calculateMetrics(app_file, srcmeter_out);
					}
					File src_meter_folder = new File(srcmeter_out, SRC_METER_FOLDER);
					if (!getMetrics(src_meter_folder)) {
						for (int i = 0; i < 7; i++)
							results.add(null);
					}
					File andro_results = new File(result_dir + "androlyze\\" + s);
					if (!andro_results.exists()) {
						if (buildApk(app_file)) {
							getApkFile(app_file);
							if (compiled_apk != null) {
								System.out.println(compiled_apk);
								if (getPermissions(compiled_apk, andro_results))
									parseJsonFile(andro_results);
							} else {
								System.err.println(s + ": Build failed!");
								apks_not_built.add(s);
							}
						}
					} else
						parseJsonFile(andro_results);
					writeCSV(metric_results, s);
					results.clear();
				}
			} else
				System.err.println("Sourcecode-Directory is empty!");
		}
		if (apks_not_built.isEmpty())
			System.out.println("No Problems occured!");
		else
			System.out.println(apks_not_built);
	}

	public boolean buildApk(File srcCode) {
		if (new File(srcCode, "build").exists()) {
			System.out.println("Build already exists!");
			return true;
		}
		boolean apk_build = false;
		String cmd = "cd " + srcCode.getPath() + " && gradlew assembleDebug";
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (isWindowsSystem()) {
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
			} else if (isLinuxSystem())
				process = run.exec(cmd + " && exit\"");
			else
				return false;
			BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = stream_reader.readLine()) != null) {
				System.out.println("> " + line); //$NON-NLS-1$
			}
			process.waitFor();
			process.destroy();
			stream_reader.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return apk_build;
	}

	public boolean getApkFile(File file) {
		compiled_apk = null;
		File[] list = file.listFiles();
		// Arrays.stream(list).forEach(System.out::println);
		if (list != null) {
			for (File fil : list) {
				if (compiled_apk != null)
					break;
				if (!fil.isDirectory()) {

					FilenameFilter filter = new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.endsWith(".apk");
						};
					};
					File temp = new File(fil.getParent());
					File[] apklist = temp.listFiles(filter);
					// Arrays.stream(apklist).forEach(System.out::println);
					if (apklist.length > 0) {
						compiled_apk = apklist[0];
						// System.out.println(compiled_apk);
						return true;
					}
				} else
					getApkFile(fil);
			}
			return true;
		} else
			System.out.println("Directory is empty!");
		return false;
	}

	public int getAntipatterns(IJavaProject p) {
		List<HAntiPattern> hulkResults = null;

		try {
			hulkResults = HulkAPI.detect(p, new NullProgressMonitor(), AntiPatternNames.Blob, AntiPatternNames.IGAM,
					AntiPatternNames.IGAT);
			System.out.println("Antipatterns: " + hulkResults.size());

		} catch (NoConverterRegisteredException e) {

			e.printStackTrace();
		}
		return hulkResults.size();
	}

	/*
	 * TODO für Linux cmd-Befehl testen
	 */
	public boolean getPermissions(File apk, File andro_results) {

		String andro = System.getenv(this.env_variable_name_androlyze);

		String andro_cmd = "cd " + andro + " && " + "androlyze.py " + "analyze " + "CodePermissions.py " + "--apks "
				+ apk + " -pm  non-parallel";

		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (isWindowsSystem())
				process = run.exec("cmd /c \"" + andro_cmd + " && exit\"");
			else if (isLinuxSystem())
				process = run.exec(andro_cmd + " && exit\"");
			else
				return false;

			BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;

			while ((line = stream_reader.readLine()) != null) {
				System.out.println("> " + line); //$NON-NLS-1$
			}
			process.waitFor();
			process.destroy();
			stream_reader.close();

			File location = new File(andro + "\\storage\\res");
			String[] fileList = location.list();
			if (fileList.length > 0) {
				String jsonPath = andro + "\\storage\\res\\" + fileList[0];
				File f = new File(jsonPath);

				while (!f.isFile()) {
					String[] s = f.list();
					jsonPath = jsonPath + "\\" + s[0];
					f = new File(jsonPath);

				}
				Files.move(f.toPath(), andro_results.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE,
						java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				clear(location);
				return true;
			}

			return false;

		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	public boolean parseJsonFile(File andro_results) {

		JSONParser parser = new JSONParser();
		Object obj;
		try {
			FileReader reader = new FileReader(andro_results);
			obj = parser.parse(reader);
			JSONObject jsonObject = (JSONObject) obj;
			Object name = jsonObject.get("code permissions");
			JSONObject j = (JSONObject) name;
			Object per = j.get("listing");
			JSONObject j2 = (JSONObject) per;

			@SuppressWarnings("unchecked")
			Iterator<JSONArray> iterator = j2.values().iterator();
			int sumPermissions = 0;
			int sumNotUsedPermissions = 0;

			while (iterator.hasNext()) {
				sumPermissions++;
				JSONArray jsonArray = iterator.next();
				if (jsonArray.isEmpty()) {
					sumNotUsedPermissions++;
				}
			}
			DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
			dfs.setDecimalSeparator('.');
			DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
			double permission_metric = (double) sumNotUsedPermissions / (double) sumPermissions;
			results.add(String.valueOf(dFormat.format(permission_metric)));
			System.out.println("All: " + sumPermissions + ", NotUsed: " + sumNotUsedPermissions);
			System.out.println(results);
			reader.close();
			return true;
		} catch (IOException | ParseException e) {

			return false;
		}

	}

	public boolean createGradleSrcFolder(IJavaProject project, NullProgressMonitor monitor) {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		IFolder src = project.getProject().getFolder("src/main/java");
		if (!src.exists())
			return false;

		IClasspathEntry srcEntry = JavaCore.newSourceEntry(src.getFullPath());

		IClasspathEntry[] classpath;
		try {
			classpath = project.getRawClasspath();
			for (IClasspathEntry e : classpath) {
				if (e.getPath().equals(src.getFullPath())) {
					return true;
				}
			}
			IClasspathEntry[] newClasspath = new IClasspathEntry[classpath.length + 1];
			System.arraycopy(classpath, 0, newClasspath, 1, classpath.length);
			newClasspath[0] = srcEntry;

			project.setRawClasspath(newClasspath, monitor);
		} catch (JavaModelException e) {
			return false;
		}

		return true;
	}

	public String decompileApk(File in, File out) {

		String jadx = System.getenv(this.env_variable_name_jadx);
		String cmd = jadx + " -d" + " " + out.toString() + " -e " + in.toString();

		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (isWindowsSystem())
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
			else if (isLinuxSystem())
				process = run.exec(cmd + " && exit\"");
			else
				return "Program is not compatibel with the Operating System";
			BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = stream_reader.readLine()) != null) {
				System.out.println("> " + line); //$NON-NLS-1$
			}
			process.waitFor();
			process.destroy();
			stream_reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return "ok";
	}

	public boolean calculateMetrics(File in, File out) {

		String src_meter = System.getenv(this.env_variable_name_srcmeter);

		String cmd = src_meter + " -projectName=" + SRC_METER_FOLDER + //$NON-NLS-1$
				" -projectBaseDir=" + in.toString() + //$NON-NLS-1$
				" -resultsDir=" + out.toString(); //$NON-NLS-1$

		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (isWindowsSystem())
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
			else if (isLinuxSystem())
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
			else {
				System.out.println("Program is not compatibel with the Operating System");
				return false;
			}

			BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = stream_reader.readLine()) != null) {
				System.out.println("> " + line); //$NON-NLS-1$
			}
			stream_reader.close();
			process.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return true;
	}

	public boolean getMetrics(File src_meter_folder) {
		File[] java_folder = new File(src_meter_folder, "java").listFiles(); //$NON-NLS-1$

		if (java_folder.length > 0) {
			try {
				File metrics = new File(java_folder[0], "SrcMeter-Class.csv"); // $NON-NLS-1$
				BufferedReader file_reader = new BufferedReader(new FileReader(metrics));
				String line = file_reader.readLine();
				if (line == null) {
					file_reader.close();
					System.err.println("Sourcemeter metric file is empty");
				}
				String[] names = line.substring(1, line.length() - 1).split("\",\""); //$NON-NLS-1$
				String[] metric_names = { "LOC", "WMC", "CBO", "LCOM5", "DIT", "LDC" };
				List<Double> class_values = new ArrayList<Double>();

				for (String s : metric_names) {
					int metric_index = Arrays.asList(names).indexOf(s);
					System.out.println(s + ": " + metric_index);
					try {
						String[] files = { "SrcMeter-Class.csv", "SrcMeter-Enum.csv" };
						for (String f : files) {
							metrics = new File(java_folder[0], f); // $NON-NLS-1$
							BufferedReader metric_reader = new BufferedReader(new FileReader(metrics));
							String m_line = metric_reader.readLine();
							while ((m_line = metric_reader.readLine()) != null) {
								String[] values = m_line.substring(1, m_line.length() - 1).split("\",\""); //$NON-NLS-1$
								class_values.add(Double.parseDouble(values[metric_index]));
							}
							metric_reader.close();
						}

						double sum = 0;
						for (int i = 0; i < class_values.size(); i++) {
							sum += class_values.get(i);
						}
						DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
						dfs.setDecimalSeparator('.');
						DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
						if (s == "LOC") {
							results.add(String.valueOf(sum));
							double average = sum / class_values.size();
							results.add(dFormat.format(average).toString());
						} else {

							double average = sum / class_values.size();
							results.add(dFormat.format(average).toString());
						}
						class_values.clear();
						file_reader.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
				file_reader.close();
				// clear(src_meter_folder);
				return true;
			} catch (IOException e) {
				System.out.println("critical error");
				return false;
			}

		}
		return false;
	}

	public void initCSV(File metric_results, File time_log) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(metric_results));
			writer.write("Application Name," + "LOC," + "LOCpC," + "WMC," + "CBO," + "LCOM5," + "DIT," + "LDC,"
					+ "Permissions");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(time_log));
			writer.write("Application Name," + "JADX_start," + "SourceMeter_start," + "AndroLyze," + "End");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void initCSV(File metric_results) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(metric_results));
			writer.write("Application Name," + "LOC," + "LOCpC," + "WMC," + "CBO," + "LCOM5," + "DIT," + "LDC,"
					+ "Permissions");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeCSV(File metric_results, File time_log, String apk) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(metric_results, true));
			ListIterator<String> iterator = results.listIterator();
			writer.newLine();
			writer.write(apk + ", ");
			while (iterator.hasNext()) {
				String s = iterator.next();
				if (iterator.hasNext()) {
					writer.write(s + ", ");
				} else
					writer.write(s);
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(time_log, true));
			ListIterator<String> iterator = timelog.listIterator();
			writer.newLine();
			writer.write(apk + ", ");
			while (iterator.hasNext()) {
				String s = iterator.next();
				if (iterator.hasNext()) {
					writer.write(s + ", ");
				} else
					writer.write(s);
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeCSV(File metric_results, String apk) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(metric_results, true));
			ListIterator<String> iterator = results.listIterator();
			writer.newLine();
			writer.write(apk + ", ");
			while (iterator.hasNext()) {
				String s = iterator.next();
				if (iterator.hasNext()) {
					writer.write(s + ", ");
				} else
					writer.write(s);
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getTimestamp() {
		Date date = new Date();
		long time = date.getTime();
		Timestamp ts = new Timestamp(time);
		timelog.add(ts.toString());
	}

	private void clear(File file) {
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
