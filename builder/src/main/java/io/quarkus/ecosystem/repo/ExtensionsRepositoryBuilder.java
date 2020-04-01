package io.quarkus.ecosystem.repo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.DeploymentInjectingDependencyVisitor;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.ecosystem.repo.spec.ExtensionSpec;
import io.quarkus.ecosystem.repo.spec.ExtensionsRepoSpec;
import io.quarkus.ecosystem.repo.spec.PlatformSpec;

/**
 * Parses a repository spec file and builds the corresponding object model.
 */
public class ExtensionsRepositoryBuilder {

    private static final String IO_QUARKUS = "io.quarkus";
	private static final String QUARKUS_CORE = "quarkus-core";
	private static final String QUARKUS_CORE_DEPLOYMENT = "quarkus-core-deployment";

	public static ExtensionsRepositoryBuilder getInstance() throws ExtensionsRepositoryException {
    	try {
			return getInstance(MavenArtifactResolver.builder().build());
		} catch (AppModelResolverException e) {
			throw new ExtensionsRepositoryException("Failed to initialize extensions repository builder", e);
		}
    }

    public static ExtensionsRepositoryBuilder getInstance(MavenArtifactResolver mvn) {
    	return new ExtensionsRepositoryBuilder(mvn);
    }

    private final MavenArtifactResolver mvn;

    private ExtensionsRepositoryBuilder(MavenArtifactResolver mvn) {
    	this.mvn = mvn;
    }

    public ExtensionsRepository build(Path repoSpecYaml) throws ExtensionsRepositoryException {
		if(!Files.exists(repoSpecYaml)) {
			throw new ExtensionsRepositoryException("Failed to locate Quarkus extensions repository spec file " + repoSpecYaml);
		}
		debug("Generating Quarkus extension repository from %s", repoSpecYaml);

		final ExtensionsRepository repo = new ExtensionsRepository();

		ExtensionsRepoSpec repoSpec;
		try(Reader reader = Files.newBufferedReader(repoSpecYaml)) {
		    repoSpec = initYaml().loadAs(reader, ExtensionsRepoSpec.class);
		} catch (IOException e) {
			throw new ExtensionsRepositoryException("Failed to parse " + repoSpecYaml, e);
		}

		for(PlatformSpec platform : repoSpec.getPlatforms()) {
		    for(String version : platform.getVersions()) {
		    	processPlatformSpec(repo, platform, version);
		    }
		}

		for(ExtensionSpec extension : repoSpec.getIndividualExtensions()) {
		    for(String version : extension.getVersions()) {
		    	processIndividualExtensionSpec(repo, extension, version);
		    }
		}

		return repo;
    }

	private void processIndividualExtensionSpec(ExtensionsRepository repo, ExtensionSpec spec, String version) throws ExtensionsRepositoryException {
		final Artifact artifact = new DefaultArtifact(spec.getGroupId(), spec.getArtifactId(), "jar", version);
		debug("Processing individual extension %s", artifact);
		final String quarkusCoreVersion;
		try {
			quarkusCoreVersion = resolveQuarkusCoreVersion(artifact);
		} catch (AppModelResolverException e) {
			throw new ExtensionsRepositoryException("Failed to resolve artifact descriptor for " + artifact, e);
		}
		if(quarkusCoreVersion == null) {
			throw new ExtensionsRepositoryException(artifact + " specified as an individual extension does not appear to be a Quarkus extension");
		}
		processExtension(repo.getOrCreateExtension(getId(artifact)), artifact, repo.getOrCreateQuarkusCore(quarkusCoreVersion));
	}

	private void processPlatformSpec(ExtensionsRepository repo, PlatformSpec spec, String version) throws ExtensionsRepositoryException {
		debug("Processing platform %s:%s:%s", spec.getGroupId(), spec.getArtifactId(), version);
		final ArtifactDescriptorResult platformArtifactDescr = resolveDescriptor(spec.getGroupId(), spec.getArtifactId(), version);
		final String platformQuarkusCoreVersion = findQuarkusCoreVersion(platformArtifactDescr.getManagedDependencies(), QUARKUS_CORE);
		if(platformQuarkusCoreVersion == null) {
			throw new ExtensionsRepositoryException("Failed to locate " + IO_QUARKUS + ":" + QUARKUS_CORE
					+ " among the managed dependencies of " + spec.getGroupId() + ":" + spec.getArtifactId());
		}
		debug("  - Quarkus Core: %s", platformQuarkusCoreVersion);

		final QuarkusCore platformQuarkusCore = repo.getOrCreateQuarkusCore(platformQuarkusCoreVersion);
		final PlatformSummary platformSummary = repo.getOrCreatePlatform(getId(platformArtifactDescr.getArtifact()));
		final PlatformRelease platformRelease = platformSummary.getOrCreateRelease(version, platformQuarkusCore);

        for (Dependency dep : platformArtifactDescr.getManagedDependencies()) {
            final Artifact artifact = dep.getArtifact();
            if (!artifact.getExtension().equals("jar")
                    || "javadoc".equals(artifact.getClassifier())
                    || "tests".equals(artifact.getClassifier())
                    || "sources".equals(artifact.getClassifier())) {
                continue;
            }
            try {
            	final String extQuarkusCoreVersion = resolveQuarkusCoreVersion(artifact);
                if (extQuarkusCoreVersion != null) {
            		final ExtensionSummary extSummary = repo.getOrCreateExtension(getId(artifact));
					final ExtensionRelease extRelease = processExtension(extSummary, artifact, repo.getOrCreateQuarkusCore(extQuarkusCoreVersion));
                	extRelease.compatibleWith(platformQuarkusCore);
                	platformRelease.addExtension(extRelease);
            		extRelease.addPlatform(platformRelease);
                }
            } catch (AppModelResolverException e) {
                // there are some parent poms that appear as jars for some reason
                debug("Failed to resolve dependency %s defined in %s", artifact, platformArtifactDescr.getArtifact());
			}
        }
	}

	private ExtensionRelease processExtension(ExtensionSummary extSummary, Artifact runtimeArtifact,
			QuarkusCore extensionQuarkusCore)
			throws ExtensionsRepositoryException {
		debug("  Extension release %s", runtimeArtifact);
		return extSummary.getOrCreateRelease(runtimeArtifact.getVersion(), extensionQuarkusCore);
	}

	private static AppArtifactKey getId(Artifact artifact) {
		return new AppArtifactKey(artifact.getGroupId(), artifact.getArtifactId());
	}

	private String resolveQuarkusCoreVersion(Artifact runtimeArtifact)
			throws ExtensionsRepositoryException, AppModelResolverException {
		final ArtifactResult resolved = mvn.resolve(runtimeArtifact);
		final Artifact deploymentArtifact = getDeploymentArtifact(resolved.getArtifact());
		if (deploymentArtifact == null) {
			return null;
		}
		final CollectResult collectResult = mvn.collectDependencies(deploymentArtifact, Collections.emptyList());
		final String[] version = new String[1];
        final TreeDependencyVisitor visitor = new TreeDependencyVisitor(new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                final Dependency dep = node.getDependency();
                if (dep != null
                		&& dep.getArtifact().getArtifactId().equals(QUARKUS_CORE_DEPLOYMENT)
                		&& dep.getArtifact().getGroupId().equals(IO_QUARKUS)) {
                	version[0] = dep.getArtifact().getVersion();
                    return false;
                }
                return true;
            }
        });
        collectResult.getRoot().accept(visitor);
        if(version[0] == null) {
			throw new ExtensionsRepositoryException("Extension " + deploymentArtifact
			+ " does not appear to depend on " + IO_QUARKUS + ":" + QUARKUS_CORE_DEPLOYMENT);
        }
        return version[0];
	}

    private Artifact getDeploymentArtifact(Artifact artifact) throws ExtensionsRepositoryException {
        final Path path = artifact.getFile().toPath();
        if (Files.isDirectory(path)) {
         	return readDeploymentArtifactCoords(path.resolve(BootstrapConstants.DESCRIPTOR_PATH));
        }
		try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
			return readDeploymentArtifactCoords(artifactFs.getPath(BootstrapConstants.DESCRIPTOR_PATH));
		} catch (IOException e) {
			throw new ExtensionsRepositoryException("Failed to open " + path);
		}
    }

    private Artifact readDeploymentArtifactCoords(Path path) throws ExtensionsRepositoryException {
    	if(!Files.exists(path)) {
    		return null;
    	}
        final Properties rtProps = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            rtProps.load(reader);
        } catch (IOException e) {
            throw new ExtensionsRepositoryException("Failed to load " + path, e);
        }
        final String value = rtProps.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        if(value == null) {
        	throw new ExtensionsRepositoryException(path + " is missing " + BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        }
        return DeploymentInjectingDependencyVisitor.toArtifact(value);
    }

	private static String findQuarkusCoreVersion(List<Dependency> deps, String artifactId) {
		for(Dependency dep : deps) {
			if (dep.getArtifact().getArtifactId().equals(artifactId)
					&& dep.getArtifact().getGroupId().equals(IO_QUARKUS)) {
				return dep.getArtifact().getVersion();
			}
		}
		return null;
	}

	private ArtifactDescriptorResult resolveDescriptor(String groupId, String artifactId, String version)
			throws ExtensionsRepositoryException {
		final DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, "jar", version);
		try {
			return mvn.resolveDescriptor(artifact);
		} catch (AppModelResolverException e) {
			throw new ExtensionsRepositoryException("Failed to resolve artifact descriptor for " + artifact, e);
		}
	}

	private static Yaml initYaml() {
		final Constructor repoSpecCtor = new Constructor(ExtensionsRepoSpec.class);
		repoSpecCtor.setPropertyUtils(new PropertyUtils() {
			@Override
			public Property getProperty(Class<? extends Object> type, String name) {
				StringBuilder buf = null;
				boolean sawDash = false;
				for (int i = 0; i < name.length(); ++i) {
					final char c = name.charAt(i);
					if (c == '-') {
						if (buf == null) {
							buf = new StringBuilder();
							buf.append(name.subSequence(0, i));
						}
						sawDash = true;
						continue;
					}
					if(buf == null) {
						continue;
					}
					if (sawDash) {
						sawDash = false;
						buf.append(Character.toUpperCase(c));
					} else {
						buf.append(c);
					}
				}
				return super.getProperty(type, buf == null ? name : buf.toString());
			}
		});
		return new Yaml(repoSpecCtor);
	}

	private static void debug(String msg, Object... args) {
	    if(args.length == 0) {
	    	System.out.println(msg);
	    	return;
	    }
	    System.out.println(String.format(msg, args));
	}
}
