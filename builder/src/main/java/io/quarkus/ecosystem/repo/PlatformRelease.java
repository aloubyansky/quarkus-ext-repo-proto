package io.quarkus.ecosystem.repo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Specific platform release
 */
public class PlatformRelease {

	private final PlatformSummary summary;
	private final String version;
	private final List<ExtensionRelease> extensions = new ArrayList<>();
	private final QuarkusCore quarkusCore;

	PlatformRelease(PlatformSummary summary, String version, QuarkusCore quarkusCore) {
		this.summary = Objects.requireNonNull(summary);
		this.version = Objects.requireNonNull(version);
		this.quarkusCore = Objects.requireNonNull(quarkusCore);
		quarkusCore.addPlatform(this);
	}

	void addExtension(ExtensionRelease extension) {
		extensions.add(extension);
	}

	public PlatformSummary getSummary() {
		return summary;
	}

	public String getVersion() {
		return version;
	}

	public QuarkusCore getQuarkusCore() {
		return quarkusCore;
	}

	public Collection<ExtensionRelease> getExtensions() {
		return extensions;
	}
}
