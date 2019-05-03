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

package com.scalified.axonframework.cdi.component;

import com.scalified.axonframework.cdi.api.Configurable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.reflect.TypeUtils;

import javax.enterprise.inject.spi.BeanManager;

/**
 * {@link Configurable} bean {@link Component} resolver
 *
 * @author shell
 * @since 2019-04-25
 */
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class ConfigurableComponentResolver {

	/**
	 * Resolver used to resolve bean
	 */
	private final ComponentResolver resolver;

	/**
	 * {@link ConfigurableComponentResolver} factory method
	 *
	 * @param component underlying {@code component}
	 * @return {@link ConfigurableComponentResolver} instance
	 */
	public static ConfigurableComponentResolver of(Component component) {
		return new ConfigurableComponentResolver(ComponentResolver.of(component));
	}

	/**
	 * Resolves bean using the underlying {@code resolver} and
	 * the given current {@code beanManager}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @param <T>         bean type
	 * @return resolved bean
	 */
	public <T> Configurable<T> resolve(BeanManager beanManager) {
		Object bean = resolver.resolve(beanManager);
		return isConfigurable() ? (Configurable<T>) bean : conf -> (T) bean;
	}

	/**
	 * Returns {@code true} if the underlying {@code resolver} component
	 * type is assignable from {@link Configurable} type, {@code false} otherwise
	 *
	 * @return {@code true} if the underlying {@code resolver} component
	 * type is assignable from {@link Configurable} type, {@code false} otherwise
	 */
	private boolean isConfigurable() {
		return TypeUtils.isAssignable(resolver.getComponent().getType(), Configurable.class);
	}

}
