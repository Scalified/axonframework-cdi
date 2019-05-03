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

package com.scalified.axonframework.cdi.api.annotation;

import org.apache.commons.lang3.StringUtils;

import javax.enterprise.inject.Stereotype;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that annotated element is <b>Axon</b> component
 *
 * @author shell
 * @since 2019-04-12
 */
@Stereotype
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AxonComponent {

	/**
	 * Qualified annotated element value, which may be used for
	 * component configuration
	 *
	 * @return qualified annotated element value, which may be
	 * used for component configuration
	 */
	String value() default StringUtils.EMPTY;

	/**
	 * Type of referenced bean, which may be used for component
	 * configuration
	 *
	 * @return type of referenced bean, which may be used for
	 * component configuration
	 */
	Class<?> ref() default Object.class;

}
