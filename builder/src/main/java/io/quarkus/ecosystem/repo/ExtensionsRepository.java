package io.quarkus.ecosystem.repo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.bootstrap.model.AppArtifactKey;

/**
 * Repository of extensions
 */
public class ExtensionsRepository {

	private final Map<AppArtifactKey, ExtensionSummary> extensions = new HashMap<>();
	private final Map<AppArtifactKey, PlatformSummary> platforms = new HashMap<>();
	private final Map<String, QuarkusCore> quarkusCores = new HashMap<>();

	ExtensionsRepository() {
	}

	QuarkusCore getOrCreateQuarkusCore(String version) {
		QuarkusCore quarkusCore = quarkusCores.get(version);
		if(quarkusCore == null) {
			quarkusCore = new QuarkusCore(version);
			quarkusCores.put(version, quarkusCore);
		}
		return quarkusCore;
	}

	ExtensionSummary getOrCreateExtension(AppArtifactKey id) {
		ExtensionSummary summary = extensions.get(id);
		if(summary == null) {
			summary = new ExtensionSummary(id);
			extensions.put(id, summary);
		}
		return summary;
	}

	PlatformSummary getOrCreatePlatform(AppArtifactKey id) {
		PlatformSummary summary = platforms.get(id);
		if(summary == null) {
			summary = new PlatformSummary(id);
			platforms.put(id, summary);
		}
		return summary;
	}

	public Collection<String> getQuarkusCoreVersions() {
		return quarkusCores.keySet();
	}

	public QuarkusCore getQuarkusCore(String version) {
		return quarkusCores.get(version);
	}

	public Collection<QuarkusCore> getQuarkusCores() {
		return quarkusCores.values();
	}

	public Collection<AppArtifactKey> getPlatformIds() {
		return platforms.keySet();
	}

	public PlatformSummary getPlatformSummary(String groupId, String artifactId) {
		return getPlatformSummary(new AppArtifactKey(groupId, artifactId));
	}

	public PlatformSummary getPlatformSummary(AppArtifactKey id) {
		return platforms.get(id);
	}

	public Collection<PlatformSummary> getPlatformSummaries() {
		return platforms.values();
	}

	public Collection<ExtensionSummary> getExtensionSummaries() {
		return extensions.values();
	}
}
