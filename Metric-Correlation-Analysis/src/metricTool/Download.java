package metricTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Stack;

import org.gravity.eclipse.os.OperationSystem;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;

public class Download {

	protected static void organizeDownloads(File downloadURLs, File destination) throws UnsupportedOperationSystemException {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(downloadURLs));
			String line = reader.readLine();
			String[] git_urls = line.substring(0, line.length()).split(",");
			reader.close();
			for (String url : git_urls) {
				try {
					gitClone(url, destination);
				} catch (GitCloneException e) {
					System.err.println(url + ": not successfully cloned");
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected static boolean gitClone(String url, File destination) throws UnsupportedOperationSystemException, GitCloneException {
		return gitClone(url, destination, false);
	}

	protected static boolean gitClone(String url, File destination, boolean replace) throws UnsupportedOperationSystemException, GitCloneException {
		if (!destination.exists()) {
			destination.mkdirs();
		}
		String name = url.substring(url.lastIndexOf('/') + 1, url.length() - 4);
		File repository = new File(destination, name);
		if(repository.exists()) {
			if(replace) {
				Stack<File> files = new Stack<>();
				files.add(repository);
				Stack<File> delete = new Stack<>();
				delete.add(repository);
				while(!files.isEmpty()) {
					File next = files.pop();
					for(File f : next.listFiles()) {
						if(f.isDirectory()) {
							files.add(f);
							delete.add(f);
						}
						else {
							f.delete();
						}
					}
				}
				while(!delete.isEmpty()) {
					delete.pop().delete();
				}
			}
			else {
				throw new GitCloneException("There is already a repository with the name \""+name+ "\".");
			}
		}
		String cmd = "cd " + destination.getAbsolutePath() + " && " + "git clone --recursive " + url;
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			switch (OperationSystem.getCurrentOS()) {
			case WINDOWS:
				process = run.exec("cmd /c \"" + cmd);
				break;
			case LINUX:
				cmd = "git clone --recursive " + url;
				process = run.exec(cmd, null, destination);
				break;
			default:
				throw new UnsupportedOperationSystemException("Program is not compatibel with the Operating System");
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

	protected static boolean changeVersion(File src_code, String id) throws UnsupportedOperationSystemException {

		System.out.println(id);
		File build_dir = new File(src_code, "build");
		if (build_dir.exists()) {
			GradleBuild.cleanBuild(src_code);
			Executer.clear(build_dir);
		}
		String cmd = "cd " + src_code.getPath() + " && " + "git checkout " + id + " .";
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			switch (OperationSystem.getCurrentOS()) {
			case WINDOWS:
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
				break;
			case LINUX:
				cmd = "git checkout " + id + " .";
				process = run.exec(cmd, null, src_code);
				break;
			default:
				throw new UnsupportedOperationSystemException("Program is not compatibel with the Operating System");
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
