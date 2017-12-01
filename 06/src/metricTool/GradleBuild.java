package metricTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

public class GradleBuild {
		
	protected static File compiled_apk;

	protected boolean buildApk(File src_code) {
		if (new File(src_code, "build").exists()) {
			System.out.println("Build already exists!");
			return true;
		}		
		String cmd = "cd " + src_code.getPath() + " && gradlew assembleDebug";
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (Executor.windows) {
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
			} else if (Executor.linux)
				process = run.exec("./gradlew assembleDebug", null, src_code);
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

	protected boolean getApk(File project) {
		compiled_apk = null;
		File[] list = project.listFiles();
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
	
	protected boolean cleanBuild(File src_code){
		String cmd = "cd " + src_code.getPath() + " && gradlew clean";
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (Executor.windows) {
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
			} else if (Executor.linux)
				process = run.exec("./gradlew clean", null, src_code);
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

}
