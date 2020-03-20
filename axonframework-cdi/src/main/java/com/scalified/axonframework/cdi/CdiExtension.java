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

import com.scalified.axonframework.cdi.api.*;
import com.scalified.axonframework.cdi.api.annotation.Aggregate;
import com.scalified.axonframework.cdi.api.annotation.AxonComponent;
import com.scalified.axonframework.cdi.api.annotation.Saga;
import com.scalified.axonframework.cdi.commons.CdiUtils;
import com.scalified.axonframework.cdi.commons.ReflectionUtils;
import com.scalified.axonframework.cdi.component.Component;
import com.scalified.axonframework.cdi.component.ComponentResolver;
import com.scalified.axonframework.cdi.component.ConfigurableComponentResolver;
import com.scalified.axonframework.cdi.configuration.LazyRetrievedModuleConfiguration;
import com.scalified.axonframework.cdi.configuration.transaction.JtaTransactionManager;
import com.scalified.axonframework.cdi.event.AxonConfiguredEvent;
import com.scalified.axonframework.cdi.event.AxonStartedEvent;
import com.scalified.axonframework.cdi.event.AxonStoppedEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.*;
import org.axonframework.eventhandling.ErrorHandler;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ListenerInvocationErrorHandler;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.messaging.correlation.CorrelationDataProvider;
import org.axonframework.messaging.interceptors.CorrelationDataInterceptor;
import org.axonframework.modelling.saga.ResourceInjector;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.upcasting.event.EventUpcaster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * {@link Extension} for <b>Axon</b> configuration
 *
 * @author shell
 * @since 2019-04-12
 */
@Slf4j
@ApplicationScoped
public class CdiExtension implements Extension {

	/**
	 * Underlying {@code configurer}
	 */
	private Configurer configurer = DefaultConfigurer.defaultConfiguration(false);

	/**
	 * Underlying {@code configuration}
	 */
	@Getter(AccessLevel.PACKAGE)
	private Configuration configuration;

	/**
	 * <b>Axon</b> configuration properties component
	 */
	private Component axonPropertiesComponent;

	/**
	 * {@link org.axonframework.modelling.command.Aggregate} types
	 */
	private final List<Class<?>> aggregateTypes = new LinkedList<>();

	/**
	 * Default {@link AggregateConfigurator} component
	 */
	private Component defaultAggregateConfiguratorComponent;

	/**
	 * {@link AggregateConfigurator} components
	 */
	private final Map<Class<?>, Component> aggregateConfiguratorComponents = new HashMap<>();

	/**
	 * {@link org.axonframework.modelling.saga.Saga} types
	 */
	private final List<Class<?>> sagaTypes = new LinkedList<>();

	/**
	 * Default {@link SagaConfigurator} component
	 */
	private Component defaultSagaConfiguratorComponent;

	/**
	 * {@link SagaConfigurator} components
	 */
	private final Map<Class<?>, Component> sagaConfiguratorComponents = new HashMap<>();

	/**
	 * {@link EventProcessingConfigurator} component
	 */
	private Component eventProcessingConfiguratorComponent;

	/**
	 * {@link Serializer} components
	 */
	private final Map<String, Component> serializerComponents = new HashMap<>();

	/**
	 * <b>CommandHandler</b> types
	 */
	private final List<Type> commandHandlerTypes = new LinkedList<>();

	/**
	 * <b>EventHandler</b> types
	 */
	private final List<Type> eventHandlerTypes = new LinkedList<>();

	/**
	 * <b>QueryHandler</b> types
	 */
	private final List<Type> queryHandlerTypes = new LinkedList<>();

	/**
	 * {@link TransactionManager} component
	 */
	private Component transactionManagerComponent;

	/**
	 * {@link CommandBus} component
	 */
	private Component commandBusComponent;

	/**
	 * {@link EventStorageEngine} component
	 */
	private Component eventStorageEngineComponent;

	/**
	 * {@link EventBus} component
	 */
	private Component eventBusComponent;

	/**
	 * {@link QueryBus} component
	 */
	private Component queryBusComponent;

	/**
	 * {@link ResourceInjector} component
	 */
	private Component resourceInjectorComponent;

	/**
	 * {@link QueryUpdateEmitter} component
	 */
	private Component queryUpdateEmitterComponent;

	/**
	 * {@link ErrorHandler} component
	 */
	private Component errorHandlerComponent;

	/**
	 * {@link ListenerInvocationErrorHandler} component
	 */
	private Component listenerInvocationErrorHandlerComponent;

	/**
	 * {@link CommandDispatchInterceptor} components
	 */
	private final List<Component> commandDispatchInterceptorComponents = new LinkedList<>();

	/**
	 * {@link EventDispatchInterceptor} components
	 */
	private final List<Component> eventDispatchInterceptorComponents = new LinkedList<>();

	/**
	 * {@link EventUpcaster} components
	 */
	private final List<Component> eventUpcasterComponents = new LinkedList<>();

	/**
	 * {@link CorrelationDataProvider} components
	 */
	private final List<Component> correlationDataProviderComponents = new LinkedList<>();

	/**
	 * {@link ModuleConfiguration} components
	 */
	private final List<Component> moduleConfigurationComponents = new LinkedList<>();

	/**
	 * {@link ConfigurerModule} components
	 */
	private final List<Component> configurerModuleComponents = new LinkedList<>();

	/**
	 * <b>Custom</b> components
	 */
	private final List<Component> customComponents = new LinkedList<>();

	/**
	 * Processes the given {@link AxonProperties} annotated {@code type}
	 *
	 * @param processAnnotatedType to process
	 * @param <T>                  {@link AxonProperties} type
	 */
	<T extends AxonProperties> void processAxonPropertiesAnnotatedType(
			@Observes ProcessAnnotatedType<T> processAnnotatedType) {
		Type type = processAnnotatedType.getAnnotatedType().getBaseType();
		axonPropertiesComponent = new Component(type);
		log.trace("Initialized AxonProperties component: {} ", axonPropertiesComponent);
	}

	/**
	 * Processes the given {@link AxonProperties} {@code processProducer}
	 *
	 * @param processProducer to process
	 * @param <T>             the bean class of the bean that declares the producer
	 *                        method or field
	 * @param <K>             {@link AxonProperties} type
	 */
	<T, K extends AxonProperties> void processAxonPropertiesProducer(@Observes ProcessProducer<T, K> processProducer) {
		Producer<K> producer = processProducer.getProducer();
		AnnotatedMember<T> annotatedMember = processProducer.getAnnotatedMember();
		Type type = annotatedMember.getBaseType();
		axonPropertiesComponent = new Component(type, producer);
		log.trace("Initialized AxonProperties component: {} ", axonPropertiesComponent);
	}

	/**
	 * Processes the given {@code type} annotated with {@code Aggregate} annotation
	 *
	 * @param type {@link ProcessAnnotatedType} to process
	 * @param <T>  the class being annotated
	 */
	<T> void processAggregateAnnotated(@Observes @WithAnnotations({Aggregate.class}) ProcessAnnotatedType<T> type) {
		Class<T> aggregateType = type.getAnnotatedType().getJavaClass();
		aggregateTypes.add(aggregateType);
		log.trace("Initialized Aggregate class: {}", aggregateType);
	}

	/**
	 * Processes the given {@code type} annotated with {@code Saga} annotation
	 *
	 * @param type {@link ProcessAnnotatedType} to process
	 * @param <T>  the class being annotated
	 */
	<T> void processSagaAnnotated(@Observes @WithAnnotations({Saga.class}) ProcessAnnotatedType<T> type) {
		Class<T> sagaType = type.getAnnotatedType().getJavaClass();
		sagaTypes.add(sagaType);
		log.trace("Initialized Saga class: {}", sagaType);
	}

	/**
	 * Processes the given {@code processAnnotatedType} annotated with
	 * {@code AxonComponent} annotation
	 *
	 * @param processAnnotatedType {@link ProcessAnnotatedType} to process
	 * @param <T>                  the class being annotated
	 */
	<T> void processAxonComponentAnnotatedType(
			@Observes @WithAnnotations({AxonComponent.class}) ProcessAnnotatedType<T> processAnnotatedType) {
		if (CdiUtils.hasAnnotation(processAnnotatedType, AxonComponent.class)) {
			AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();
			AxonComponent annotation = annotatedType.getAnnotation(AxonComponent.class);
			Type type = annotatedType.getBaseType();
			Component component = new Component(type);
			initAxonComponent(annotation, component);
		}
	}

	/**
	 * Processes the given {@code processProducer}
	 *
	 * @param processProducer {@link ProcessProducer} to process
	 * @param <T>             the bean class of the bean that declares the producer method or field
	 * @param <K>             the return type of the producer method or the type of the producer field
	 */
	<T, K> void processAxonComponentProducer(@Observes ProcessProducer<T, K> processProducer) {
		if (CdiUtils.hasAnnotation(processProducer, AxonComponent.class)) {
			Producer<K> producer = processProducer.getProducer();
			AnnotatedMember<T> annotatedMember = processProducer.getAnnotatedMember();
			AxonComponent annotation = annotatedMember.getAnnotation(AxonComponent.class);
			Type type = annotatedMember.getBaseType();
			Component component = new Component(type, producer);
			initAxonComponent(annotation, component);
		}
	}

	/**
	 * Initializes <b>Axon</b> {@code component} based on its type and the given
	 * {@code annotated} annotation initialization data
	 *
	 * @param annotation annotation use to obtain additional initialization data
	 * @param component  component to initialize
	 */
	private void initAxonComponent(AxonComponent annotation, Component component) {
		Type actualType = Optional.ofNullable(ReflectionUtils.getTypeArgument(component.getType(), Configurable.class))
				.orElse(component.getType());

		boolean initialized = false;

		if (TypeUtils.isAssignable(actualType, AggregateConfigurator.class)) {
			initialized = initAggregateConfiguratorComponent(annotation.ref(), component);
		}
		if (TypeUtils.isAssignable(actualType, SagaConfigurator.class)) {
			initialized = initSagaConfiguratorComponent(annotation.ref(), component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, EventProcessingConfigurator.class)) {
			initialized = initEventProcessingConfiguratorComponent(component) || initialized;
		}

		if (TypeUtils.isAssignable(actualType, Serializer.class)) {
			initialized = initSerializerComponent(annotation.value(), component) || initialized;
		}

		if (ReflectionUtils.hasAnnotatedMethod(actualType, CommandHandler.class)) {
			initialized = initCommandHandlerType(actualType) || initialized;
		}
		if (ReflectionUtils.hasAnnotatedMethod(actualType, EventHandler.class)) {
			initialized = initEventHandlerType(actualType) || initialized;
		}
		if (ReflectionUtils.hasAnnotatedMethod(actualType, QueryHandler.class)) {
			initialized = initQueryHandlerType(actualType) || initialized;
		}

		if (TypeUtils.isAssignable(actualType, TransactionManager.class)) {
			initialized = initTransactionManagerComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, CommandBus.class)) {
			initialized = initCommandBusComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, EventStorageEngine.class)) {
			initialized = initEventStorageEngineComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, EventBus.class)) {
			initialized = initEventBusComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, QueryBus.class)) {
			initialized = initQueryBusComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, ResourceInjector.class)) {
			initialized = initResourceInjectorComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, QueryUpdateEmitter.class)) {
			initialized = initQueryUpdateEmitterComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, ErrorHandler.class)) {
			initialized = initErrorHandlerComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, ListenerInvocationErrorHandler.class)) {
			initialized = initListenerInvocationErrorHandlerComponent(component) || initialized;
		}

		if (TypeUtils.isAssignable(actualType, CommandDispatchInterceptor.class)) {
			initialized = initCommandDispatchInterceptorComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, EventDispatchInterceptor.class)) {
			initialized = initEventDispatchInterceptorComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, EventUpcaster.class)) {
			initialized = initEventUpcasterComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, CorrelationDataProvider.class)) {
			initialized = initCorrelationDataProviderComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, ModuleConfiguration.class)) {
			initialized = initModuleConfigurationComponent(component) || initialized;
		}
		if (TypeUtils.isAssignable(actualType, ConfigurerModule.class)) {
			initialized = initConfigurerModuleComponent(component) || initialized;
		}

		if (!initialized) {
			initCustomComponent(component);
		}
	}

	/**
	 * Initializes {@code defaultAggregateConfiguratorComponent} if the given
	 * {@code aggregateType} is not specific (default), otherwise adds the given
	 * {@code component} to the {@code aggregateConfiguratorComponents} by the given
	 * {@code aggregateType}
	 *
	 * @param aggregateType {@link org.axonframework.modelling.command.Aggregate} type
	 * @param component     component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initAggregateConfiguratorComponent(Class<?> aggregateType, Component component) {
		if (!Objects.equals(Object.class, aggregateType)) {
			if (aggregateConfiguratorComponents.containsKey(aggregateType)) {
				throw new AxonConfigurationException(String.format("Multiple AggregateConfigurator components " +
						"for the same '%s' aggregate declared", aggregateType));
			}
			aggregateConfiguratorComponents.put(aggregateType, component);
			log.trace("Initialized AggregateConfigurator component: {}", component);
		} else {
			if (nonNull(defaultAggregateConfiguratorComponent)) {
				throw new AxonConfigurationException("Multiple default AggregateConfigurator components " +
						"declared. Please specify referenced aggregate to distinguish configurations");
			}
			defaultAggregateConfiguratorComponent = component;
			log.trace("Initialized default AggregateConfigurator component: {}", defaultAggregateConfiguratorComponent);
		}
		return true;
	}

	/**
	 * Initializes {@code defaultSagaConfiguratorComponent} if the given {@code sagaType}
	 * is not specific (default), otherwise adds the given {@code component} to the
	 * {@code sagaConfiguratorComponents} by the given {@code sagaType}
	 *
	 * @param sagaType  {@link org.axonframework.modelling.saga.Saga} type
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initSagaConfiguratorComponent(Class<?> sagaType, Component component) {
		if (!Objects.equals(Object.class, sagaType)) {
			if (sagaConfiguratorComponents.containsKey(sagaType)) {
				throw new AxonConfigurationException(String.format("Multiple SagaConfigurator components " +
						"for the same '%s' saga declared", sagaType));
			}
			sagaConfiguratorComponents.put(sagaType, component);
			log.trace("Initialized SagaConfigurator component: {}", component);
		} else {
			if (nonNull(defaultSagaConfiguratorComponent)) {
				throw new AxonConfigurationException("Multiple default SagaConfigurator components declared. " +
						"Please specify referenced saga to distinguish configurations");
			}
			defaultSagaConfiguratorComponent = component;
			log.trace("Initialized default SagaConfigurator component: {}", defaultSagaConfiguratorComponent);
		}
		return true;
	}

	/**
	 * Initializes {@code eventProcessingConfiguratorComponent} from the given
	 * {@code component}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initEventProcessingConfiguratorComponent(Component component) {
		if (nonNull(eventProcessingConfiguratorComponent)) {
			throw new AxonConfigurationException("Multiple EventProcessingConfigurator components declared");
		}
		eventProcessingConfiguratorComponent = component;
		log.trace("Initialized EventProcessingConfigurator component: {}", eventProcessingConfiguratorComponent);
		return true;
	}

	/**
	 * Adds the given {@code component} to {@code serializerComponents} by its
	 * {@code type}
	 *
	 * @param type      {@link SerializerType} qualifier value
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initSerializerComponent(String type, Component component) {
		String assignedSerializerType = StringUtils.isBlank(type) ? SerializerType.DEFAULT : type;
		String[] serializerTypes = new String[]{SerializerType.DEFAULT, SerializerType.EVENT, SerializerType.MESSAGE};
		if (!ArrayUtils.contains(serializerTypes, assignedSerializerType)) {
			throw new AxonConfigurationException(String.format("Unknown Serializer '%s' name declared. " +
					"Please choose one among SerializerType", assignedSerializerType));
		}
		if (serializerComponents.containsKey(assignedSerializerType)) {
			throw new AxonConfigurationException(String.format("Multiple Serializer components declared " +
					"for %s serializer", assignedSerializerType));
		}
		serializerComponents.put(assignedSerializerType, component);
		log.trace("Initialized Serializer component: {}", component);
		return true;
	}

	/**
	 * Adds the given <b>CommandHandler</b> {@code type} to {@code commandHandlerTypes}
	 *
	 * @param type <b>CommandHandler</b> type to add
	 * @return {@code true} if the <b>CommandHandler</b> {@code type} was added,
	 * {@code false} otherwise
	 */
	private boolean initCommandHandlerType(Type type) {
		commandHandlerTypes.add(type);
		log.trace("Initialized CommandHandler class: {}", type);
		return true;
	}

	/**
	 * Adds the given <b>EventHandler</b> {@code type} to {@code eventHandlerTypes}
	 *
	 * @param type <b>EventHandler</b> type to add
	 * @return {@code true} if the <b>EventHandler</b> {@code type} was added,
	 * {@code false} otherwise
	 */
	private boolean initEventHandlerType(Type type) {
		eventHandlerTypes.add(type);
		log.trace("Initialized EventHandler class: {}", type);
		return true;
	}

	/**
	 * Adds the given <b>QueryHandler</b> {@code type} to {@code queryHandlerTypes}
	 *
	 * @param type <b>QueryHandler</b> type to add
	 * @return {@code true} if the <b>QueryHandler</b> {@code type} was added,
	 * {@code false} otherwise
	 */
	private boolean initQueryHandlerType(Type type) {
		queryHandlerTypes.add(type);
		log.trace("Initialized QueryHandler class: {}", type);
		return true;
	}

	/**
	 * Initializes {@code transactionManagerComponent} from the given {@code component}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initTransactionManagerComponent(Component component) {
		if (nonNull(transactionManagerComponent)) {
			throw new AxonConfigurationException("Multiple TransactionManager components declared");
		}
		transactionManagerComponent = component;
		log.trace("Initialized TransactionManager component: {}", component);
		return true;
	}

	/**
	 * Initializes {@code commandBusComponent} from the given {@code component}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initCommandBusComponent(Component component) {
		if (nonNull(commandBusComponent)) {
			throw new AxonConfigurationException("Multiple CommandBus components declared");
		}
		commandBusComponent = component;
		log.trace("Initialized CommandBus component: {}", component);
		return true;
	}

	/**
	 * Initializes {@code eventStorageEngineComponent} from the given {@code component}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initEventStorageEngineComponent(Component component) {
		if (nonNull(eventStorageEngineComponent)) {
			throw new AxonConfigurationException("Multiple EventStorageEngine components declared");
		}
		eventStorageEngineComponent = component;
		log.trace("Initialized EventStorageEngine component: {}", component);
		return true;
	}

	/**
	 * Initializes {@code eventBusComponent} from the given {@code component}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initEventBusComponent(Component component) {
		if (nonNull(eventBusComponent)) {
			throw new AxonConfigurationException("Multiple EventBus components declared");
		}
		eventBusComponent = component;
		log.trace("Initialized EventBus component: {}", component);
		return true;
	}

	/**
	 * Initializes {@code queryBusComponent} from the given {@code component}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initQueryBusComponent(Component component) {
		if (nonNull(queryBusComponent)) {
			throw new AxonConfigurationException("Multiple QueryBus components declared");
		}
		queryBusComponent = component;
		log.trace("Initialized QueryBus component: {}", component);
		return true;
	}

	/**
	 * Initializes {@code resourceInjectorComponent} from the given {@code component}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initResourceInjectorComponent(Component component) {
		if (nonNull(resourceInjectorComponent)) {
			throw new AxonConfigurationException("Multiple ResourceInjector components declared");
		}
		resourceInjectorComponent = component;
		log.trace("Initialized ResourceInjector component: {}", component);
		return true;
	}

	/**
	 * Initializes {@code queryUpdateEmitterComponent} from the given {@code component}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initQueryUpdateEmitterComponent(Component component) {
		if (nonNull(queryUpdateEmitterComponent)) {
			throw new AxonConfigurationException("Multiple QueryUpdateEmitter components declared");
		}
		queryUpdateEmitterComponent = component;
		log.trace("Initialized QueryUpdateEmitter component: {}", component);
		return true;
	}

	/**
	 * Initializes {@code errorHandlerComponent} from the given {@code component}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initErrorHandlerComponent(Component component) {
		if (nonNull(errorHandlerComponent)) {
			throw new AxonConfigurationException("Multiple ErrorHandler components declared");
		}
		errorHandlerComponent = component;
		log.trace("Initialized ErrorHandler component: {}", component);
		return true;
	}

	/**
	 * Initializes {@code listenerInvocationErrorHandlerComponent} from the given {@code component}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 * @throws AxonConfigurationException in case of initialization error
	 */
	private boolean initListenerInvocationErrorHandlerComponent(Component component) {
		if (nonNull(listenerInvocationErrorHandlerComponent)) {
			throw new AxonConfigurationException("Multiple ListenerInvocationErrorHandler components declared");
		}
		listenerInvocationErrorHandlerComponent = component;
		log.trace("Initialized ListenerInvocationErrorHandler component: {}", component);
		return true;
	}

	/**
	 * Adds the given {@code component} to the {@code commandDispatchInterceptorComponents}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 */
	private boolean initCommandDispatchInterceptorComponent(Component component) {
		commandDispatchInterceptorComponents.add(component);
		log.trace("Initialized CommandDispatchInterceptor component: {}", component);
		return true;
	}

	/**
	 * Adds the given {@code component} to the {@code eventDispatchInterceptorComponents}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 */
	private boolean initEventDispatchInterceptorComponent(Component component) {
		eventDispatchInterceptorComponents.add(component);
		log.trace("Initialized EventDispatchInterceptor component: {}", component);
		return true;
	}

	/**
	 * Adds the given {@code component} to the {@code eventUpcasterComponents}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 */
	private boolean initEventUpcasterComponent(Component component) {
		eventUpcasterComponents.add(component);
		log.trace("Initialized EventUpcaster component: {}", component);
		return true;
	}

	/**
	 * Adds the given {@code component} to the {@code correlationDataProviderComponents}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 */
	private boolean initCorrelationDataProviderComponent(Component component) {
		correlationDataProviderComponents.add(component);
		log.trace("Initialized CorrelationDataProvider component: {}", component);
		return true;
	}

	/**
	 * Adds the given {@code component} to the {@code moduleConfiguration}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 */
	private boolean initModuleConfigurationComponent(Component component) {
		moduleConfigurationComponents.add(component);
		log.trace("Initialized ModuleConfiguration component: {}", component);
		return true;
	}

	/**
	 * Adds the given {@code component} to the {@code configurerModuleComponents}
	 *
	 * @param component component to initialize
	 * @return {@code true} if the {@code component} was initialized, {@code false} otherwise
	 */
	private boolean initConfigurerModuleComponent(Component component) {
		configurerModuleComponents.add(component);
		log.trace("Initialized ConfigurerModule component: {}", component);
		return true;
	}

	/**
	 * Adds the given {@code component} to the {@code customComponents}
	 *
	 * @param component component to initialize
	 */
	private void initCustomComponent(Component component) {
		customComponents.add(component);
		log.trace("Initialized custom component: {}", component);
	}

	/**
	 * Invoked after container has validated that there are no deployment problems
	 * and before creating contexts or processing requests
	 *
	 * <p>
	 * Fires {@link AxonConfiguredEvent}
	 *
	 * <p>
	 * May fire {@link AxonStartedEvent} if {@link AxonProperties#isAutoStartEnabled()}
	 * is {@code true}
	 *
	 * @param event       an event fired after deployment validated
	 * @param beanManager current {@link BeanManager}
	 * @see AxonProperties
	 * @see AxonConfiguredEvent
	 * @see AxonStartedEvent
	 */
	void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
		log.debug("Configuring Axon application");

		AxonProperties axonProperties = resolveProperties(beanManager);

		configureAggregates(beanManager);
		configureSagas(beanManager);
		configureEventProcessing(beanManager);

		configureSerializer(beanManager);

		configureCommandHandlers(beanManager);
		configureEventHandlers(beanManager);
		configureQueryHandlers(beanManager);

		configureTransactionManager(beanManager);
		configureCommandBus(beanManager);
		configureEventBus(beanManager);
		configureEventStorageEngine(beanManager);
		configureQueryBus(beanManager);
		configureResourceInjector(beanManager);
		configureQueryUpdateEmitter(beanManager);
		configureErrorHandler(beanManager);
		configureListenerInvocationErrorHandler(beanManager);

		configureEventUpcasters(beanManager);
		configureModules(beanManager);
		configureConfigurerModules(beanManager);
		configureCorrelationDataProviders(beanManager);

		configureCustomComponents(beanManager);

		configuration = configurer.buildConfiguration();

		configureCommandDispatchInterceptors(beanManager);
		configureEventDispatchInterceptors(beanManager);

		log.info("Axon application configured");
		beanManager.fireEvent(new AxonConfiguredEvent(configuration));

		if (axonProperties.isAutoStartEnabled()) {
			log.debug("Starting Axon application");
			configuration.start();
			log.info("Axon application started");
			beanManager.fireEvent(new AxonStartedEvent());
		} else {
			log.info("Skipping Axon auto start since disabled");
		}
	}

	/**
	 * Resolves <b>Axon</b> configuration properties
	 *
	 * @param beanManager current {@link BeanManager}
	 * @return <b>Axon</b> configuration properties
	 */
	private AxonProperties resolveProperties(BeanManager beanManager) {
		return Optional.ofNullable(axonPropertiesComponent)
				.map(component -> ComponentResolver.of(component).<AxonProperties>resolve(beanManager))
				.orElseGet(() -> AxonProperties.builder().build());
	}

	/**
	 * Configures <b>Axon</b> {@link org.axonframework.modelling.command.Aggregate} instances
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#configureAggregate(Class)
	 * @see Configurer#configureAggregate(AggregateConfiguration)
	 */
	private void configureAggregates(BeanManager beanManager) {
		List<Class<?>> unknownAggregateReferences = aggregateConfiguratorComponents.keySet()
				.stream()
				.filter(type -> !aggregateTypes.contains(type))
				.collect(Collectors.toList());

		if (!unknownAggregateReferences.isEmpty()) {
			throw new AxonConfigurationException(String.format("Unknown AggregateConfigurator components for " +
					"aggregates declared: %s", unknownAggregateReferences));
		}

		for (Class<?> aggregateType : aggregateTypes) {
			Component component = aggregateConfiguratorComponents.getOrDefault(
					aggregateType, defaultAggregateConfiguratorComponent);
			if (isNull(component)) {
				configurer = configurer.configureAggregate(aggregateType);
			} else {
				AggregateConfigurator configurator = ComponentResolver.of(component).resolve(beanManager);
				configurer = configurer.configureAggregate(
						configurator.apply(ReflectionUtils.getRawType(aggregateType)));
			}
			log.debug("Configured Aggregate: {}", aggregateType);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link org.axonframework.modelling.saga.Saga} instances
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see EventProcessingConfigurer#registerSaga(Class)
	 * @see EventProcessingConfigurer#registerSaga(Class, Consumer)
	 */
	private void configureSagas(BeanManager beanManager) {
		List<Class<?>> unknownSagaReferences = sagaConfiguratorComponents.keySet()
				.stream()
				.filter(type -> !sagaTypes.contains(type))
				.collect(Collectors.toList());

		if (!unknownSagaReferences.isEmpty()) {
			throw new AxonConfigurationException(String.format("Unknown SagaConfigurator references to " +
					"sagas declared: %s", unknownSagaReferences));
		}

		for (Class<?> sagaType : sagaTypes) {
			Component component =
					sagaConfiguratorComponents.getOrDefault(sagaType, defaultSagaConfiguratorComponent);
			if (isNull(component)) {
				configurer = configurer.eventProcessing(eventProcessingConfigurer ->
						eventProcessingConfigurer.registerSaga(sagaType));
			} else {
				SagaConfigurator configurator = ComponentResolver.of(component).resolve(beanManager);
				configurer = configurer.eventProcessing(eventProcessingConfigurer ->
						eventProcessingConfigurer.registerSaga(ReflectionUtils.getRawType(sagaType), configurator));
			}
			log.debug("Configured Saga: {}", sagaType);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link EventProcessingConfigurer}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#eventProcessing(Consumer)
	 */
	private void configureEventProcessing(BeanManager beanManager) {
		if (nonNull(eventProcessingConfiguratorComponent)) {
			EventProcessingConfigurator configurator =
					ComponentResolver.of(eventProcessingConfiguratorComponent).resolve(beanManager);
			configurer = configurer.eventProcessing(configurator);
			log.debug("Configured EventProcessing component: {}", eventProcessingConfiguratorComponent);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link Serializer} instances
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#configureSerializer(Function)
	 * @see Configurer#configureMessageSerializer(Function)
	 * @see Configurer#configureEventSerializer(Function)
	 */
	private void configureSerializer(BeanManager beanManager) {
		if (!serializerComponents.isEmpty()) {
			Component defaultSerializerComponent = serializerComponents.get(SerializerType.DEFAULT);
			if (nonNull(defaultSerializerComponent)) {
				ConfigurableComponentResolver resolver = ConfigurableComponentResolver.of(defaultSerializerComponent);
				Configurable<Serializer> defaultSerializer = resolver.resolve(beanManager);
				configurer = configurer.configureSerializer(defaultSerializer);
				log.debug("Configured default Serializer: {}", defaultSerializerComponent);
			}

			Component messageSerializerComponent = serializerComponents.get(SerializerType.MESSAGE);
			if (nonNull(messageSerializerComponent)) {
				Configurable<Serializer> messageSerializer =
						ConfigurableComponentResolver.of(messageSerializerComponent).resolve(beanManager);
				configurer = configurer.configureMessageSerializer(messageSerializer);
				log.debug("Configured message Serializer: {}", messageSerializerComponent);
			}

			Component eventSerializerComponent = serializerComponents.get(SerializerType.EVENT);
			if (nonNull(eventSerializerComponent)) {
				Configurable<Serializer> eventSerializer =
						ConfigurableComponentResolver.of(eventSerializerComponent).resolve(beanManager);
				configurer = configurer.configureEventSerializer(eventSerializer);
				log.debug("Configured event Serializer: {}", eventSerializerComponent);
			}
		}
	}

	/**
	 * Configures <b>Axon</b> <b>CommandHandler</b> instances
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#registerCommandHandler(Function)
	 */
	private void configureCommandHandlers(BeanManager beanManager) {
		for (Type commandHandlerType : commandHandlerTypes) {
			Object commandHandler = CdiUtils.getReference(beanManager, commandHandlerType);
			configurer = configurer.registerCommandHandler(conf -> commandHandler);
			log.debug("Configured CommandHandler: {}", commandHandlerType);
		}
	}

	/**
	 * Configures <b>Axon</b> <b>EventHandler</b> instances
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#registerEventHandler(Function)
	 */
	private void configureEventHandlers(BeanManager beanManager) {
		for (Type eventHandlerType : eventHandlerTypes) {
			Object eventHandler = CdiUtils.getReference(beanManager, eventHandlerType);
			configurer = configurer.registerEventHandler(conf -> eventHandler);
			log.debug("Configured EventHandler: {}", eventHandlerType);
		}
	}

	/**
	 * Configures <b>Axon</b> <b>QueryHandler</b> instances
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#registerQueryHandler(Function)
	 */
	private void configureQueryHandlers(BeanManager beanManager) {
		for (Type queryHandlerType : queryHandlerTypes) {
			Object queryHandler = CdiUtils.getReference(beanManager, queryHandlerType);
			configurer = configurer.registerQueryHandler(conf -> queryHandler);
			log.debug("Configured QueryHandler: {}", queryHandlerType);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link TransactionManager}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#configureTransactionManager(Function)
	 */
	private void configureTransactionManager(BeanManager beanManager) {
		if (nonNull(transactionManagerComponent)) {
			Configurable<TransactionManager> configurable =
					ConfigurableComponentResolver.of(transactionManagerComponent).resolve(beanManager);
			configurer = configurer.configureTransactionManager(configurable);
			log.debug("Configured TransactionManager component: {}", transactionManagerComponent);
		} else {
			configurer = configurer.configureTransactionManager(conf -> new JtaTransactionManager());
			log.debug("Configured default JtaTransactionManager");
		}
	}

	/**
	 * Configures <b>Axon</b> {@link CommandBus}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#configureCommandBus(Function)
	 */
	private void configureCommandBus(BeanManager beanManager) {
		if (nonNull(commandBusComponent)) {
			Configurable<CommandBus> configurable =
					ConfigurableComponentResolver.of(commandBusComponent).resolve(beanManager);
			configurer = configurer.configureCommandBus(conf -> {
				CommandBus commandBus = configurable.apply(conf);
				commandBus.registerHandlerInterceptor(
						new CorrelationDataInterceptor<>(conf.correlationDataProviders()));
				return commandBus;
			});
			log.debug("Configured CommandBus: {}", commandBusComponent);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link EventStorageEngine}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#configureEmbeddedEventStore(Function)
	 */
	private void configureEventStorageEngine(BeanManager beanManager) {
		if (nonNull(eventStorageEngineComponent)) {
			Configurable<EventStorageEngine> configurable =
					ConfigurableComponentResolver.of(eventStorageEngineComponent).resolve(beanManager);
			configurer = configurer.configureEmbeddedEventStore(configurable);
			log.debug("Configured EventStorageEngine: {}", eventStorageEngineComponent);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link EventBus}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#configureEventBus(Function)
	 */
	private void configureEventBus(BeanManager beanManager) {
		if (nonNull(eventBusComponent)) {
			Configurable<EventBus> configurable =
					ConfigurableComponentResolver.of(eventBusComponent).resolve(beanManager);
			configurer = configurer.configureEventBus(configurable);
			log.debug("Configured EventBus: {}", eventStorageEngineComponent);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link QueryBus}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#configureQueryBus(Function)
	 */
	private void configureQueryBus(BeanManager beanManager) {
		if (nonNull(queryBusComponent)) {
			Configurable<QueryBus> configurable =
					ConfigurableComponentResolver.of(queryBusComponent).resolve(beanManager);
			configurer = configurer.configureQueryBus(configurable);
			log.debug("Configured QueryBus: {}", queryBusComponent);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link ResourceInjector}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#configureResourceInjector(Function)
	 */
	private void configureResourceInjector(BeanManager beanManager) {
		if (nonNull(resourceInjectorComponent)) {
			Configurable<ResourceInjector> configurable =
					ConfigurableComponentResolver.of(resourceInjectorComponent).resolve(beanManager);
			configurer = configurer.configureResourceInjector(configurable);
			log.debug("Configured ResourceInjector: {}", resourceInjectorComponent);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link QueryUpdateEmitter}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#configureQueryUpdateEmitter(Function)
	 */
	private void configureQueryUpdateEmitter(BeanManager beanManager) {
		if (nonNull(queryUpdateEmitterComponent)) {
			Configurable<QueryUpdateEmitter> configurable =
					ConfigurableComponentResolver.of(queryUpdateEmitterComponent).resolve(beanManager);
			configurer = configurer.configureQueryUpdateEmitter(configurable);
			log.debug("Configured QueryUpdateEmitter: {}", queryUpdateEmitterComponent);
		}
	}

	/**
	 * Configures <b>Axon</b> default {@link ErrorHandler}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see EventProcessingConfigurer#registerDefaultErrorHandler(Function)
	 */
	private void configureErrorHandler(BeanManager beanManager) {
		if (nonNull(errorHandlerComponent)) {
			Configurable<ErrorHandler> configurable =
					ConfigurableComponentResolver.of(errorHandlerComponent).resolve(beanManager);
			configurer = configurer.eventProcessing(eventProcessingConfigurer ->
					eventProcessingConfigurer.registerDefaultErrorHandler(configurable));
			log.debug("Configured default ErrorHandler: {}", errorHandlerComponent);
		}
	}

	/**
	 * Configures <b>Axon</b> default {@link ListenerInvocationErrorHandler}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see EventProcessingConfigurer#registerDefaultListenerInvocationErrorHandler(Function)
	 */
	private void configureListenerInvocationErrorHandler(BeanManager beanManager) {
		if (nonNull(listenerInvocationErrorHandlerComponent)) {
			Configurable<ListenerInvocationErrorHandler> configurable =
					ConfigurableComponentResolver.of(listenerInvocationErrorHandlerComponent).resolve(beanManager);
			configurer = configurer.eventProcessing(eventProcessingConfigurer ->
					eventProcessingConfigurer.registerDefaultListenerInvocationErrorHandler(configurable));
			log.debug("Configured default ListenerInvocationErrorHandler: {}", listenerInvocationErrorHandlerComponent);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link EventUpcaster}
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#registerEventUpcaster(Function)
	 */
	private void configureEventUpcasters(BeanManager beanManager) {
		for (Component component : eventUpcasterComponents) {
			Configurable<EventUpcaster> configurable = ConfigurableComponentResolver.of(component).resolve(beanManager);
			configurer = configurer.registerEventUpcaster(configurable);
			log.debug("Configured EventUpcaster: {}", component);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link CorrelationDataProvider} instances
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#configureCorrelationDataProviders(Function)
	 */
	private void configureCorrelationDataProviders(BeanManager beanManager) {
		List<CorrelationDataProvider> correlationDataProviders = correlationDataProviderComponents.stream()
				.map(component -> ComponentResolver.of(component).<CorrelationDataProvider>resolve(beanManager))
				.collect(Collectors.toList());

		if (!correlationDataProviders.isEmpty()) {
			configurer.configureCorrelationDataProviders(conf -> correlationDataProviders);
			log.debug("Configured CorrelationDataProvider instances: {}", correlationDataProviderComponents);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link ModuleConfiguration} instances
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see Configurer#registerModule(ModuleConfiguration)
	 */
	private void configureModules(BeanManager beanManager) {
		for (Component component : moduleConfigurationComponents) {
			ModuleConfiguration moduleConfiguration = ComponentResolver.of(component).resolve(beanManager);
			configurer = configurer.registerModule(new LazyRetrievedModuleConfiguration(() ->
					moduleConfiguration, component.getType()));
			log.debug("Configured ModuleConfiguration: {}", component);
		}
	}

	/**
	 * Configures <b>Axon</b> {@link ConfigurerModule} instances
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see ConfigurerModule#configureModule(Configurer)
	 */
	private void configureConfigurerModules(BeanManager beanManager) {
		for (Component component : configurerModuleComponents) {
			ConfigurerModule configurerModule = ComponentResolver.of(component).resolve(beanManager);
			configurerModule.configureModule(configurer);
			log.debug("Configured ConfigurerModule: {}", configurerModule);
		}
	}

	/**
	 * Configures <b>Axon</b> custom components
	 *
	 * @param beanManager current {@link BeanManager}
	 * @param <T>         custom component type
	 * @see Configurer#registerComponent(Class, Function)
	 */
	private <T> void configureCustomComponents(BeanManager beanManager) {
		for (Component component : customComponents) {
			Configurable<T> configurable = ConfigurableComponentResolver.of(component).resolve(beanManager);
			Type actualType = Optional.ofNullable(
					ReflectionUtils.getTypeArgument(component.getType(), Configurable.class))
					.orElse(component.getType());
			configurer = configurer.registerComponent(ReflectionUtils.getRawType(actualType), configurable);
			log.debug("Configured custom component: {}", component);
		}
	}

	/**
	 * Configures <b>Axon</b> command {@link MessageDispatchInterceptor} instances
	 *
	 * <p>
	 * Must be called after {@code configuration} initialization and before
	 * {@code configuration} start
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see CommandBus#registerDispatchInterceptor(MessageDispatchInterceptor)
	 */
	private void configureCommandDispatchInterceptors(BeanManager beanManager) {
		CommandBus commandBus = configuration.commandBus();
		for (Component component : commandDispatchInterceptorComponents) {
			CommandDispatchInterceptor interceptor = ComponentResolver.of(component).resolve(beanManager);
			commandBus.registerDispatchInterceptor(interceptor);
			log.debug("Configured CommandDispatchInterceptor: {}", component);
		}
	}

	/**
	 * Configures <b>Axon</b> event {@link MessageDispatchInterceptor} instances
	 *
	 * <p>
	 * Must be called after {@code configuration} initialization and before
	 * {@code configuration} start
	 *
	 * @param beanManager current {@link BeanManager}
	 * @see EventBus#registerDispatchInterceptor(MessageDispatchInterceptor)
	 */
	private void configureEventDispatchInterceptors(BeanManager beanManager) {
		EventBus eventBus = configuration.eventBus();
		for (Component component : eventDispatchInterceptorComponents) {
			EventDispatchInterceptor interceptor = ComponentResolver.of(component).resolve(beanManager);
			eventBus.registerDispatchInterceptor(interceptor);
			log.debug("Configured EventDispatchInterceptor: {}", component);
		}
	}

	/**
	 * Invoked after container has finished processing requests and destroyed all contexts
	 *
	 * <p>
	 * Fires {@link AxonStoppedEvent}
	 *
	 * @param event       an event fired after deployment validated
	 * @param beanManager current {@link BeanManager}
	 * @see AxonStoppedEvent
	 */
	void beforeShutdown(@Observes BeforeShutdown event, BeanManager beanManager) {
		log.debug("Stopping Axon application");
		configuration.shutdown();
		log.info("Axon application stopped");
		beanManager.fireEvent(new AxonStoppedEvent());
	}

}
