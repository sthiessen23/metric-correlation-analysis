package metricTool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public class Storage {
	
	protected void initCSV(File result_file) {
		try {
			Object[] obj = Executer.metric_results.keySet().toArray();
			BufferedWriter writer = new BufferedWriter(new FileWriter(result_file));
			writer.write("Application Name");
			for(int i=0; i< obj.length; i++){
				writer.write("," + obj[i].toString());
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	protected void writeCSV(File result_file, String apk_name) {
		try {
			Set<String> set = Executer.metric_results.keySet();
			Iterator<String> iterator = set.iterator();
			BufferedWriter writer = new BufferedWriter(new FileWriter(result_file, true));
			writer.newLine();
			writer.write(apk_name);
			while (iterator.hasNext()) {
				writer.write("," + Executer.metric_results.get(iterator.next()));
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
