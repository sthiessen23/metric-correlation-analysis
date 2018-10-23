/**
 * 
 */
package metric.correlation.analysis.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Level;
import org.gravity.eclipse.os.OperationSystem;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;

import metric.correlation.analysis.io.GitTools;

/**
 * This class provides the functionality to execute commands
 * 
 * @author speldszus
 *
 */
public class CommandExecuter {
	
	private CommandExecuter() {
		// As the class only provides static methods the class shouldn't be instantiated
	}

	/**
	 * Executes a command at the given location
	 * 
	 * Currently only Windows and Linux are supported!
	 * 
	 * @param location The location
	 * @param command The command
	 * @return true, iff the command has been executed successfully
	 * @throws UnsupportedOperationSystemException If the operation system is not supported
	 */
	public static boolean executeCommand(File location, String command) throws UnsupportedOperationSystemException {
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			switch (OperationSystem.getCurrentOS()) {
			case WINDOWS:
				final String cmd = "cmd /c \" cd " + location.getAbsolutePath() + " && " + command;
				process = run.exec(cmd);
				break;
			case LINUX:
				process = run.exec(command, null, location);
				break;
			default:
				throw new UnsupportedOperationSystemException("Program is not compatibel with the Operating System");
			}
			printErrorSteam(process);
			process.waitFor();
			process.destroy();
			return process.exitValue() == 0;
		} catch (IOException e) {
			GitTools.LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		} catch (InterruptedException e) {
			GitTools.LOGGER.log(Level.ERROR, e.getMessage(), e);
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * Loggs the error stream of the process
	 * 
	 * @param process The process
	 */
	private static void printErrorSteam(Process process) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				GitTools.LOGGER.log(Level.ERROR, "> " + line); //$NON-NLS-1$
			}
		} catch (IOException e) {
			GitTools.LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

}
