package metric.correlation.analysis.database;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.gravity.eclipse.os.Execute;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoSocketReadException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

/**
 * contains helper methods for dealing with the mongoDB and metrics
 * 
 * @author Stefan Thießen
 *
 */
public class MongoDBHelper {

	private static final Logger LOGGER = Logger.getLogger(MongoDBHelper.class);

	public static final int DEFAULT_PORT = 27017;
	public static final String SERVER_URI = "mongodb://localhost:" + DEFAULT_PORT;
	public static final String DEFAULT_DATABASE = "metric_correlation";
	public static final String DEFAULT_COLLECTION = "metrics";
	public static final String MONGOD = "MONGOD";
	// keys to identify a projects metric results
	private List<String> projectKeys = Arrays.asList("version", "product", "vendor");

	private MongoCollection<Document> dbCollection;
	private MongoClient client;
	private String databaseName;
	private String collectionName;
	static Process mongodProcess;

	public MongoDBHelper() {
		this(DEFAULT_DATABASE, DEFAULT_COLLECTION);
	}

	public MongoDBHelper(String databaseName, String collectionName) {
		this.databaseName = databaseName;
		this.collectionName = collectionName;
		client = new MongoClient(new MongoClientURI(SERVER_URI));
		dbCollection = client.getDatabase(databaseName).getCollection(collectionName);
	}

	/**
	 * starts the server using the .cfg file in the mongodb folder, if present
	 */
	public static void startServer() {
		if (available(DEFAULT_PORT)) {
			String config = findConfig();
			List<String> args = config == null ? null : Arrays.asList("--config", config);
			try {
				mongodProcess = Execute.run(getMongoDBFolder(), "mongod", args, null);
				Thread.sleep(5000); // give the server time to start
				if (available(DEFAULT_PORT)) {
					String msg = Execute.collectMessages(mongodProcess).toString();
					LOGGER.warn(msg);
					throw new IllegalStateException();
				}
			} catch (Exception e) {
				LOGGER.log(Level.ERROR, "COULD NOT START THE MONGODB SERVER");
				LOGGER.error(e.getStackTrace());
			}
		}
	}

	/**
	 * store the (key, value) pairs as metrics in the database
	 * 
	 * @param metrics keys and values of the metrics
	 */
	public void storeMetrics(Map<String, String> metrics) {
		Document filter = new Document();
		projectKeys.stream().forEach(key -> filter.append(key, metrics.get(key)));
		Document doc = new Document();
		metrics.entrySet().stream().forEach(entry -> doc.append(entry.getKey(), entry.getValue()));
		Document updateDoc = new Document();
		updateDoc.put("$set", doc);
		UpdateOptions options = new UpdateOptions();
		options.upsert(true);
		dbCollection.updateOne(filter, updateDoc, options);
	}

	public void storeMetrics(List<Map<String, String>> metricsList) {
		for (Map<String, String> metrics : metricsList) {
			storeMetrics(metrics);
		}
	}

	/**
	 * get all metrics from projects that fit the given
	 * 
	 * @param filterMap filter values (e.g. version and product), can be more
	 *                  complicated like lloc > 100,000
	 * @return list of all metrics from projects that matched the filter
	 */
	public List<Map<String, String>> getMetrics(Map<String, Object> filterMap) {
		List<Map<String, String>> results = new LinkedList<>();
		Document filter = new Document(filterMap);
		for (Document d : dbCollection.find(filter)) {
			Map<String, String> next = new HashMap<>();
			d.entrySet().stream().filter(entry -> !(entry.getKey().equals("_id")))
					.forEach(entry -> next.put(entry.getKey(), (String) entry.getValue()));
			results.add(next);
		}
		return results;
	}

	public void cleanCollection() {
		dbCollection.drop();
		client.getDatabase(databaseName).createCollection(collectionName);
		dbCollection = client.getDatabase(databaseName).getCollection(collectionName);
	}

	/**
	 * close the db connection
	 */
	public void close() {
		client.close();
	}

	/**
	 * shuts down the database, you should close all clients before calling this
	 */
	public static void shutdownDatabase() {
		Document shutdownDoc = new Document();
		shutdownDoc.append("shutdown", 1);
		try (MongoClient shutdownClient = new MongoClient(new MongoClientURI(SERVER_URI))){
			shutdownClient.getDatabase("admin").runCommand(shutdownDoc);
		} catch (MongoSocketReadException e) { // the shutdown throws this exception when successful
			LOGGER.info("shutdown mongodb server");
		}

	}

	/**
	 * if the config file is not set as parameter, mondodb will choose default
	 * values and ignore .cfg files in the folder
	 * 
	 * @return absolute location of the config file, if present
	 */
	private static String findConfig() {
		File mongodParent = getMongoDBFolder();
		Optional<File> configOpt = Arrays.stream(mongodParent.listFiles())
				.filter(f -> FilenameUtils.getExtension(f.getName()).equals("cfg")).findFirst();
		if (configOpt.isPresent()) {
			return configOpt.get().getName();
		} else {
			return null;
		}
	}

	/**
	 * 
	 * @return absolute path of mongodb folder
	 */
	private static File getMongoDBFolder() {
		return new File(System.getenv(MONGOD)).getParentFile();
	}

	/**
	 * checks if the port is available
	 * 
	 * @param port
	 * @return
	 */
	private static boolean available(int port) {
		try (Socket ignored = new Socket("localhost", port)) {
			return false;
		} catch (IOException ignored) {
			return true;
		}
	}
}
