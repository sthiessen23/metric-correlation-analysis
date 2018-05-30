package metricTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class Download {

	protected void organizeDownloads(File downloadURLs) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(downloadURLs));
			String line = reader.readLine();
			String[] git_urls = line.substring(0, line.length()).split(",");
			reader.close();
			for (String url : git_urls) {
				if (!gitClone(url))
				System.err.println(url + ": not successfully cloned");
			}
			
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected boolean gitClone(String url) {
		File folder = new File(Executer.result_dir, "Sourcecode");
		folder.mkdirs();
		String cmd = "cd " + Executer.result_dir + "Sourcecode" + " && " + "git clone --recursive " + url;
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (Executer.windows)
				process = run.exec("cmd /c \"" + cmd);
			else if (Executer.linux) {
				cmd = "git clone --recursive " + url;
				process = run.exec(cmd, null, folder);
			} else {
				System.err.println("Program is not compatibel with the Operating System");
				return false;
			}
			try (BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				String line;
				while ((line = stream_reader.readLine()) != null) {
					System.err.println("> " + line); //$NON-NLS-1$
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			process.waitFor();
			process.destroy();
			return process.exitValue() == 0;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

	protected boolean changeVersion(File src_code, String id) {

		GradleBuild gb = new GradleBuild();

		System.out.println(id);
		File build_dir = new File(src_code, "build");
		if (build_dir.exists()) {
			gb.cleanBuild(src_code);
			Executer.clear(build_dir);
		}
		String cmd = "cd " + src_code.getPath() + " && " + "git checkout " + id + " .";
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if (Executer.windows) {
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
			} else if (Executer.linux) {
				cmd = "git checkout " + id + " .";
				process = run.exec(cmd, null, src_code);
			} else {
				System.err.println("Program is not compatibel with the Operating System");
				return false;
			}

			BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = stream_reader.readLine()) != null) {
				System.out.println("> " + line); //$NON-NLS-1$
			}
			process.waitFor();
			process.destroy();
			stream_reader.close();
			return true;

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		return false;
	}
}
