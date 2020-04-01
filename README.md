# Extension Repository-related Utilities

This project is an attempt to prototype a few utilities for the extension repositories.

At this point, there is a simple extension repository specification file in YAML format (an example can be found in `playground\quarkus-extensions-repo.yaml`)
that lists Maven coordinates of the existing platform BOMs as well as Maven coordinates of extensions that aren't appearing in any platform.
Theoretically, this kind of file could be exposed to the users and be defining the content of an extension repository (e.g. Quarkus community extension repository).

There is a repository builder (implemented in the `builder` module) that can parse the spec yaml file and build the corresponding object model that
allows to perform all sorts of queries accross the repository, such as:

* which Quarkus Core versions are supported in the repository;
* which platforms are available;
* which extensions are available.

For a given Quarkus Core version:

* list the available platforms;
* list all the extensions including those that aren't a part of any platform.

For a given platform:

* list the platform versions available (i.e. releases of this platform);
* list Quarkus Core versions it supports.

For a given platform release:

* Quarkus Core version;
* Extension releases that are included.

For a given extension:

* list available versions of the extension (i.e. releases of the extension);
* (The list could be easily extended)

For a given extension release:

* Quarkus Core versions it was found to be compatible with;
* Platforms it is appearing in.

The builder may take time to process the spec file and initialize the in-memory representation of the repo. However, once built, the repo
could be persisted in another form that is more optimal to initialize from, if necessary. The idea to have the simplest possible format
exposed to the users and admins to define the extension repository.

Also, just in case, the repo could also be initialized in a static-init method.

## To give it a try

1. `mvn install` This will install the `builder` and the `io.quarkus:quarkus-ecosystem-maven-plugin` into the local Maven repo
2. `cd playground`
3. `python install-non-platform-extensions.py` This will install a few dummy extensions in the local Maven repo that aren't appearing in any platform
   (It creates a `tmp` dir and calls `io.quarkus:quarkus-maven-plugin:1.3.1.Final:create-extension` for the individual extensions found in `quarkus-extensions-repo.yaml`)
4. Look into `quarkus-extensions-repo.yaml` and adjust it, if you like (NOTE: the extensions will be created with Quarkus Core 1.3.1.Final release)
5. `mvn io.quarkus:quarkus-ecosystem-maven-plugin:999-SNAPSHOT:generate-extensions-repo`
  * This will parse the yaml file;
  * Unfortunately it will be resolving a lot of Maven artifacts to get the extension descriptors and the Quarkus version info
    (this step can be significantly optimized if we add a few things into our extension descriptors). Once it has downloaded everything
    subsequent runs will be faster.
  * Build the in-memory repo object model;
  * dump the repo summary on the screen.
