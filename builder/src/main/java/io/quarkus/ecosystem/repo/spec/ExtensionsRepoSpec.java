package io.quarkus.ecosystem.repo.spec;

import java.util.Collections;
import java.util.List;

/**
 * Extension repository info from the spec file
 */
public class ExtensionsRepoSpec {

	private List<ExtensionSpec> individualExtensions = Collections.emptyList();
	private List<PlatformSpec> platforms = Collections.emptyList();

	public List<ExtensionSpec> getIndividualExtensions() {
		return individualExtensions;
	}
	public void setIndividualExtensions(List<ExtensionSpec> individualExtensions) {
		this.individualExtensions = individualExtensions;
	}
	public List<PlatformSpec> getPlatforms() {
		return platforms;
	}
	public void setPlatforms(List<PlatformSpec> platforms) {
		this.platforms = platforms;
	}
}
