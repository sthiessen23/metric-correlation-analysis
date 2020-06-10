package metric.correlation.analysis.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MongoDBHelperTest {
	private static final String TEST_COLLECTION = "metrics";
	private static final String TEST_DB = "test";
	private static final String TEST_DATA_FILE = "resources/test_results.txt";

	private List<Map<String, String>> metrics = new LinkedList<>();
	private MongoDBHelper helper;

	@Before
	public void cleanCollection() {
		helper = new MongoDBHelper(TEST_DB, TEST_COLLECTION);
		helper.cleanCollection();
		importData();
	}

	@After
	public void close() {
		helper.close();
	}

	@Test
	public void createData() {
		assertTrue(helper.getMetrics(new HashMap<>()).isEmpty());
		helper.storeMetrics(metrics);
		assertEquals(7, helper.getMetrics(new HashMap<>()).size());
	}

	@Test
	public void addMetric() {
		helper.storeMetrics(metrics);
		Map<String, String> metricsTmp = new HashMap<>();
		metricsTmp.put("vendor", "vipshop");
		metricsTmp.put("product", "vjtools");
		metricsTmp.put("version", "v.1.0.8");
		metricsTmp.put("lloc", "100");
		helper.storeMetrics(metricsTmp);
		metricsTmp.remove("lloc");
		Map<String, Object> filter = new HashMap<>(metricsTmp);
		Map<String, String> metrics = helper.getMetrics(filter).get(0);
		assertEquals("100", metrics.get("lloc"));
		assertEquals("49.58", metrics.get("LOCpC"));
	}

	/**
	 * reads the metrics data into a map
	 */
	private void importData() {
		Map<Integer, String> keyIndices = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(TEST_DATA_FILE))) {
			String[] metricKeys = br.readLine().split(",");
			for (int i = 0; i < metricKeys.length; i++) {
				keyIndices.put(i, metricKeys[i]);
			}
			String line;
			while ((line = br.readLine()) != null) {
				HashMap<String, String> nextMetrics = new HashMap<>();
				String[] values = line.split(",");
				for (int i = 0; i < values.length; i++) {
					nextMetrics.put(keyIndices.get(i), values[i]);
				}
				metrics.add(nextMetrics);
			}
		} catch (IOException e) {
			fail("could not read data file");
		}
	}
}
