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

package com.scalified.axonframework.cdi.api;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.axonframework.serialization.Serializer;

/**
 * <b>Axon</b> {@link Serializer} type qualifiers
 *
 * @author shell
 * @since 2019-04-22
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SerializerType {

	/**
	 * Default <b>Axon</b> {@link Serializer} qualifier value
	 */
	public static final String DEFAULT = "defaultSerializer";

	/**
	 * <b>Axon</b> {@link Serializer} qualifier value for messages
	 */
	public static final String MESSAGE = "messageSerializer";

	/**
	 * <b>Axon</b> {@link Serializer} qualifier value for events
	 */
	public static final String EVENT = "eventSerializer";

}
