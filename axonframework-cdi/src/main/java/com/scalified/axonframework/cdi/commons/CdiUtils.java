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

package com.scalified.axonframework.cdi.commons;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.enterprise.inject.spi.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Utilities for working with <b>CDI</b>
 *
 * @author shell
 * @since 2019-04-13
 */
@SuppressWarnings("unchecked")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CdiUtils {

	/**
	 * Returns the bean of the given {@code type} annotated by the
	 * given {@code qualifiers}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @param type        type to get the reference of
	 * @param qualifiers  qualifiers to choose among references
	 * @param <T>         bean type
	 * @return bean of the given {@code type} annotated by the
	 * given {@code qualifiers}
	 */
	public static <T> T getReference(BeanManager beanManager, Type type, Annotation... qualifiers) {
		Bean<?> bean = beanManager.resolve(beanManager.getBeans(type, qualifiers));
		return (T) beanManager.getReference(bean, type, beanManager.createCreationalContext(bean));
	}

	/**
	 * Produces bean using the given {@code producer}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @param producer    {@link Producer} to use
	 * @param <T>         bean type
	 * @return bean produced using the given {@code producer}
	 */
	public static <T> T produce(BeanManager beanManager, Producer<?> producer) {
		return (T) producer.produce(beanManager.createCreationalContext(null));
	}

	/**
	 * Returns {@code true} if the given {@code producer} has the given
	 * {@code annotationType}, otherwise returns {@code false}
	 *
	 * @param producer       {@link ProcessProducer} to check the
	 *                       annotation presence for
	 * @param annotationType annotation type to check
	 * @param <T>            the bean class of the bean that declares
	 *                       the producer method or field
	 * @param <K>            the return type of the producer method
	 *                       or the type of the producer field
	 * @return {@code true} if the given {@code producer} has the given
	 * {@code annotationType}, otherwise returns {@code false}
	 */
	public static <T, K> boolean hasAnnotation(ProcessProducer<T, K> producer,
	                                           Class<? extends Annotation> annotationType) {
		return producer.getAnnotatedMember().isAnnotationPresent(annotationType);
	}

	/**
	 * Returns {@code true} if the given {@code processAnnotatedType}
	 * has the given {@code annotationType}, otherwise returns {@code false}
	 *
	 * @param processAnnotatedType {@link ProcessAnnotatedType} to check
	 *                             the annotation presence for
	 * @param annotationType       annotation type to check
	 * @param <T>                  the class being annotated
	 * @return {@code true} if the given {@code processAnnotatedType}
	 * has the given {@code annotationType}, otherwise returns {@code false}
	 */
	public static <T> boolean hasAnnotation(ProcessAnnotatedType<T> processAnnotatedType,
	                                        Class<? extends Annotation> annotationType) {
		return processAnnotatedType.getAnnotatedType().isAnnotationPresent(annotationType);
	}

}
