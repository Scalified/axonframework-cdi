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

import lombok.Getter;
import lombok.ToString;

import javax.enterprise.inject.spi.Producer;
import java.lang.reflect.Type;

/**
 * Component containing <b>CDI</b> elements for
 * resolving bean
 *
 * @author shell
 * @since 2019-04-21
 */
@Getter
@ToString
public class Component {

	/**
	 * Component {@code type}
	 */
	@ToString.Include
	private final Type type;

	/**
	 * Component {@code producer}
	 */
	@ToString.Exclude
	private Producer<?> producer;

	/**
	 * {@link Component} constructor
	 *
	 * @param type component type
	 */
	public Component(Type type) {
		this.type = type;
	}

	/**
	 * {@link Component} constructor
	 *
	 * @param type     component type
	 * @param producer component producer
	 */
	public Component(Type type, Producer<?> producer) {
		this.type = type;
		this.producer = producer;
	}

}
