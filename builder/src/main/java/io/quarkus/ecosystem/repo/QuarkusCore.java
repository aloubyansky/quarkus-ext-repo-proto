package io.quarkus.ecosystem.repo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Quarkus Core version and relevant data
 */
public class QuarkusCore {

	private final String version;
	private final List<PlatformRelease> platforms = new ArrayList<>();
	private final List<ExtensionRelease> extensions = new ArrayList<>();

	QuarkusCore(String version) {
		this.version = Objects.requireNonNull(version);
	}

	void addPlatform(PlatformRelease platform) {
		platforms.add(platform);
	}

	void addExtension(ExtensionRelease extension) {
		extensions.add(extension);
	}

	public String getVersion() {
		return version;
	}

	public Collection<PlatformRelease> getPlatforms() {
		return platforms;
	}

	public Collection<ExtensionRelease> getExtensions() {
		return extensions;
	}

	public String toString() {
		return version;
	}
}
