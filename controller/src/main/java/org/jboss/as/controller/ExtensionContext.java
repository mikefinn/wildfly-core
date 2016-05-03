/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller;


import org.jboss.as.controller.services.path.PathManager;

/**
 * The context for registering a new extension.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author David Bosschaert
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ExtensionContext {

    /**
     * Convenience variant of {@link #registerSubsystem(String, int, int, int)} that uses {@code 0}
     * as the {@code microVersion}.
     *
     * @param name the name of the subsystem
     * @param version the version of the subsystem's management interface.
     *
     * @return the {@link SubsystemRegistration}
     *
     * @throws IllegalStateException if the subsystem name has already been registered
     */
    SubsystemRegistration registerSubsystem(String name, ModelVersion version);

    /**
     * Register a new subsystem type.  The returned registration object should be used
     * to configure XML parsers, operation handlers, and other subsystem-specific constructs
     * for the new subsystem.  If the subsystem registration is deemed invalid by the time the
     * extension registration is complete, the subsystem registration will be ignored, and an
     * error message will be logged.
     * <p>
     * The new subsystem registration <em>must</em> register a handler and description for the
     * {@code add} operation at its root address.  The new subsystem registration <em>must</em> register a
     * {@code remove} operation at its root address.
     *
     * @param name the name of the subsystem
     * @param version the version of the subsystem's management interface.
     * @param deprecated mark this extension as deprecated
     *
     * @return the {@link SubsystemRegistration}
     *
     * @throws IllegalStateException if the subsystem name has already been registered
     */
    SubsystemRegistration registerSubsystem(String name, ModelVersion version, boolean deprecated);

    /**
     * Convenience variant of {@link #registerSubsystem(String, int, int, int)} that uses {@code 0}
     * as the {@code microVersion}.
     *
     * @param name the name of the subsystem
     * @param majorVersion the major version of the subsystem's management interface
     * @param minorVersion the minor version of the subsystem's management interface
     *
     * @return the {@link SubsystemRegistration}
     *
     * @throws IllegalStateException if the subsystem name has already been registered
     * @deprecated {@see #registerSubsystem(String, ModelVersion)}
     */
    @Deprecated
    SubsystemRegistration registerSubsystem(String name, int majorVersion, int minorVersion);

    /**
     * Register a new subsystem type.  The returned registration object should be used
     * to configure XML parsers, operation handlers, and other subsystem-specific constructs
     * for the new subsystem.  If the subsystem registration is deemed invalid by the time the
     * extension registration is complete, the subsystem registration will be ignored, and an
     * error message will be logged.
     * <p>
     * The new subsystem registration <em>must</em> register a handler and description for the
     * {@code add} operation at its root address.  The new subsystem registration <em>must</em> register a
     * {@code remove} operation at its root address.
     *
     * @param name the name of the subsystem
     * @param majorVersion the major version of the subsystem's management interface
     * @param minorVersion the minor version of the subsystem's management interface
     * @param microVersion the micro version of the subsystem's management interface
     *
     * @return the {@link SubsystemRegistration}
     *
     * @throws IllegalStateException if the subsystem name has already been registered
     * @deprecated {@see #registerSubsystem(String, ModelVersion)}
     */
    @Deprecated
    SubsystemRegistration registerSubsystem(String name, int majorVersion, int minorVersion, int microVersion);

    /**
     * Register a new subsystem type.  The returned registration object should be used
     * to configure XML parsers, operation handlers, and other subsystem-specific constructs
     * for the new subsystem.  If the subsystem registration is deemed invalid by the time the
     * extension registration is complete, the subsystem registration will be ignored, and an
     * error message will be logged.
     * <p>
     * The new subsystem registration <em>must</em> register a handler and description for the
     * {@code add} operation at its root address.  The new subsystem registration <em>must</em> register a
     * {@code remove} operation at its root address.
     *
     * @param name the name of the subsystem
     * @param majorVersion the major version of the subsystem's management interface
     * @param minorVersion the minor version of the subsystem's management interface
     * @param microVersion the micro version of the subsystem's management interface
     * @param deprecated mark this extension as deprecated
     *
     * @return the {@link SubsystemRegistration}
     *
     * @throws IllegalStateException if the subsystem name has already been registered
     * @deprecated {@see #registerSubsystem(String, ModelVersion, boolean)}
     */
    @Deprecated
    SubsystemRegistration registerSubsystem(String name, int majorVersion, int minorVersion, int microVersion, boolean deprecated);

    /**
     * Gets the type of the current process.
     * @return the current process type. Will not be {@code null}
     */
    ProcessType getProcessType();

    /**
     * Gets the current running mode of the process.
     * @return the current running mode. Will not be {@code null}
     */
    RunningMode getRunningMode();

    /**
     * Gets whether it is valid for the extension to register resources, attributes or operations that do not
     * involve the persistent configuration, but rather only involve runtime services. Extensions should use this
     * method before registering such "runtime only" resources, attributes or operations. This
     * method is intended to avoid registering resources, attributes or operations on process types that
     * can not install runtime services.
     *
     * @return whether it is valid to register runtime resources, attributes, or operations.
     */
    boolean isRuntimeOnlyRegistrationValid();

    /**
     * Gets the process' {@link PathManager} if the process is a {@link ProcessType#isServer() server}; throws
     * an {@link IllegalStateException} if not.
     *
     * @return the path manager. Will not return {@code null}
     *
     * @throws IllegalStateException if the process is not a {@link ProcessType#isServer() server}
     */
    PathManager getPathManager();

    /**
     * Returns true if subsystems should register transformers. This is true if {@link #getProcessType()} equals {@link ProcessType#HOST_CONTROLLER} and the
     * process controller is the master domain controller.
     *
     * @return {@code true} if transformers should be registered
     * @deprecated Experimental, the way transformers are registered may change to be the same as we do for parsers
     */
    @Deprecated
    boolean isRegisterTransformers();
}
