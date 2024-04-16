# Event Bus Logging Filter JAX-RS

Questo è un progetto Quarkus creato come compendio per l'articolo [Sfruttare al massimo l'Event Bus di Quarkus: Utilizzi e Vantaggi](https://theredcode.it).
pubblicato sul blog [The Red Code](https://theredcode.it) di [Serena Sensini.](https://www.linkedin.com/in/serena-sensini/)

Questo progetto è un'applicazione Quarkus che mostra come realizzare un sistema che sia capace di
tracciare le richieste JAX-RS in arrivo e in uscita dall'applicazione su diversi canali di
storage, come ad esempio un database MongoDB, SQL o un broker AMQP sfruttando l'Event Bus 
di Quarkus.

[![Flusso Applicazione Quarkus](src/doc/resources/images/flusso_applicazione_quarkus.jpeg)](src/doc/resources/images/flusso_applicazione_quarkus.jpeg)

**Figura 1**: Flusso dell'applicazione Quarkus

Per i dettagli completi su com'è stata implementata l'applicazione, vi rimando alla lettura
dell'articolo [Sfruttare al massimo l'Event Bus di Quarkus: Utilizzi e Vantaggi](https://theredcode.it).

Se vuoi saperne di più su Quarkus, visita il sito ufficiale [quarkus.io](https://quarkus.io/).
A seguire trovi le istruzioni per eseguire l'applicazione in modalità sviluppo e creare un eseguibile
nativo.

## Esecuzione dell'applicazione in dev mode

Puoi eseguire l'applicazione in modalità sviluppo che abilita il live coding utilizzando:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus ora include una UI di sviluppo, disponibile solo in modalità sviluppo all'indirizzo http://localhost:8080/q/dev/.

## Packaging e avvio dell'applicazione

L'applicazione può essere impacchettata utilizzando:
```shell script
./mvnw package
```

Il processo produrrà il file `quarkus-run.jar` in `target/quarkus-app/`.
Questo non è un _über-jar_ in quanto le dipendenze sono copiate nella 
directory `target/quarkus-app/lib/`.

L'applicazione è ora eseguibile utilizzando `java -jar target/quarkus-app/quarkus-run.jar`.

Se vuoi creare un _über-jar_, esegui il seguente comando:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

L'applicazione, impacchettata come un _über-jar_, è ora eseguibile utilizzando `java -jar target/*-runner.jar`.

## Creazione di un eseguibile nativo

Puoi creare un eseguibile nativo utilizzando: 
```shell script
./mvnw package -Dnative
```

Nel caso in cui tu non avvessi GraalVM installato, puoi eseguire la build dell'eseguibile nativo in un container 
utilizzando:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Puoi esequire l'eseguibile nativo con: `./target/eventbus-logging-filter-jaxrs-1.0.0-SNAPSHOT-runner`

Se vuoi saperne di più sulla creazione di eseguibili nativi, consulta https://quarkus.io/guides/maven-tooling.

## Guida ai servizi e alle estensioni utilizzate

- MongoDB client ([guide](https://quarkus.io/guides/mongodb)): Connect to MongoDB in either imperative or reactive style
- Eclipse Vert.x ([guide](https://quarkus.io/guides/vertx)): Write reactive applications with the Vert.x API
- OpenShift ([guide](https://quarkus.io/guides/deploying-to-openshift)): Generate OpenShift resources from annotations
- ArC ([guide](https://quarkus.io/guides/cdi-reference)): Build time CDI dependency injection
- Messaging - AMQP Connector ([guide](https://quarkus.io/guides/amqp)): Connect to AMQP with Reactive Messaging
- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Validate object properties (field, getter) and method parameters for your beans (REST, CDI, Jakarta Persistence)
- Using Podman with Quarkus ([guide](https://quarkus.io/guides/podman))

## Team Tools

[![alt tag](http://pylonsproject.org/img/logo-jetbrains.png)](https://www.jetbrains.com/?from=LiferayPortalSecurityAudit)

Antonio Musarra's Blog Team would like inform that JetBrains is helping by
provided IDE to develop the application. Thanks to its support program for
an Open Source projects!

