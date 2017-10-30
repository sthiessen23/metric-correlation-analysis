package metricTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Androlyze implements MetricCalculator {

	public String env_variable_name_androlyze = "ANDROLYZE";

	public Androlyze(String env_name) {
		this.env_variable_name_androlyze = env_name;
	}

	@Override
	public boolean calculateMetric(File in) {
		
		String andro = System.getenv(this.env_variable_name_androlyze);
		String andro_cmd = "cd " + andro + " && " + "androlyze.py " + "analyze " + "CodePermissions.py " + "--apks "
				+ GradleBuild.compiled_apk + " -pm  non-parallel";

		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (windows)
				process = run.exec("cmd /c \"" + andro_cmd + " && exit\"");
			else if (linux)
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

				try {
					while (!f.isFile()) {
						String[] s = f.list();
						jsonPath = jsonPath + "\\" + s[0];
						f = new File(jsonPath);

					}
				} catch (NullPointerException e) {
					System.err.println("Androlyze could't store JSON file.");
					return false;
				}
				Files.move(f.toPath(), in.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE,
						java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				Executor.clear(location);
				return true;
			}

			return false;

		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	@Override
	public LinkedHashMap<String,Double> getResults(File in) {
		
		LinkedHashMap<String, Double> metric_results = new LinkedHashMap<String, Double>();
		
		JSONParser parser = new JSONParser();
		Object obj;
		try {
			FileReader reader = new FileReader(in);
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
			
			reader.close();
			return metric_results;
			
		} catch (IOException | ParseException e) {
			metric_results.put("PERMISSIONS", -1.0);
			return metric_results;
		}
	}

	

}
