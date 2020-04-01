package io.quarkus.ecosystem.repo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Specific extension release
 */
public class ExtensionRelease {

	private final ExtensionSummary summary;
	private final String version;
	private final Set<String> quarkusCores = new HashSet<>();
	private final List<PlatformRelease> platforms = new ArrayList<>(2);

	ExtensionRelease(ExtensionSummary summary, String version, QuarkusCore quarkusCore) {
		this.summary = Objects.requireNonNull(summary);
		this.version = Objects.requireNonNull(version);
		compatibleWith(quarkusCore);
	}

	void compatibleWith(QuarkusCore quarkusCore) {
		if(quarkusCores.add(Objects.requireNonNull(quarkusCore).getVersion())) {
			quarkusCore.addExtension(this);
		}
	}

	void addPlatform(PlatformRelease platform) {
		platforms.add(platform);
	}

	public ExtensionSummary getSummary() {
		return summary;
	}

	public String getVersion() {
		return version;
	}

	public Collection<PlatformRelease> getPlatforms() {
		return platforms;
	}

	public Collection<String> getQuarkusCores() {
		return quarkusCores;
	}
}
