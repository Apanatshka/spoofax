package org.metaborg.core.config;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;

/**
 * Stores and retrieves language component configurations.
 */
public interface IProjectConfigService {
    /**
     * Checks if a configuration exists for the project at the given location.
     *
     * @param rootFolder
     *            The project root folder.
     * @return True if a configuration exists, false otherwise.
     */
    boolean available(FileObject rootFolder);

    /**
     * Gets the configuration for the project at the given location.
     *
     * @param rootFolder
     *            The project root folder.
     * @return The configuration, or <code>null</code> when no configuration could be retrieved.
     * @throws ConfigException
     *             When reading the configuration fails.
     */
    @Nullable IProjectConfig get(FileObject rootFolder) throws ConfigException;
}
