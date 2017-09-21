package metricTool;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.gravity.hulk.HulkAPI;
import org.gravity.hulk.HulkAPI.AntiPatternNames;
import org.gravity.hulk.antipatterngraph.HAntiPattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import metricTool.GradleImport;

//import de.uni_hamburg.informatik.swt.accessanalysis.Analysis;
//import de.uni_hamburg.informatik.swt.accessanalysis.AnalysisFactory;
//import de.uni_hamburg.informatik.swt.accessanalysis.AnalysisFactory.AnalysisMode;
//import de.uni_hamburg.informatik.swt.accessanalysis.results.ResultFormatter;
//import de.uni_hamburg.informatik.swt.accessanalysis.AnalysisException;

import org.gravity.eclipse.exceptions.NoConverterRegisteredException;

public class Test {
	public final String env_variable_name_jadx = "JADX"; //$NON-NLS-1$
	public final String env_variable_name_srcmeter = "SOURCE_METER_JAVA"; //$NON-NLS-1$
	public final String env_variable_name_androlyze = "ANDROLYZE";
	public final String env_variable_name_mongod = "MONGOD";
	public List<String> results = new ArrayList<String>();
	public List<String> timelog = new ArrayList<String>();
	private static final String SRC_METER_FOLDER = "SrcMeter"; //$NON-NLS-1$
	public static List<IJavaProject> apk_project = new ArrayList<IJavaProject>();

	@org.junit.Test
	public void execute() {

		// IProject myProject0 =
		// ResourcesPlugin.getWorkspace().getRoot().getProject("ColorNote.apk");
		// IJavaProject p0 = JavaCore.create(myProject0);
		// apk_project.add(p0);
		// getAntipatterns(p0);

		// IProject myProject1 =
		// ResourcesPlugin.getWorkspace().getRoot().getProject("Torch.apk");
		// IJavaProject p1 = JavaCore.create(myProject1);
		// apk_project.add(p1);
		//
		 IProject myProject2 =
		 ResourcesPlugin.getWorkspace().getRoot().getProject("com.facebook.lite.apk");
		 IJavaProject p2 = JavaCore.create(myProject2);
		 apk_project.add(p2);
		 getAntipatterns(p2);
		//
		// IProject myProject3 =
		// ResourcesPlugin.getWorkspace().getRoot().getProject("FlappyBirdv1.2.apk");
		// IJavaProject p3 = JavaCore.create(myProject3);
		// apk_project.add(p3);
		//
		// IProject myProject4 =
		// ResourcesPlugin.getWorkspace().getRoot().getProject("info.free.followers1.1.apk");
		// IJavaProject p4 = JavaCore.create(myProject4);
		// apk_project.add(p4);
		//
		// IProject myProject5 =
		// ResourcesPlugin.getWorkspace().getRoot().getProject("net.openvpn.openvpn.76.apk");
		// IJavaProject p5 = JavaCore.create(myProject5);
		// apk_project.add(p5);
		//
		// for (IJavaProject i : apk_project) {
		// if (!createGradleSrcFolder(i, null)) {
		// fail("Adding src folder to classpath failed.");
		// }
		// getAntipatterns(i);
		// }

		Test t = new Test();
		File apks = new File("C:\\Users\\Biggi\\Documents\\all_apks");
		File metric_results = new File("C:\\Users\\Biggi\\Documents\\results\\MetricResults.csv");
		File time_log = new File("C:\\Users\\Biggi\\Documents\\results\\TimeLog.csv");
		//t.bigProcess(apks, metric_results, time_log);

		// File location = new
		// File("C:\\Users\\Biggi\\Documents\\myProject3\\Torch.apk");
		//
		// IProject project = GradleImport.importProject(location);
		// IJavaProject iProject = JavaCore.create(project);
		// createGradleSrcFolder(iProject, new NullProgressMonitor());
	}

	public static void main(String args[]) {
		Test t = new Test();
		File apks = new File("C:\\Users\\Biggi\\Documents\\apks");
		File metric_results = new File("C:\\Users\\Biggi\\Documents\\results\\MetricResults.csv");
		File time_log = new File("C:\\Users\\Biggi\\Documents\\results\\TimeLog.csv");
		t.bigProcess(apks, metric_results, time_log);

		File location = new File("");
		IProject project = GradleImport.importProject(location);
		IJavaProject iProject = JavaCore.create(project);
		t.createGradleSrcFolder(iProject, new NullProgressMonitor());
	}

	public void getTimestamp() {
		Date date = new Date();
		long time = date.getTime();
		Timestamp ts = new Timestamp(time);
		timelog.add(ts.toString());
	}

	public void bigProcess(File apks, File metric_results, File time_log) {
		initCSV(metric_results, time_log);
		if (apks.isDirectory()) {
			String[] apk_string = apks.list();
			if (apk_string[0] != null) {
				for (String s : apk_string) {
					File apk_file = new File(apks, s);
					File src_code = new File("C:\\Users\\Biggi\\Documents\\myProject5\\" + s);
					getTimestamp();
					if (!src_code.exists()) {
						decompileApk(apk_file, src_code);
					}
					File srcmeter_out = new File("C:\\Users\\Biggi\\Documents\\myProject5\\" + s + "SrcMeter");
					getTimestamp();
					if (!srcmeter_out.exists()) {
						calculateMetrics(src_code, srcmeter_out);
					}
					getTimestamp();
					if (getPermissions(apk_file)) {
						getTimestamp();
						writeCSV(metric_results, time_log, s);
						results.clear();
						timelog.clear();
					}
				}

			} else
				System.err.println("APK-Directory is empty!");
		} else
			System.err.println("Denoted path is no directory!");

	}

	public boolean getAntipatterns(IJavaProject p) {
		List<HAntiPattern> hulkResults;
		try {
			hulkResults = HulkAPI.detect(p, new NullProgressMonitor(), AntiPatternNames.Blob);
			System.out.println(hulkResults.size());
			hulkResults.size();

		} catch (NoConverterRegisteredException e) {

			e.printStackTrace();
		}
		return false;
	}

	public boolean getPermissions(File apk) {

		String andro = System.getenv(this.env_variable_name_androlyze);

		String andro_cmd = "cd " + andro + " && " + "androlyze.py " + "analyze " + "CodePermissions.py " + "--apks "
				+ apk + " -pm  non-parallel";

		Runtime run = Runtime.getRuntime();
		try {
			Process process = run.exec("cmd /c \"" + andro_cmd + " && exit\"");
			BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;

			while ((line = stream_reader.readLine()) != null) {
				System.out.println("> " + line); //$NON-NLS-1$
			}
			process.waitFor();
			process.destroy();
			stream_reader.close();

			String path = andro + "\\storage\\res";
			File location = new File(path);
			String[] fileList = location.list();

			String jsonPath = path + "\\" + fileList[0];
			File f = new File(jsonPath);

			while (!f.isFile()) {
				String[] s = f.list();
				jsonPath = jsonPath + "\\" + s[0];
				f = new File(jsonPath);

			}
			if (parseJsonFile(jsonPath))
				return true;

			return false;

		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	public boolean parseJsonFile(String jsonPath) {

		JSONParser parser = new JSONParser();
		Object obj;
		try {
			obj = parser.parse(new FileReader(jsonPath));
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

			results.add(String.valueOf(sumPermissions));
			results.add(String.valueOf(sumNotUsedPermissions));
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
			Process process = run.exec("cmd /c \"" + cmd + " && exit\"");
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

	public String calculateMetrics(File in, File out) {

		String src_meter = System.getenv(this.env_variable_name_srcmeter);
		File src_meter_folder = new File(out, SRC_METER_FOLDER);
		String cmd = src_meter + " -projectName=" + SRC_METER_FOLDER + //$NON-NLS-1$
				" -projectBaseDir=" + in.toString() + //$NON-NLS-1$
				" -resultsDir=" + out.toString(); //$NON-NLS-1$

		Runtime run = Runtime.getRuntime();
		try {
			Process process = run.exec("cmd /c \"" + cmd + " && exit\"");
			BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = stream_reader.readLine()) != null) {
				System.out.println("> " + line); //$NON-NLS-1$
			}
			stream_reader.close();
			process.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
			return "error";
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		getMetrics(src_meter_folder);
		return "ok";
	}

	public void getMetrics(File src_meter_folder) {
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
						// Durchschnitt bilden
						double sum = 0;
						for (int i = 0; i < class_values.size(); i++) {
							sum += class_values.get(i);
						}
						if (s == "LOC") {
							results.add(String.valueOf(sum));
						} else {
							DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
							dfs.setDecimalSeparator('.');
							DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
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

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public void initCSV(File metric_results, File time_log) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(metric_results));
			writer.write("Application Name," + "LOC," + "WMC," + "CBO," + "LCOM5," + "DIT," + "LDC," + "SumPermissions,"
					+ "SumNotUsedPermissions");
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

	// private void clear(File file) {
	// if (file.exists()) {
	// for (File f : file.listFiles()) {
	// if (f.isDirectory()) {
	// clear(f);
	// f.delete();
	// } else {
	// f.delete();
	// }
	// }
	// }
	// }

	// public boolean printAccessibilityMetric(File folder, NullProgressMonitor
	// monitor) {
	// for(IJavaProject p:apk_project){
	// p.getProject().deleteMarkers(null, true, IResource.DEPTH_INFINITE);
	// }
	// try {
	// Analysis accessAnalysis = AnalysisFactory.analyzer(apk_project,
	// AnalysisMode.ACCESS_QUIET);
	// accessAnalysis.run(monitor);
	// ResultFormatter formatter =
	// accessAnalysis.getResults().get(0).getFormatter();
	// Files.write(new File(folder, "accessMetrics.txt").toPath(),
	// ("igat = " + formatter.igat() + " igam = " +
	// formatter.igam()).getBytes());
	// } catch (AnalysisException | IOException e) {
	// return false;
	// }
	// return true;
	// }

}
