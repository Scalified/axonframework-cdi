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

import com.scalified.axonframework.cdi.commons.CdiUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.enterprise.inject.spi.BeanManager;
import java.util.Optional;

/**
 * Bean {@link Component} resolver
 *
 * @author shell
 * @since 2019-04-25
 */
@Getter
@SuppressWarnings("unchecked")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ComponentResolver {

	/**
	 * Component used to resolve bean
	 */
	private final Component component;

	/**
	 * {@link ComponentResolver} factory method
	 *
	 * @param component underlying {@code component}
	 * @return {@link ComponentResolver} instance
	 */
	public static ComponentResolver of(Component component) {
		return new ComponentResolver(component);
	}

	/**
	 * Resolves the bean using the underlying {@code component} and
	 * the given current {@code beanManager}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @param <T>         bean type
	 * @return resolved bean
	 */
	public <T> T resolve(BeanManager beanManager) {
		return (T) Optional.ofNullable(component.getProducer())
				.map(producer -> CdiUtils.produce(beanManager, producer))
				.orElseGet(() -> CdiUtils.getReference(beanManager, component.getType()));
	}

}
