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

package com.scalified.axonframework.cdi.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.axonframework.config.Configuration;

/**
 * <b>Axon</b> event, which is fired once <b>Axon</b>
 * {@link Configuration} is built
 *
 * @author shell
 * @since 2019-05-07
 */
@Getter
@RequiredArgsConstructor
public class AxonConfiguredEvent implements AxonEvent {

	/**
	 * <b>Axon</b> configuration
	 */
	private final Configuration configuration;

}
