package metric.correlation.analysis.test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingReport;
import metric.correlation.analysis.MetricCalculation;
import metric.correlation.analysis.configuration.ProjectConfiguration;

public class ExecutionTest {

	private static final Logger LOGGER = Logger.getLogger(ExecutionTest.class);

	@Test
	public void execute() throws UnsupportedOperationSystemException {
		for (File jsonFile : new File("input").listFiles((path, name) -> name.toLowerCase().endsWith(".json"))) {
			try {
				JsonNode configurationNode = JsonLoader.fromFile(jsonFile);
	
				JsonNode schemaNode = JsonLoader.fromFile(new File("schema.json"));
				ProcessingReport validation = JsonSchemaFactory.byDefault().getValidator().validate(schemaNode, configurationNode);
				if(!validation.isSuccess()) {
					LOGGER.log(Level.WARN, "The project configuration is not valid: "+jsonFile.getAbsolutePath());
				}
				else {
					System.out.println(configurationNode.getNodeType());						
					
				}
			}
			catch(IOException | ProcessingException e) {
				LOGGER.log(Level.WARN, "Couldn't load project configuration from file: "+jsonFile.getAbsolutePath());
			}
		}
		Hashtable<String, String> mapping = new Hashtable<String, String>();
		mapping.put("225648e16028af05ab2be93553abef1126e6dbfe", "*");
		ProjectConfiguration config = new ProjectConfiguration("APIJSON", "TommyLemon",
				"https://github.com/TommyLemon/APIJSON.git", mapping);
		new MetricCalculation().calculateAll(Arrays.asList(config));
	}
}
