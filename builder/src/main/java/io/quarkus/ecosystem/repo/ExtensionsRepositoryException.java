package io.quarkus.ecosystem.repo;

public class ExtensionsRepositoryException extends Exception {

	private static final long serialVersionUID = 1L;

	public ExtensionsRepositoryException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExtensionsRepositoryException(String message) {
		super(message);
	}
}
