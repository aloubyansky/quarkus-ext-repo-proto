package io.quarkus.ecosystem.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.ecosystem.repo.ExtensionRelease;
import io.quarkus.ecosystem.repo.ExtensionsRepository;
import io.quarkus.ecosystem.repo.ExtensionsRepositoryBuilder;
import io.quarkus.ecosystem.repo.ExtensionsRepositoryException;


/**
 * Parses an extensions repository spec yaml file, builds the corresponding object model
 * and dumps the summary.
 */
@Mojo(name = "generate-extensions-repo", requiresProject = false)
public class BasicMojo extends AbstractMojo {

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    @Parameter(property = "repo-yaml", defaultValue = "${basedir}/quarkus-extensions-repo.yaml")
    private File repoYaml;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
	    final ExtensionsRepository repo = buildExtensionsRepo();

	    logSummary(repo);
	}

    private void logSummary(ExtensionsRepository repo) {

    	log("");
		log("REPOSITORY SUMMARY");

		log("  QUARKUS CORE VERSIONS");
		repo.getQuarkusCores().forEach(v -> {
			log("    " + v);
			log("      Available platforms: ");
			v.getPlatforms().forEach(p -> {
				log("        " + p.getSummary().getId() + ":" + p.getVersion() + "; ");
			});
		});

		log("  PLATFORMS");
		repo.getPlatformSummaries().forEach(p -> {
			log("    " + p.getId());
			log("      Available for Quarkus Core:");
			p.getQuarkusCores().forEach(v -> log("        " + v));
		});

		log("  EXTENSIONS");
		repo.getExtensionSummaries().forEach(e -> {
			log("    " + e.getId());
			log("      Available versions:");
			e.getReleases().forEach(r -> {
				log("        " + r.getVersion());
				log("          Compatible with Quarkus Core:");
				r.getQuarkusCores().forEach(s -> log("            " + s));
				log("          Appears in platforms:");
				r.getPlatforms().forEach(p -> {
					log("            " + p.getSummary().getId() + ":" + p.getVersion());
				});
			});
		});

		log("  NON-PLATFORM EXTENSIONS");
		repo.getExtensionSummaries().forEach(s -> {
			boolean loggedSummary = false;
			for(ExtensionRelease r : s.getReleases()) {
				if(r.getPlatforms().isEmpty()) {
					if(!loggedSummary) {
						loggedSummary = true;
						log("    " + s.getId());
					}
					log("      " + r.getVersion());
					log("        Compatible with Quarkus Core:");
					r.getQuarkusCores().forEach(v -> log("          " + v));
				}
			}
		});
    }

    private void log(String line) {
    	System.out.println(line);
    }
	private ExtensionsRepository buildExtensionsRepo() throws MojoExecutionException {
		try {
			return ExtensionsRepositoryBuilder.getInstance(getMavenResolver()).build(repoYaml.toPath());
		} catch (ExtensionsRepositoryException e) {
			throw new MojoExecutionException("Failed to build Quarkus extensions repo from " + repoYaml, e);
		}
	}


	private MavenArtifactResolver getMavenResolver() throws MojoExecutionException {
		try {
			return MavenArtifactResolver.builder().setRepositorySystem(repoSystem)
					.setRepositorySystemSession(repoSession)
					.setRemoteRepositories(repos)
					.build();
		} catch (AppModelResolverException e) {
			throw new MojoExecutionException("Failed to initialize Maven artifact resolver", e);
		}
	}
}
