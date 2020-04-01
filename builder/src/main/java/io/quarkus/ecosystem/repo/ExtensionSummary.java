package io.quarkus.ecosystem.repo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.quarkus.bootstrap.model.AppArtifactKey;

/**
 * Brief extension summary
 */
public class ExtensionSummary {

	private final AppArtifactKey id;
	private final Map<String, ExtensionRelease> releases = new HashMap<>();

	ExtensionSummary(String groupId, String artifactId) {
		id = new AppArtifactKey(Objects.requireNonNull(groupId), Objects.requireNonNull(artifactId));
	}

	ExtensionSummary(AppArtifactKey id) {
		this.id = Objects.requireNonNull(id);
	}

	ExtensionRelease getOrCreateRelease(String version, QuarkusCore quarkusCore) {
		ExtensionRelease release = releases.get(version);
		if(release == null) {
			release = new ExtensionRelease(this, version, quarkusCore);
			releases.put(version, release);
		} else {
			release.compatibleWith(quarkusCore);
		}
		return release;
	}

	public AppArtifactKey getId() {
		return id;
	}

	public String getGroupId() {
		return id.getGroupId();
	}

	public String getArtifactId() {
		return id.getArtifactId();
	}

	public Collection<ExtensionRelease> getReleases() {
		return releases.values();
	}
}
