package metric.correlation.analysis.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Stores metric calculation results as csv
 * 
 * @author speldszus
 *
 */
public class Storage {

	private static final Logger LOGGER = Logger.getLogger(Storage.class);
	
	private final List<String> keys;
	private final File output;
	
	/**
	 * Creates a new instance with a given output file and the names of the metrics
	 * 
	 * @param resultFile The output file
	 * @param keys The metric names
	 * @throws IOException If the output file cannot be created
	 */
	public Storage(File resultFile, Collection<String> keys) throws IOException {
		this.output = resultFile;
		if (keys instanceof List) {
			this.keys = (List<String>) keys;
			
		}
		else {
			this.keys = new ArrayList<String>(keys);
		}
		
		if(!resultFile.exists()) {
			resultFile.getParentFile().mkdirs();
			resultFile.createNewFile();
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile))){
			writer.write("Application-Name");
			for (String key : keys) {
				writer.write("," + key);
			}
		}
	}

	/**
	 * Appends the results of a detection run on a project
	 * 
	 * @param name The project name
	 * @param results The metric results
	 * @return true, iff the results have been appended successfully
	 */
	public boolean writeCSV(String name, Map<String, Double> results) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(output, true))) {
			writer.newLine();
			writer.write(name);
			for(String key : keys) {
				writer.write("," + results.get(key));
			}
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		}
		return true;
	}

}
