package metricTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.os.OperationSystem;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Androlyze implements IMetricCalculator {

	public static final String env_variable_name_androlyze = "ANDROLYZE";
	public static final String env_variable_name_mongod = "MONGOD";
	
	private final File androlyzeDir;

	public Androlyze() throws MetricCalculatorInitializationException {
		String andro = System.getenv(env_variable_name_androlyze);
		if (andro == null) {
			throw new MetricCalculatorInitializationException("Androlyze environment variable not set!");
		}
		File androlyzeDir = new File(andro);
		if (!androlyzeDir.exists()) {
			throw new MetricCalculatorInitializationException(
					"The location \"" + androlyzeDir.getAbsolutePath() + "\" doesn't exist.");
		}
		if(androlyzeDir.isFile()) {
			androlyzeDir = androlyzeDir.getParentFile();
		}
		if(!new File(androlyzeDir, "androanalyze").exists()) {
			throw new MetricCalculatorInitializationException(
					"Androlyze executable not found in \"" + androlyzeDir.getAbsolutePath() + "\".");
		}
		this.androlyzeDir = androlyzeDir;
		
		startDatabase();
	}

	private boolean startDatabase() throws MetricCalculatorInitializationException {

		String mongod = System.getenv(env_variable_name_mongod);
		if (mongod == null) {
			throw new MetricCalculatorInitializationException("Environment variable \"" + env_variable_name_mongod + "\" not set.");
		}
		File mongodFile = new File(mongod);
		if(!mongodFile.exists()) {
			throw new MetricCalculatorInitializationException("Location \"" + env_variable_name_mongod + "\" not found.");
		}
		if(mongodFile.isDirectory()) {
			mongodFile = new File(mongodFile, "mongod");
			if(!mongodFile.exists()) {
				throw new MetricCalculatorInitializationException("Location \"" + env_variable_name_mongod + "\" not found.");
			}
		}
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			switch(OperationSystem.getCurrentOS()) {
				case WINDOWS:
					process = run.exec("cmd /c \"" + mongodFile.getAbsolutePath());
				break;
				case LINUX:
					process = run.exec(mongodFile.getAbsolutePath());
				break;
				default:
					throw new MetricCalculatorInitializationException("Program is not compatibel with the Operating System");		
			}
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))){
				String line;
				while((line = reader.readLine())!= null){
					System.err.println("MONGO_DB: "+line);
				}
			}
		} catch (IOException e) {
			throw new MetricCalculatorInitializationException(e);
		}
		return true;
	}

	
	@Override
	public boolean calculateMetric(IJavaProject project) {
		File compiled_apk;
		try {
			compiled_apk = GradleBuild.buildApk(project.getProject().getLocation().toFile());
		} catch (UnsupportedOperationSystemException e1) {
			e1.printStackTrace();
			return false;
		}
		String andro_cmd = "cd " + androlyzeDir + " && " + "androlyze.py " + "analyze " + "CodePermissions.py "
				+ "--apks " + compiled_apk + " -pm  non-parallel";

		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			switch (OperationSystem.getCurrentOS()) { case WINDOWS:
				process = run.exec("cmd /c \"" + andro_cmd + " && exit\"");break;
			case LINUX:
				andro_cmd = "./androanalyze scripts_builtin/CodePermissions.py --apks " + compiled_apk;
				process = run.exec(andro_cmd, null, androlyzeDir);
			break;
			default:
				return false;}

			try (BufferedReader stream = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				String line;
				while ((line = stream.readLine()) != null) {
					System.err.println("ANDROLYZE: " + line);
				}
			}
			process.waitFor();
			process.destroy();

			return true;

		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	@Override
	public LinkedHashMap<String, Double> getResults() {
		File resultsLocation = new File(androlyzeDir, "storage" + File.separator + "res");
		
		LinkedHashMap<String, Double> metric_results = new LinkedHashMap<String, Double>();

		JSONParser parser = new JSONParser();
		Object obj;
		try {
			FileReader reader = new FileReader(resultsLocation);
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
			metric_results.put("PERMISSIONS", Double.parseDouble(dFormat.format(permission_metric)));

			System.out.println(sumPermissions);
			System.out.println(sumNotUsedPermissions);

			reader.close();
			return metric_results;

		} catch (IOException | ParseException e) {
			metric_results.put("PERMISSIONS", -1.0);
			return metric_results;
		}
	}

}
