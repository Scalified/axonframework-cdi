/*
 * Copyright 2019 Scalified
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scalified.axonframework.cdi.configuration;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.axonframework.config.Configuration;
import org.axonframework.config.ModuleConfiguration;

import java.lang.reflect.Type;
import java.util.function.Supplier;

import static java.util.Objects.isNull;

/**
 * Lazily retrieved {@link ModuleConfiguration}
 *
 * @author shell
 * @since 2019-04-21
 */
@RequiredArgsConstructor
public class LazyRetrievedModuleConfiguration implements ModuleConfiguration {

	/**
	 * Underlying {@code moduleConfiguration}
	 */
	private ModuleConfiguration moduleConfiguration;

	/**
	 * Supplier to initialize {@code moduleConfiguration}
	 */
	private final Supplier<ModuleConfiguration> supplier;

	/**
	 * Module type
	 */
	private final Type moduleType;

	/**
	 * Initialize the module configuration using the given global {@code config}
	 *
	 * @param config the global configuration, providing access to generic components
	 */
	@Override
	public void initialize(Configuration config) {
		delegate().initialize(config);
	}

	/**
	 * Defines a phase in which this module's {@link #initialize(Configuration)},
	 * {@link #start()}, {@link #shutdown()} will be invoked
	 *
	 * @return this module's phase
	 */
	@Override
	public int phase() {
		return delegate().phase();
	}

	/**
	 * Invoked when the Configuration is started
	 *
	 * @see Configuration#start()
	 */
	@Override
	public void start() {
		delegate().start();
	}

	/**
	 * Invoked prior to shutdown of the application
	 *
	 * @see Configuration#shutdown()
	 */
	@Override
	public void shutdown() {
		delegate().shutdown();
	}

	/**
	 * Returns the actual module configuration instance. Usually, it is the
	 * instance itself. However, in case of module configuration wrappers,
	 * we would like to provide the wrapped module configuration as the instance
	 *
	 * @return the actual module configuration instance
	 */
	@Override
	public ModuleConfiguration unwrap() {
		return delegate();
	}

	/**
	 * Checks whether this Module Configuration is of the given {@code type}
	 *
	 * @param type a {@link Class} type to check the Module Configuration against
	 * @return whether Module Configuration is of given {@code type}
	 */
	@Override
	public boolean isType(Class<?> type) {
		return TypeUtils.isAssignable(type, moduleType);
	}

	/**
	 * Returns the underlying {@code moduleConfiguration}
	 *
	 * <p>
	 * Initialized underlying {@code moduleConfiguration} if it is not yet initialized
	 *
	 * @return underlying {@code moduleConfiguration}
	 */
	private ModuleConfiguration delegate() {
		if (isNull(moduleConfiguration)) {
			moduleConfiguration = supplier.get();
		}
		return moduleConfiguration;
	}

}
