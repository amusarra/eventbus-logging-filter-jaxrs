# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Aggiunta figura 2 sul file README.md che mostra la Dev UI di Quarkus.
- Aggiunto scenario di Load Testing con JMeter attraverso Taurus

### Changed
- Revisione dei parametri di configurazione della JVM.
- Revisione del file README.md con aggiunta del capitolo "Load Testing con JMeter e Taurus".

### Removed

### Deprecated

### Security

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
