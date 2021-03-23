# AxonFramework CDI Integration Library

[![Build Status](https://travis-ci.com/Scalified/axonframework-cdi.svg)](https://travis-ci.com/Scalified/axonframework-cdi)
[![Maven Central](https://img.shields.io/maven-central/v/com.scalified/axonframework-cdi.svg)](https://search.maven.org/search?q=g:com.scalified%20AND%20a:axonframework-cdi&core=gav)

## Description

This Library provides an integration between [AxonFramework](https://axoniq.io/) and CDI specification from 
Java EE API version 8

Note that this is not official implementation. An official implementation provided by Axon team can be found among 
official public [Axon GitHub repositories](https://github.com/AxonFramework). The main idea of this Library is to have
a bit more different approach in configuration and to provide more convenient tools specific for Java EE world

## Requirements

The Library requires Java EE container

## Gradle dependency

```java
dependencies {
    implementation "com.scalified:axonframework-cdi:$VERSION"
}
```

## Version 

Each `$VERSION` consists of **AxonFramework** version and the Library version itself. For example, version 
**4.1.1-RELEASE** indicates the **AxonFramework** version **4.1.1** and the Library version **RELEASE**

## Changelog

[Changelog](CHANGELOG.md)

## Usage

The following **Axon** components are available for injection out-of-the-box:

 * org.axonframework.config.Configuration
 * org.axonframework.commandhandling.gateway.CommandGateway
 * org.axonframework.queryhandling.QueryGateway
 * org.axonframework.modelling.command.Repository (specific for concrete Aggregate type)

Example:

```java
// imports omitted
import org.axonframework.config.Configuration;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.command.Repository;
import org.axonframework.queryhandling.QueryGateway;

@ApplicationScoped
public class SomeBean {
    
    @Inject
    private Configuration configuration;
    
    @Inject
    private CommandGateway commandGateway;
    
    @Inject
    private QueryGateway queryGateway;
    
    @Inject
    private Repository<Account> accountRepository;
    
    @Inject
    private Repository<Creditor> creditorRepository;
    
}
```

### Lifecycle Events

Library provides the following lifecycle events:

 * com.scalified.axonframework.cdi.event.AxonEvent (supertype)
 * com.scalified.axonframework.cdi.event.AxonConfiguredEvent (fired once **Axon** configured)
 * com.scalified.axonframework.cdi.event.AxonStartedEvent (fired once **Axon** started)
 * com.scalified.axonframework.cdi.event.AxonStoppedEvent (fired once **Axon** stopped)

Events can be subscribed as follows:

```java
// imports omitted
import org.axonframework.config.Configuration;
import com.scalified.axonframework.cdi.event.AxonEvent;
import com.scalified.axonframework.cdi.event.AxonConfiguredEvent;
import com.scalified.axonframework.cdi.event.AxonStartedEvent;
import com.scalified.axonframework.cdi.event.AxonStoppedEvent;

@ApplicationScoped
public class AxonEventListener {

    void on(@Observes AxonEvent event) {
        // ...
    }

    void on(@Observes AxonConfiguredEvent event) {
        Configuration configuration = event.getConfiguration();
        // ...
    }

    void on(@Observes AxonStartedEvent event) {
        // ...
    }

    void on(@Observes AxonStoppedEvent event) {
        // ...
    }

}
```

### Properties

**Axon** library-specific properties can be provided as follows:

```java
// imports omitted
import com.scalified.axonframework.cdi.AxonProperties;

@ApplicationScoped
public class AxonConfiguration {

    AxonProperties axonProperties() {
        return AxonProperties.builder()
            .autoStartEnabled(false)
            .build();
    }

}
```

## Configuration

Any **Axon** component (except **Aggregates** and **Sagas**), which is expected to be processed, must be annotated with 
`@AxonComponent` annotation. This annotation can be put on either a type (class) or producer method (annotated 
with `@Produces`). During startup, all components annotated with `@AxonComponent` annotation are collected and
applied on **Axon** configuration. For the *known* **Axon** components the specific configuration method will be 
invoked (e.g. `configureTransactionManager`). For any other component the `registerComponent(..)` method 
will be invoked. For example:

```java
// imports omitted
import com.scalified.axonframework.cdi.api.annotation.AxonComponent;
import org.axonframework.common.transaction.TransactionManager;
import com.scalified.axonframework.cdi.configuration.transaction.JtaTransactionManager;

@ApplicationScoped
public class CdiSetup {
    
    @Produces
    @AxonComponent
    TransactionManager transactionManager() {
        return new JtaTransactionManager(); // configureTransactionManager(conf -> transactionManager);
    }
    
    @Produces
    @AxonComponent
    SomeBean someService() {
        return new SomeBean(); // registerComponent(SomeBean.class, conf -> someBean);
    }
    
}
```

Alternatively, components can be provided using `Configurable` function. This function accepts **Axon** `Configuration` 
object and returns the **Axon** component. This is extremely helpful for cases, when there is a need in **Axon** 
`Configuration` for configuring the component or when, for example, there is no way to annotate an existing component 
with `@AxonComponent` annotation (dependency etc.). The `Configurable` component can be either produced or declared:

```java
// imports omitted
import com.scalified.axonframework.cdi.api.annotation.AxonComponent;
import com.scalified.axonframework.cdi.api.Configurable;

@ApplicationScoped
public class CdiSetup {
    
    @Produces
    @AxonComponent
    Configurable<TokenStore> tokenStore(EntityManagerProvider provider) {
        return configuration -> JpaTokenStore.builder()
                .entityManagerProvider(provider)
                .serializer(configuration.serializer())
                .build();
    }

}
```

```java
// imports omitted
import com.scalified.axonframework.cdi.api.Configurable;
import com.scalified.axonframework.cdi.api.annotation.AxonComponent;

@AxonComponent
public class CommandBusConfigurable implements Configurable<CommandBus> {

    @Override
    public CommandBus apply(Configuration configuration) {
        return SimpleCommandBus.builder().build();
    }

}
``` 

**Aggregates** and **Sagas** have special annotations, which must be put on **Aggregate** or **Saga** type (class) 
instead of `@AxonComponent` annotation. These are `@Aggregate` and `@Saga` respectively.

### Aggregate

**Aggregate** configurations can be provided via `AggregateConfigurator` function, which accepts the **Aggregate** 
type (class) and returns `AggregateConfigurer` instance. For providing specific **Aggregate** configurations, the 
**Aggregate** type must be specified via `@AxonComponent` annotation's `ref()` method:

```java
// imports omitted
import com.scalified.axonframework.cdi.api.annotation.AxonComponent;
import com.scalified.axonframework.cdi.api.AggregateConfigurator;

@ApplicationScoped
public class CdiSetup {
    
    // Default AggregateConfigurator
    @Produces
    @AxonComponent
    AggregateConfigurator defaultAggregateConfigurator() {
        return type -> AggregateConfigurer.defaultConfiguration(type);
    }
    
    // AggregateConfigurator specific for Account Aggregate
    @Produces
    @AxonComponent(ref = Account.class)
    AggregateConfigurator accountAggregateConfigurator() {
        return type -> AggregateConfigurer.defaultConfiguration(type)
                .configureRepository(conf -> EventSourcingRepository.builder(type)
                            .eventStore(conf.eventStore())
                            .build());
    }

}
```

The **Aggregate** itself must be annotated with `@Aggregate` annotation:

```java
// imports omitted
import com.scalified.axonframework.cdi.api.annotation.Aggregate;

@Aggregate
public class Account {

    @AggregateIdentifier
    private String id;

    private double balance;

    public Account(OpenAccountCommand command) {
        AccountOpenedEvent event = AccountOpenedEvent.builder()
                .id(command.getId())
                .balance(command.getBalance())
                .build();
        apply(event);
    }

    @EventSourcingHandler
    public void on(AccountOpenedEvent event) {
        this.id = event.getId();
        this.balance = event.getBalance();
    }

}
```

### Saga

**Saga** configurations can be provided via `SagaConfigurator` consumer, which accepts the `SagaConfigurer` instance 
For providing specific **Saga** configurations, the **Saga** type must be specified via `@AxonComponent` 
annotation's `ref()` method:

```java
// imports omitted
import com.scalified.axonframework.cdi.api.annotation.AxonComponent;
import com.scalified.axonframework.cdi.api.SagaConfigurator;

@ApplicationScoped
public class CdiSetup {
    
    // Default SagaConfigurator
    @Produces
    @AxonComponent
    SagaConfigurator defaultSagaConfigurator(EntityManagerProvider provider) {
        return configurer -> configurer.configureSagaStore(conf -> JpaSagaStore.builder()
                .entityManagerProvider(provider)
                .serializer(conf.serializer())
                .build());
    }
    
    // SagaConfigurator specific for Account Saga
    @Produces
    @AxonComponent(ref = AccountSaga.class)
    SagaConfigurator accountSagaConfigurator() {
        return configurer -> configurer.configureSagaStore(conf -> JpaSagaStore.builder()
                .entityManagerProvider(provider)
                .serializer(XStreamSerializer.defaultSerializer())
                .build());
    }

}
```

The **Saga** itself must be annotated with `@Saga` annotation:

```java
// imports omitted
import com.scalified.axonframework.cdi.api.annotation.Saga;

@Saga
public class AccountSaga {

    @StartSaga
    @SagaEventHandler(associationProperty = "id")
    private void on(AccountOpenedEvent event) {
        // ...
    }

}
```

### Command Handler

```java
// imports omitted

@AxonComponent
public class AccountCommandHandler {

    @Inject
    private Repository<Account> repository;

    @CommandHandler
    public void handle(OpenAccountCommand command) throws Exception {
        repository.newInstance(() -> new Account(command));
    }

}
```

### Event Handler

```java
// imports omitted

@AxonComponent
public class AccountEventHandler {

    @EventHandler
    public void on(AccountOpenedEvent event) {
        // ...
    }

}
```

### Query Handler

```java
// imports omitted

@AxonComponent
public class AccountQueryHandler {
    
    @QueryHandler
    public void on(GetAccountBalanceQuery query) {
        // ...
    }
    
}
```

### Serializer

**Axon** distinguishes **default**, **message** and **event** serializers. To provide different serializers, its type 
must be specified via `@AxonComponent` annotation's `value()` method:

```java
// imports omitted
import com.scalified.axonframework.cdi.api.SerializerType;
import com.scalified.axonframework.cdi.api.annotation.AxonComponent;

@ApplicationScoped
public class CdiSetup {
    
    @Produces
    @AxonComponent(SerializerType.DEFAULT)
    Serializer serializer() {
        return JacksonSerializer.defaultSerializer();
    }

    @Produces
    @AxonComponent(SerializerType.MESSAGE)
    Serializer messageSerializer() {
        return JacksonSerializer.defaultSerializer();
    }

    @Produces
    @AxonComponent(SerializerType.EVENT)
    Serializer eventSerializer() {
        return JacksonSerializer.defaultSerializer();
    }

}
```

### Event Processing Configuration

Event processing can be further customized via `EventProcessingConfigurator` consumer, which accepts 
`EventProcessingConfigurer` instance:

```java
// imports omitted
import com.scalified.axonframework.cdi.api.annotation.AxonComponent;
import com.scalified.axonframework.cdi.api.EventProcessingConfigurator;

@ApplicationScoped
public class CdiSetup {
    
    @Produces
    @AxonComponent
    EventProcessingConfigurator eventProcessingConfigurator(EntityManagerProvider provider) {
        return configurer -> configurer.registerSagaStore(conf -> JpaSagaStore.builder()
                .serializer(conf.serializer())
                .entityManagerProvider(provider)
                .build());
    }

}
```

### Command Dispatch Interceptor

`MessageDispatchInterceptor` for commands, which implements `CommandDispatchInterceptor` interface are registered 
automatically:

```java
// imports omitted
import com.scalified.axonframework.cdi.api.CommandDispatchInterceptor;
import com.scalified.axonframework.cdi.api.annotation.AxonComponent;

@AxonComponent
public class NoOpCommandDispatchInterceptor implements CommandDispatchInterceptor {

    @Override
    public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(List<? extends CommandMessage<?>> messages) {
        return (idx, message) -> message;
    }

}
```

### Event Dispatch Interceptor

`MessageDispatchInterceptor` for events, which implements `EventDispatchInterceptor` interface are registered 
automatically:

```java
// imports omitted
import com.scalified.axonframework.cdi.api.EventDispatchInterceptor;
import com.scalified.axonframework.cdi.api.annotation.AxonComponent;

@AxonComponent
public class NoOpEventDispatchInterceptor implements EventDispatchInterceptor {

    @Override
    public BiFunction<Integer, EventMessage<?>, EventMessage<?>> handle(List<? extends EventMessage<?>> messages) {
        return (idx, message) -> message;
    }
    
}
```

## License

```
Copyright 2019 Scalified

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Scalified Links

* [Scalified](http://www.scalified.com)
* [Scalified Official Facebook Page](https://www.facebook.com/scalified)
* <a href="mailto:info@scalified.com?subject=[AxonFramework CDI Integration Library]: Proposals And Suggestions">Scalified Support</a>
