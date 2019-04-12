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
import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Utilities for working with Java Reflection
 *
 * @author shell
 * @since 2019-04-27
 */
@SuppressWarnings("unchecked")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionUtils {

	/**
	 * Returns the raw type resolved from the given {@code type}
	 *
	 * @param type type to resolve
	 * @param <T>  raw type
	 * @return resolved raw type if it can be resolved,
	 * {@code null} otherwise
	 */
	public static <T> Class<T> getRawType(Type type) {
		return (Class<T>) TypeUtils.getRawType(type, null);
	}

	/**
	 * Returns the type of the first {@code type} argument
	 *
	 * @param type    type from which to determine the type
	 *                parameters of {@code toClass} argument
	 * @param toClass class whose type parameters are to be
	 *                determined based on the subtype {@code type}
	 * @return type of the first {@code type} argument if it can be
	 * resolved, {@code null} otherwise
	 */
	public static Type getTypeArgument(Type type, Class<?> toClass) {
		Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(type, toClass);
		if (isNull(typeArguments) || typeArguments.isEmpty()) {
			return null;
		}
		return typeArguments.values().iterator().next();
	}

	/**
	 * Returns {@code true} if the given {@code type} has at least
	 * one {@link Method} annotated with the given {@code annotationType},
	 * {@code false} otherwise
	 *
	 * @param type           type to examine
	 * @param annotationType annotation type to search
	 * @return {@code true} if the given {@code type} has at least
	 * one {@link Method} annotated with the given {@code annotationType},
	 * {@code false} otherwise
	 */
	public static boolean hasAnnotatedMethod(Type type, Class<? extends Annotation> annotationType) {
		Class<?> classType = getRawType(type);
		return Arrays.stream(classType.getMethods())
				.anyMatch(method -> method.isAnnotationPresent(annotationType));
	}

}
