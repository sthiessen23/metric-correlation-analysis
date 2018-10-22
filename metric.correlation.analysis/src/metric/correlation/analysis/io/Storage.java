package metric.correlation.analysis.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

public class Storage {

	private final List<String> keys;
	private final File output;
	
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
			writer.close();
		}
	}

	public boolean writeCSV(String name, Hashtable<String, Double> results) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(output, true))) {
			writer.newLine();
			writer.write(name);
			for(String key : keys) {
				writer.write("," + results.get(key));
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
