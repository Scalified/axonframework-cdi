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
import org.axonframework.messaging.Message;
import org.axonframework.messaging.Scope;
import org.axonframework.messaging.ScopeDescriptor;
import org.axonframework.modelling.command.Aggregate;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.axonframework.modelling.command.Repository;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static java.util.Objects.isNull;

/**
 * Lazily initialized {@link Repository}
 *
 * @author shell
 * @since 2019-04-29
 */
@RequiredArgsConstructor
public class LazyRepository<T> implements Repository<T> {

	/**
	 * Underlying {@code repository}
	 */
	private Repository<T> repository;

	/**
	 * Supplier to initialize {@code repository}
	 */
	private final Supplier<Repository<T>> supplier;

	/**
	 * Load the aggregate with the given unique identifier.
	 * No version checks are done when loading an aggregate,
	 * meaning that concurrent access will not be checked for
	 *
	 * @param aggregateIdentifier the identifier of the aggregate to load
	 * @return the aggregate root with the given identifier
	 * @throws AggregateNotFoundException if aggregate with given id cannot be found
	 */
	@Override
	public Aggregate<T> load(String aggregateIdentifier) {
		return delegate().load(aggregateIdentifier);
	}

	/**
	 * Load the aggregate with the given unique identifier
	 *
	 * @param aggregateIdentifier the identifier of the aggregate to load
	 * @param expectedVersion     the expected version of the loaded aggregate
	 * @return the aggregate root with the given identifier
	 * @throws AggregateNotFoundException if aggregate with given id cannot be found
	 */
	@Override
	public Aggregate<T> load(String aggregateIdentifier, Long expectedVersion) {
		return delegate().load(aggregateIdentifier, expectedVersion);
	}

	/**
	 * Creates a new managed instance for the aggregate, using the given {@code factoryMethod}
	 * to instantiate the aggregate's root
	 *
	 * @param factoryMethod The method to create the aggregate's root instance
	 * @return an Aggregate instance describing the aggregate's state
	 * @throws Exception when the factoryMethod throws an exception
	 */
	@Override
	public Aggregate<T> newInstance(Callable<T> factoryMethod) throws Exception {
		return delegate().newInstance(factoryMethod);
	}

	/**
	 * Send a {@link Message} to a {@link Scope} which is described by the given
	 * {@code scopeDescription}
	 *
	 * @param message          a {@link Message} to be send to a {@link Scope}
	 * @param scopeDescription a {@code D} extending {@link ScopeDescriptor}, describing
	 *                         the {@link Scope} to send the given {@code message} to
	 * @throws Exception if sending the {@code message} failed. Might occur if the
	 *                   message handling process throws on exception
	 */
	@Override
	public void send(Message<?> message, ScopeDescriptor scopeDescription) throws Exception {
		delegate().send(message, scopeDescription);
	}

	/**
	 * Check whether this implementation can resolve a {@link Scope} object based on
	 * the provided {@code scopeDescription}. Will return {@code true} in case it
	 * should be able to resolve the Scope and {@code false} if it cannot
	 *
	 * @param scopeDescription a {@link ScopeDescriptor} describing the {@link Scope}
	 *                         to be resolved
	 * @return {@code true} in case it should be able to resolve the Scope and {@code false}
	 * if it cannot
	 */
	@Override
	public boolean canResolve(ScopeDescriptor scopeDescription) {
		return delegate().canResolve(scopeDescription);
	}

	/**
	 * Returns the underlying {@code repository}
	 *
	 * <p>
	 * Initialized underlying {@code repository} if it is not yet initialized
	 *
	 * @return underlying {@code repository}
	 */
	private Repository<T> delegate() {
		if (isNull(repository)) {
			repository = supplier.get();
		}
		return repository;
	}

}
