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

package com.scalified.axonframework.cdi;

import com.scalified.axonframework.cdi.commons.CdiUtils;
import com.scalified.axonframework.cdi.commons.ReflectionUtils;
import com.scalified.axonframework.cdi.configuration.LazyRepository;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.Configuration;
import org.axonframework.modelling.command.Aggregate;
import org.axonframework.modelling.command.Repository;
import org.axonframework.queryhandling.QueryGateway;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.reflect.Type;

/**
 * Configuration class containing <b>Axon</b> components bean producers
 *
 * @author shell
 * @since 2019-04-12
 */
@ApplicationScoped
public class CdiSetup {

	/**
	 * Produces <b>Axon</b> {@link Configuration} {@link ApplicationScoped} bean
	 *
	 * @param extension extension to use
	 * @return <b>Axon</b> {@link Configuration} {@link ApplicationScoped} bean
	 */
	@Produces
	@ApplicationScoped
	Configuration configuration(CdiExtension extension) {
		return extension.getConfiguration();
	}

	/**
	 * Produces <b>Axon</b> {@link CommandGateway} {@link ApplicationScoped} bean
	 *
	 * @param configuration <b>Axon</b> {@link Configuration}
	 * @return <b>Axon</b> {@link CommandGateway} {@link ApplicationScoped} bean
	 */
	@Produces
	@ApplicationScoped
	CommandGateway commandGateway(Configuration configuration) {
		return configuration.commandGateway();
	}

	/**
	 * Produces <b>Axon</b> {@link QueryGateway} {@link ApplicationScoped} bean
	 *
	 * @param configuration <b>Axon</b> {@link Configuration}
	 * @return <b>Axon</b> {@link QueryGateway} {@link ApplicationScoped} bean
	 */
	@Produces
	@ApplicationScoped
	QueryGateway queryGateway(Configuration configuration) {
		return configuration.queryGateway();
	}

	/**
	 * Produces <b>Axon</b> {@link Repository} {@link Dependent} bean
	 *
	 * @param point       point to use
	 * @param beanManager current {@link BeanManager}
	 * @param <T>         type of {@link Aggregate}
	 * @return <b>Axon</b> {@link Repository} {@link Dependent} bean
	 */
	@Produces
	<T> Repository<T> repository(InjectionPoint point, BeanManager beanManager) {
		Type aggregateType = ReflectionUtils.getTypeArgument(point.getType(), Repository.class);
		return new LazyRepository<>(() -> {
			Configuration configuration = CdiUtils.getReference(beanManager, Configuration.class);
			return configuration.repository(ReflectionUtils.getRawType(aggregateType));
		});
	}

}
