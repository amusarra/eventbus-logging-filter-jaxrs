# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
### Changed
### Removed
### Deprecated
### Security

## [1.4.1] - 2026-04-14

### Added
- **`EchoResourceEndPoint`**: new reactive endpoint `POST /api/rest/echo/reactive` returning
  `Uni<Response>`, enabling non-blocking echo with the same body-size validation as the
  blocking endpoint (`@Size(min = 32, max = 4096)`). Used also to verify the event-loop
  code path in `TraceJaxRsRequestResponseFilter#readBodyAsync` (lines 304–316).
- **Unit-test coverage ≥ 95 % on both target classes** (JaCoCo line coverage):
  - `TraceEventDispatcher`: **97.6 %** (↑ from ~80 %)
  - `TraceJaxRsRequestResponseFilter`: **95.0 %** (↑ from ~88 %)
- New test classes:
  - `TraceEventDispatcherTest` - covers backpressure drop paths (`enqueueRequest` /
    `enqueueResponse` with full queue), `AtomicLong` dropped-event counters and pressure
    warning (`checkPressure`). Uses reflection to replace the internal `ArrayBlockingQueue`
    with a capacity-1 queue at runtime - no `@TestProfile` restart required, no impact on
    Dev Services.
  - `TraceJaxRsFilterDisabledTest` - covers the `filterEnabled = false` early-return branches
    in both `requestFilter` (line 155–156) and `responseFilter` (line 224–225).
  - `TraceJaxRsFilterUriNotMatchedTest` - covers the URI-not-filtered early-return branch
    (lines 163–164) in `requestFilter` by invoking `GET /api/test-filter/ping`, a real
    JAX-RS resource registered only in the test classpath.
  - `TestNotFilteredResource` *(test-only JAX-RS resource)* - `GET /api/test-filter/ping`
    returns `"pong"` (200). Its path does not start with `/api/rest` so the request filter
    exits early at line 164 without processing the trace.
  - `TestRestMultiHeaderResource` *(test-only JAX-RS resource)* - `GET /api/rest/test-multi-header`
    returns a response containing `X-Test-Multi: value1` and `X-Test-Multi: value2`. The
    response filter processes the multi-value header via `getResponseHeaders`, exercising the
    `sb.append(", ")` join at line 386.
- Updated `EchoResourceEndPointTest`: added `testEchoReactiveSuccess`,
  `testEchoReactiveValidation`, `testEchoReactiveBodyEmpty` (reactive endpoint) and
  `testResponseHeaderMultiValue` (multi-value response header). Total tests in the class: 8.
- Updated `CookieTrackingCodeTest`: added `testCookieNotReplacedWhenAlreadyPresent` to cover
  the `trackingCookie != null` branch in `setCookieUserTracing` (cookie already present in
  the incoming request → filter must not override it with a new `Set-Cookie`).

## [1.4.0] - 2026-04-14

### Added
- **`TraceEventDispatcher`**: new `@ApplicationScoped` bean implementing the
  *capture-only + external dispatcher* pattern. Features a dedicated OS-scheduled
  daemon thread (`trace-event-dispatcher`) that drains event queues in burst mode,
  bounded `ArrayBlockingQueue` queues (capacity 5,000) with drop-on-full backpressure,
  `AtomicLong` dropped-event counters, and graceful flush on `@PreDestroy`.
- **`@ServerRequestFilter` / `@ServerResponseFilter`** annotation-based filter replacing
  the old `ContainerRequestFilter` / `ContainerResponseFilter` interfaces - enables
  direct injection of `RoutingContext`, `UriInfo` and other Vert.x/RESTEasy Reactive
  parameters without `@Provider` registration.
- **SPDX license headers** on all 31 Java source files (main + test) via
  `license-maven-plugin` 4.6 (Mycila). Template at `src/license/header.txt`:
  `SPDX-FileCopyrightText` + `SPDX-License-Identifier: MIT`.
- **`formatter-maven-plugin` 2.24.1** with `quarkus-ide-config` dependency: Eclipse
  Quarkus-style Java formatter bound to `process-sources`. Skip with `-Dformat.skip=true`.
  Verify without modifying: `./mvnw formatter:validate impsort:check`.
- **`impsort-maven-plugin` 1.12.0** for import ordering (Quarkus group order:
  `java. → javax. → jakarta. → org. → com.`) bound to `process-sources`.
- **`license-maven-plugin` 4.6** bound to `process-sources`. Verify: `./mvnw license:check`.
  Skip with `-Dlicense.skip=true`.
- New configuration properties:
  - `app.filter.dispatcher.interval.ms` (integer, default `20`) - idle sleep of the drain
    thread when both queues are empty.
  - `app.filter.dispatcher.queue.capacity` (default `5000`) - maximum capacity of each
    bounded queue; events are dropped (with WARN log) when full.
- New Maven properties: `format.skip`, `license.skip`, `formatter-maven-plugin.version`,
  `impsort-maven-plugin.version`, `license-maven-plugin.version`.
- `<inceptionYear>2024</inceptionYear>` added to `pom.xml`.
- README: *High-Performance Tracing Architecture* section with Mermaid flowchart diagram,
  component table, configuration properties table, and filter execution order table.
- README: *Code Quality & Development Tools* section documenting formatter and license commands.
- README: *Performance Comparison: v1.3.0 vs v1.4.0* section with real benchmark data
  (JMeter/Taurus on Red Hat Developer Sandbox - OpenShift 4.21.7 / k8s v1.34.5, 1 Pod,
  15,000 total requests): throughput +10–15%, avg latency −11–14%, p95 −17–22%, zero errors.

### Changed
- `TraceJaxRsRequestResponseFilter` rewritten with **capture-only pattern**: the filter now
  only reads the body and enqueues lightweight `RequestTrace` / `ResponseTrace` records (pure
  strings/primitives) in O(1). All JSON construction, serialization and `EventBus.publish()`
  calls are delegated to `TraceEventDispatcher` - zero impact on the HTTP lifecycle.
- `@Scheduled` drain replaced by a **dedicated daemon thread**
  (`Thread.ofPlatform().daemon(true).start(this::drainLoop)`) to eliminate worker thread pool
  starvation under high HTTP load. The thread drains continuously in burst mode when queues
  contain items, and sleeps for `app.filter.dispatcher.interval.ms` ms when idle.
- Unbounded `ConcurrentLinkedQueue` replaced by bounded **`ArrayBlockingQueue`** (capacity
  5,000): eliminates OOM risk and `SRMSG00034: Insufficient downstream requests to emit item`
  reactive streams backpressure errors.
- `quarkus.jacoco.excludes` prefixed with `%test.` to avoid *"Unrecognized configuration key"*
  warning in dev/prod profiles where `quarkus-jacoco` (test-scoped) is not on the classpath.
- `app.filter.dispatcher.interval` renamed to `app.filter.dispatcher.interval.ms` (integer in
  ms; the old ISO-8601 / `@Scheduled`-style string is no longer used).
- Queue capacity increased from 1,000 to 5,000.
- Upgraded Quarkus platform to **3.34.3**.
- Updated README: Quarkus version reference, extensions list (added `quarkus-hibernate-orm-panache`,
  `quarkus-micrometer-registry-jmx`), application log examples, and guide links.
- SPDX license header template migrated from custom plugin `<properties>` to direct
  `${project.*}` expressions so it resolves correctly in all Maven invocation contexts
  (including `quarkus:dev` embedded Maven).

### Removed
- `quarkus-scheduler` dependency - no longer needed after replacing `@Scheduled` with the
  dedicated daemon thread.
- Direct `EventBus.publish()` calls from JAX-RS filter methods - publishing is now exclusively
  handled by `TraceEventDispatcher`.

### Fixed
- **Scheduler starvation**: `@Scheduled` drain shared the Quarkus worker thread pool with HTTP
  request handlers; under heavy load the drain received no CPU time, causing queues to grow
  across benchmark runs and events to be dropped or delayed.
- **`SRMSG00034`**: unbounded queue accumulation under high load caused SmallRye Reactive
  Messaging to throw *"Insufficient downstream requests to emit item"* - resolved by the
  bounded `ArrayBlockingQueue` with drop-on-full policy.
- **"Unrecognized configuration key" warning** for `quarkus.jacoco.excludes` in non-test
  profiles - resolved by adding the `%test.` profile prefix.
- **SPDX `${url}` not expanded** in license header - `url` is a reserved Maven POM element
  name that prevented the custom plugin property from being injected; fixed by using
  `${project.organization.url}` directly in the template.

## [1.3.0] - 2025-06-03
### Changed
- Upgrade to Quarkus 3.23.0. See the migration guide at https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.23 (PR #8)
- Translate the README.md file to English (PR #10)

### Fixed
- Unable to resolve download URL for '22-ea' (PR #9) on the GitHub Actions workflow
- Replace deprecated `quarkus.hibernate-orm.database.generation` with `quarkus.hibernate-orm.schema-management.strategy`

## [1.2.13] - 2025-03-25
### Changed
- Upgrade to Quarkus 3.19.4. See the migration guide at https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.19
- Upgrade io.quarkiverse.micrometer.registry:quarkus-micrometer-registry-jmx to 3.3.1
- Update the README.md

## [1.2.12] - 2024-12-17
### Changed
- Upgrade to Quarkus 3.17.4
- Update the CONTRIBUTING.md file with the Quarkus contribution guide

### Security
- Update the image base (for Dockerfile.jvm and Dockerfile.legacy-jar) to registry.access.redhat.com/ubi9/openjdk-21:1.21-3.1733995526
- Update the image base (for Dockerfile.native) to registry.access.redhat.com/ubi9/ubi-minimal:9.5-1733767867

## [1.2.11]
### Fixed
- Fixed FROM URI container image in Dockerfile
- 
## [1.2.10]
### Changed
- Upgrade to Quarkus 3.16.4
- Upgrade Container base image to registry.redhat.io/ubi9/openjdk-21:1.21-3

## [1.2.9]
### Changed
- Upgrade to Quarkus 3.16.2

## [1.2.8] - 2024-06-09
### Changed
- Finest tuning for the JMX configuration

## [1.2.7] - 2024-06-07
### Fixed
- Fixed name of the secret name and base64 encoding
- Changed the PostgreSQL image to RedHat version.

### Changed
- Updated the README.md file with the instructions to run the application with PostgreSQL

## [1.2.6] - 2024-05-19
### Added
- Aggiunta nota sul file README.md riguardo l'uso di Podman rispetto a Docker
- Aggiunta nuova operation `createHorses()` per inserire più Horse in una sola chiamata
- Aggiunta nuova operation `countHorses()` per contare il numero di Horse
- Aggiunta nuova operation `deleteHorses()` per eliminare tutti gli Horse
- Aggiunti nuovi test per le nuove operazioni

### Changed
- Aggiornamento operation `getAllHorses()` per accettare il parametro `limit` con l'obiettivo di limitare il numero di Horse restituiti
- Aggiornamento scenario di test JMeter `scenario_2.jmx` per includere le nuove operazioni
- Uso di `Instant.now()` per la generazione del tracking invece di `LocalDateTime.now()`

### Fixed
- Removed unused import in `HorseRepositoryResources`
- Rivista `@Order` annotation per i test

## [1.2.5] - 2024-05-14
### Changed
- Aggiornamento del file README.md sezione Scenari di Test con JMeter e Taurus
- Aggiornamento della gestione degli Event Handler attraverso la configurazione di Quarkus
- Adeguamento file JMX `scenario_2.jmx`

### Fixed
- Risoluzione issue di SonarCloud

## [1.2.4] - 2024-05-13
### Added
- Aggiunto altro unit test per la pubblicazione di messaggi fake
- Aggiunta configurazione Jacoco per l'exclusion di una classe dal coverage report
- Aggiunta sulla configurazione di Quarkus le nuove secret per il database PostgreSQL
- Aggiunte le configurazioni per il database PostgreSQL per il profile di produzione (anche se non abilitate)
- Aggiunta environment JAVA_OPTS per la configurazione della JVM sul docker-compose.yml
- Aggiunta env JAVA_OPTS su docker-compose.yml Sono state aggiunte opzioni di ottimizzazione riguardo il GC e in particolare le opzioni per la JMX utili al fine di monitoraggio delle performance
- Aggiunto il capitolo "Monitoraggio delle performance dell'applicazione Quarkus" al file README.md

### Changed
- Aggiornamento README.md con la nota circa l'importanza di avere un container runtime correttamente configurato
- Logging application for prod profile
- Reviewed quarkus.thread-pool.max-threads value

### Fixed
- Risoluzione issue di SonarCloud

### Removed

### Deprecated

### Security

## [1.2.3] - 2024-05-12

### Added
- Aggiunta configurazione datasource di default per il database H2
- Aggiunta configurazione datasource per PostgreSQL profilo di produzione
- Aggiunta configurazione per i secrets K8s per la connessione al database PostgreSQL
- Aggiunte le dipeendenze Quarkus per PostgreSQL

### Changed
- Aggiornamento docker-compose.yml per aggiungere il servizio PostgreSQL
- Aggiornamento file README.md con le istruzioni per l'esecuzione dell'applicazione con PostgreSQL

### Fixed
- Fix docker-compose.yml per l'esecuzione dell'applicazione con PostgreSQL nel caso in cui il container di PostgreSQL sia eseguito su host con sistema operativo Linux

## [1.2.2] - 2024-05-11

### Changed
- Aggiunta gestione del metodo CRUD di Update per la classe di entità `Horse` e `Owner`
- Aggiunto lo scenario di test JMeter per il nuovi servizi JAX-RS introdotti con le entità `Horse` e `Owner`
- Aggiornamento del file README.md con le istruzioni per l'esecuzione dello scenario di test JMeter

### Fixed
- Eliminazione di alcuni listener jp@gc - JMeter Plugins per il test di carico con JMeter e Taurus

## [1.2.1] - 2024-05-10

### Changed
- Aggiornamento del modello ORM Panache per l'aggiunta di nuovi attributi nella classe di entità `Horse`
- Aggiornamento dello script SQL per l'inizializzazione del database H2
- Aggiornamento dei unit e integration test per l'aggiunta di nuovi attributi nella classe di entità `Horse`

## [1.2.0] - 2024-05-10

### Added
- Aggiornamento versione Quarkus a 3.10.0
- Aggiunti i moduli Quarkus:
  - io.quarkus:quarkus-hibernate-orm-panache
  - io.quarkus:quarkus-jdbc-h2
- Aggiunte le classi di entità:
  - it.dontesta.eventbus.orm.panache.entity.Horse
  - it.dontesta.eventbus.orm.panache.entity.Owner
- Aggiunte le classi di repository:
  - it.dontesta.eventbus.orm.panache.repository.HorseRepository
  - it.dontesta.eventbus.orm.panache.repository.OwnerRepository
- Aggiunte le classi di risorse (JAX-RS)
  - it.dontesta.eventbus.ws.resources.endpoint.repository.v1.HorseRepositoryResources
  - it.dontesta.eventbus.ws.resources.endpoint.repository.v1.OwnerRepositoryResources
- Aggiunta la classe di Error Mapping (JAX-RS)
  - it.dontesta.eventbus.ws.mappers.ErrorMapper
- Aggiunto il file `src/main/resources/load_data.sql` per l'inizializzazione del database H2
- Aggiunte le classi di test:
  - it.dontesta.eventbus.orm.panache.repository.HorseRepositoryIntegrationTest
  - it.dontesta.eventbus.orm.panache.repository.OwnerRepositoryIntegrationTest
  - it.dontesta.eventbus.orm.panache.repository.PanacheRepositoryMockedTest
  - it.dontesta.eventbus.ws.resources.endpoint.repository.v1.HorseRepositoryResourcesTest
  - it.dontesta.eventbus.ws.resources.endpoint.repository.v1.OwnerRepositoryResourcesTest
  
### Changed
- Revisione del file di configurazione per l'uso di Hibernate ORM Panache

### Fixed
- Null Pointer Exception sul filtro JAX-RS per il tracking delle richieste quando il body della richiesta è vuoto
- Google Checkstyle violations

### Removed

### Deprecated

### Security

## [1.1.0] - 2024-05-05

### Added
- Aggiunta figura 2 sul file README.md che mostra la Dev UI di Quarkus.
- Aggiunto scenario di Load Testing con JMeter attraverso Taurus

### Changed
- Revisione dei parametri di configurazione della JVM.
- Revisione del file README.md con aggiunta del capitolo "Load Testing con JMeter e Taurus".


## [1.0.1] - 2024-04-29

### Added

- This CHANGELOG file to hopefully serve as an evolving example of a
  standardized open source project CHANGELOG.
- Guidelines for contributing to this repository.
- GitHub Actions for Continuous Integration (CI) and Continuous Deployment (CD).
- Aggiornamento del file README.md con le istruzioni per l'esecuzione dell'applicazione
  in modalità sviluppo e creazione di un eseguibile nativo.
- Aggiornamento del file CODE_OF_CONDUCT.md con le linee guida per il comportamento
  all'interno del progetto.
- Aggiornamento del file CONTRIBUTING.md con le linee guida per contribuire al progetto.
- Abilitazione compressione su protocollo HTTP/1.1
- Abilitazione protocollo SSL/TLS
- Abilitazione Router OpenShift in modalità TLS/Passthrough
- Aggiunto il docker-compose.yml per l'esecuzione dell'applicazione in modalità sviluppo
- Aggiornamento base image a registry.access.redhat.com/ubi9/openjdk-21:1.18-3
- Aggiornamento configurazione Quarkus per tuning parametri JVM
