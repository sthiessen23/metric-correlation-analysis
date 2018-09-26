package metric.correlation.analysis.configuration;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A data class describing a relevant project
 */
public class ProjectConfiguration {

	private final String productName;
	private final String vendorName;
	private final String gitUrl;
	private final Hashtable<String, String> versionGitCommitIdMapping;

	/**
	 * Constructs a new configuration
	 * 
	 * @param productName               The name of the product
	 * @param vendorName                The vendor of the product
	 * @param gitUrl                    The git url of the product
	 * @param versionGitCommitIdMapping A mapping from project versions to commit
	 *                                  ids
	 */
	public ProjectConfiguration(String productName, String vendorName, String gitUrl,
			Hashtable<String, String> versionGitCommitIdMapping) {
		this.productName = productName;
		this.vendorName = vendorName;
		this.gitUrl = gitUrl;
		this.versionGitCommitIdMapping = versionGitCommitIdMapping;
	}

	/**
	 * A getter for the name of the product
	 * 
	 * @return The product name
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * A getter for the name of the product vendor
	 * 
	 * @return The vendor name
	 */
	public String getVendorName() {
		return vendorName;
	}

	/**
	 * A getter for the url of the git repository
	 * 
	 * @return The git url
	 */
	public String getGitUrl() {
		return gitUrl;
	}

	/**
	 * Returns a collection of all relevant commit IDs for this product
	 * 
	 * @return The commit ids
	 */
	public Collection<String> getGitCommitIds() {
		return versionGitCommitIdMapping.values();
	}

	/**
	 * Returns a set of all relevant versions of this product
	 * 
	 * @return the product versions
	 */
	public Set<String> getProjectVersions() {
		return versionGitCommitIdMapping.keySet();
	}

	/**
	 * Returns the commit ID of a product version
	 * 
	 * @param version The product version
	 * @return The commit ID or null if this configuration contains not the
	 *         requested version
	 */
	public String getCommitId(String version) {
		return versionGitCommitIdMapping.get(version);
	}

	/**
	 * Returns all pairs of product version and git commit stored in this
	 * configuration as entries (@see java.util.Map.Entry) with the version as key
	 * and the commit ID as value.
	 * 
	 * @return A set of the pairs as entries @see java.util.Map.Entry
	 */
	public Set<Entry<String, String>> getVersionCommitIdPairs() {
		return versionGitCommitIdMapping.entrySet();
	}
}
