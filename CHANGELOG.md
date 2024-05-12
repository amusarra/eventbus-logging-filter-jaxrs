# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
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
