package io.quarkus.ecosystem.repo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.quarkus.bootstrap.model.AppArtifactKey;

/**
 * Brief platform summary
 */
public class PlatformSummary {

	private final AppArtifactKey id;
	private final Map<String, PlatformRelease> releases = new HashMap<>();
	private final Set<String> quarkusCores = new HashSet<>();

	PlatformSummary(AppArtifactKey id) {
		this.id = Objects.requireNonNull(id);
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

	public Collection<String> getQuarkusCores() {
		return quarkusCores;
	}

	PlatformRelease getOrCreateRelease(String version, QuarkusCore quarkusCore) {
		PlatformRelease release = releases.get(version);
		if(release == null) {
			release = new PlatformRelease(this, version, quarkusCore);
			releases.put(version, release);
			quarkusCores.add(quarkusCore.getVersion());
		}
		return release;
	}
}
