package io.quarkus.ecosystem.repo.spec;

import java.util.Collections;
import java.util.List;

/**
 * Platform info from the spec file
 */
public class PlatformSpec {

	private String groupId;
	private String artifactId;
	private List<String> versions = Collections.emptyList();

	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public String getArtifactId() {
		return artifactId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	public List<String> getVersions() {
		return versions;
	}
	public void setVersions(List<String> versions) {
		this.versions = versions;
	}
}
