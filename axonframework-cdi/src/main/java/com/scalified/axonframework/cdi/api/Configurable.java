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

import org.axonframework.config.Configuration;

import java.util.function.Function;

/**
 * {@link Function} for providing <b>Axon</b> component using the
 * given {@link Configuration}
 *
 * @param <T> <b>Axon</b> component type
 * @author shell
 * @since 2019-04-19
 */
@FunctionalInterface
public interface Configurable<T> extends Function<Configuration, T> {
}
