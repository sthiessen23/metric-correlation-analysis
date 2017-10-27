package metricTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

public class GradleBuild {
		
	public static File compiled_apk;

	public boolean buildApk(File in) {
		if (new File(in, "build").exists()) {
			System.out.println("Build already exists!");
			return true;
		}		
		String cmd = "cd " + in.getPath() + " && gradlew assembleDebug";
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0) {
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
			} else if (System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0)
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
		return false;
	}

	public boolean getApk(File in) {
		compiled_apk = null;
		File[] list = in.listFiles();
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
					getApk(fil);
			}
			return true;
		} else
			System.out.println("Directory is empty!");
		return false;
	}

}
