# Quarkus Event Bus Logging Filter JAX-RS

[![Keep a Changelog v1.1.0 badge](https://img.shields.io/badge/changelog-Keep%20a%20Changelog%20v1.1.0-%23E05735)](CHANGELOG.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![code of conduct](https://img.shields.io/badge/Conduct-Contributor%20Covenant%202.1-purple.svg)](CODE_OF_CONDUCT.md)

![Build with Maven](https://github.com/amusarra/eventbus-logging-filter-jaxrs/actions/workflows/build_via_maven.yml/badge.svg) 
![CI Docker build](https://github.com/amusarra/eventbus-logging-filter-jaxrs/actions/workflows/docker_publish.yml/badge.svg) 
![CI Docker build native amd64](https://github.com/amusarra/eventbus-logging-filter-jaxrs/actions/workflows/docker_publish_native_amd64.yml/badge.svg)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=amusarra_eventbus-logging-filter-jaxrs&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=amusarra_eventbus-logging-filter-jaxrs)

[![Docker Image Version (tag)](https://img.shields.io/docker/v/amusarra/eventbus-logging-filter-jaxrs?label=Docker%20Hub%20Image%20)](https://hub.docker.com/r/amusarra/eventbus-logging-filter-jaxrs)



Questo progetto è un'applicazione Quarkus che mostra come realizzare un sistema che sia capace di
tracciare le richieste JAX-RS in arrivo e in uscita dall'applicazione su diversi canali di
storage, come ad esempio un database MongoDB, SQL o un broker AMQP sfruttando l'Event Bus 
di Quarkus.

[![Flusso Applicazione Quarkus](src/doc/resources/images/flusso_applicazione_quarkus.jpeg)](src/doc/resources/images/flusso_applicazione_quarkus.jpeg)

**Figura 1**: Flusso dell'applicazione Quarkus

Se vuoi saperne di più su Quarkus, visita il sito ufficiale [quarkus.io](https://quarkus.io/).
A seguire trovi le istruzioni per eseguire l'applicazione in modalità sviluppo e creare un eseguibile
nativo.

## Requisiti
La tabella seguente elenca i requisiti necessari per l'implementazione ed esecuzione del progetto 
Quarkus.

| Nome                     | Opzionale | Descrizione                                                  |
| ------------------------ | --------- | ------------------------------------------------------------ |
| Java JDK 17/21           | NO        | Implementazione di OpenJDK 17/21. È possibile usare qualunque delle [implementazioni disponibili](https://en.wikipedia.org/wiki/OpenJDK). Per questo articolo è stata usata la versione 21 di OpenJDK e l'implementazione di Amazon Corretto 21.0.2. |
| Git                      | NO        | Tool di versioning.                                          |
| Maven 3.9.6              | NO        | Tool di build per i progetti Java e di conseguenza Quarkus.  |
| Quarkus 3.9.2            | NO        | Framework Quarkus 3.9.2 la cui release note è disponibile qui https://quarkus.io/blog/quarkus-3-9-2-released/. Per maggiori informazioni per le release LTS fare riferimento all'articolo [Long-Term Support (LTS) for Quarkus](https://quarkus.io/blog/lts-releases/). |
| Quarkus CLI              | SI        | Tool a linea di comando che consente di creare progetti, gestire estensioni ed eseguire attività essenziali di creazione e sviluppo. Per ulteriori informazioni su come installare e utilizzare la CLI (Command Line Interface) di Quarkus, consulta la [guida della CLI di Quarkus](https://quarkus.io/guides/cli-tooling). |
| Docker v26 o Podman v4/5 | NO        | Tool per la gestione delle immagini e l'esecuzione dell'applicazione in modalità container. La gestione delle immagini/container sarà necessaria nel momento in cui saranno sviluppati gli Event Handler che dovranno comunicare con i servizi esterni all'applicazione (vedi NoSQL, SQL, AMQP). La gestione delle immagini necessarie e container, sarà totalmente trasparente per noi sviluppatori in quanto a carico dei [Dev Services di Quarkus](https://quarkus.io/guides/dev-services). |
| GraalVM                  | SI        | Per la build dell'applicazione in modalità nativa. Per maggiori informazioni fare riferimento alla documentazione [Building a Native Executable](https://quarkus.io/guides/building-native-image). |
| Ambiente di sviluppo C   | SI        | Richiesto da GraalVM per la build dell'applicazione nativa. Per maggiori informazioni fare riferimento alla documentazione [Building a Native Executable](https://quarkus.io/guides/building-native-image). |
| cURL 7.x/8.x             | SI        | Tool per il test dei Resource Endpoint (servizi REST)        |

**Tabella 1** - Requisiti (anche opzionali) necessari per l'implementazione del progetto Quarkus

Le estensioni Quarkus utilizzate per l'implementazione del progetto sono le seguenti:
- io.quarkus:quarkus-hibernate-validator ✔
- io.quarkus:quarkus-mongodb-client ✔
- io.quarkus:quarkus-openshift ✔
- io.quarkus:quarkus-smallrye-health ✔
- io.quarkus:quarkus-vertx ✔
- io.quarkus:quarkus-messaging-amqp ✔
- io.quarkus:quarkus-arc ✔
- io.quarkus:quarkus-rest ✔
- io.quarkus:quarkus-rest-jackson ✔

## Esecuzione dell'applicazione in Docker
Vorresti eseguire l'applicazione in un container e testare il funzionamento dell'applicazione fin da subito?
All'interno del progetto è disponibile il file `src/main/docker/docker-compose.yml` che ti permette di eseguire 
l'applicazione in un container utilizzando Docker Compose o Podman Compose.

Ecco come fare utilizzando [Podman Compose](https://docs.podman.io/en/latest/markdown/podman-compose.1.html) (non cambia nel caso di [Docker Compose](https://docs.docker.com/compose/)):

```shell script
# Tramite Podman Compose (alternativa a Docker Compose)
podman-compose -f src/main/docker/docker-compose.yml up -d
```
Console 1 - Esegui l'applicazione Quarkus in un container (compresi i servizi di supporto come MongoDB, AMQP, ecc.)

Tramite il comando `docker-compose` o `podman-compose` verranno avviati i seguenti servizi:
- MongoDB
- AMQP (Apache ActiveMQ Artemis)
- Applicazione Quarkus

L'immagine dell'applicazione Quarkus è disponibile su [Docker Hub](https://hub.docker.com/r/amusarra/eventbus-logging-filter-jaxrs)
e questa è pubblicata grazie alla GitHub Actions `.github/workflows/docker_publish.yml` e 
`.github/workflows/docker_publish_native_amd64.yml` (per build e pubblicazione dell'immagine nativa x86-64).
 
Dopo aver eseguito il comando, puoi verificare che i servizi siano attivi tramite il comando `docker ps` o `podman ps`.

```shell script
# Verifica i container attivi
podman ps
```
Console 2 - Verifica i container attivi

L'output del comando `podman ps` dovrebbe essere simile al seguente:

```shell script
CONTAINER ID  IMAGE                                                    COMMAND               CREATED         STATUS                   PORTS                                                                                             NAMES
d021a5f570bc  docker.io/library/mongo:4.4                              mongod                40 seconds ago  Up 40 seconds            0.0.0.0:27017->27017/tcp                                                                          mongodb
4faee4a45565  quay.io/artemiscloud/activemq-artemis-broker:1.0.25      /opt/amq/bin/laun...  39 seconds ago  Up 39 seconds            0.0.0.0:5445->5445/tcp, 0.0.0.0:5672->5672/tcp, 0.0.0.0:8161->8161/tcp, 0.0.0.0:61616->61616/tcp  artemis
7a3d8e2e709e  docker.io/amusarra/eventbus-logging-filter-jaxrs:latest                        38 seconds ago  Up 38 seconds (healthy)  0.0.0.0:8080->8080/tcp, 0.0.0.0:8443->8443/tcp                                                    logging-filter
```
Console 3 - Output del comando `podman ps`

Il servizio `logging-filter` è l'applicazione Quarkus che è stata avviata in un container e che è pronta per essere 
utilizzata per testare i servizi REST esposti (fai attenzione che il servizio sia in stato `healthy`).

Sul docker-compose, per il servizio `logging-filter`, è stato abilitato il servizio di Health Check di Quarkus che
verifica la salute dell'applicazione. Il servizio di Health Check è disponibile all'indirizzo
http://localhost:8080/q/health.

```yaml
  # The environment variables are used to configure the connection to the
  # Artemis message broker and the MongoDB database.
  # Se the application.properties file for more details about the names of the
  # environment variables.
  logging-filter:
    image: docker.io/amusarra/eventbus-logging-filter-jaxrs:latest
    container_name: logging-filter
    networks:
        - logging_filter_network
    environment:
        - AMQP_HOSTNAME=artemis
        - AMQP_PORT=5672
        - AMQP_USERNAME=artemis
        - AMQP_PASSWORD=artemis
        - MONGODB_CONNECTION_URL=mongodb://mongodb:27017/audit
    ports:
      - "8080:8080"
      - "8443:8443"
    healthcheck:
      test: [ "CMD-SHELL", "curl --fail http://localhost:8080/q/health" ]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    depends_on:
      - artemis
      - mongodb
```
Console 4 - Estratto del docker-compose.yml per il servizio `logging-filter`

Per testare il servizio REST esposto dall'applicazione Quarkus, puoi utilizzare il comando `curl` per inviare una
richiesta HTTP al servizio REST.

```shell script
# Invia una richiesta HTTP al servizio REST
curl -v --http2 \
  -H "Content-Type: application/json" \
  -d '{"message": "Test di tracking richiesta JAX-RS"}' \
  http://localhost:8080/api/rest/echo
  
# Risposta attesa
*   Trying [::1]:8080...
* Connected to localhost (::1) port 8080
> POST /api/rest/echo HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.4.0
> Accept: */*
> Connection: Upgrade, HTTP2-Settings
> Upgrade: h2c
> HTTP2-Settings: AAMAAABkAAQAoAAAAAIAAAAA
> Content-Type: application/json
> Content-Length: 48
>
< HTTP/1.1 101 Switching Protocols
< connection: upgrade
< upgrade: h2c
* Received 101, Switching to HTTP/2
* Copied HTTP/2 data in stream buffer to connection buffer after upgrade: len=15
< HTTP/2 200
< content-type: application/json;charset=UTF-8
< content-length: 48
< set-cookie: user_tracking_id=f7cf38cd-a16d-4d53-91d3-5a3854f93e94;Version=1;Comment="Cookie di tracciamento dell'utente";Path=/;Max-Age=2592000;HttpOnly
< x-correlation-id: f5f0b80e-f550-45f4-9a75-41d33c6376f3
< x-pod-name: 7a3d8e2e709e
<
* Connection #0 to host localhost left intact
{"message": "Test di tracking richiesta JAX-RS"}%
```
Console 5 - Esempio di richiesta HTTP al servizio REST

Utilizzando il comando `podman logs <container-id>`, puoi verificare i log dell'applicazione Quarkus dove sono presenti
le informazioni relative al tracciamento delle richieste JAX-RS. A seguire un esempio di output dei log dell'applicazione.

```shell script
2024-04-23 11:57:43,965 DEBUG [it.don.eve.ws.fil.TraceJaxRsRequestResponseFilter] (executor-thread-35) La Request URI /api/rest/echo è tra quelle che devono essere filtrate
2024-04-23 11:57:43,966 DEBUG [it.don.eve.ws.fil.TraceJaxRsRequestResponseFilter] (executor-thread-35) Pubblicazione del messaggio della richiesta HTTP su Event Bus
2024-04-23 11:57:43,967 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Received event message from source virtual address: http-request and source component: it.dontesta.eventbus.consumers.http.HttpRequestConsumer for the target virtual addresses: sql-trace,nosql-trace,queue-trace
2024-04-23 11:57:43,968 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Sending event message to target virtual address: sql-trace
2024-04-23 11:57:43,968 ERROR [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Failed to receive response from target virtual address: sql-trace with failure: (NO_HANDLERS,-1) No handlers for address sql-trace
2024-04-23 11:57:43,968 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Sending event message to target virtual address: nosql-trace
2024-04-23 11:57:43,969 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Sending event message to target virtual address: queue-trace
2024-04-23 11:57:43,967 DEBUG [it.don.eve.ws.fil.TraceJaxRsRequestResponseFilter] (executor-thread-35) La Request URI /api/rest/echo è tra quelle che devono essere filtrate
2024-04-23 11:57:43,970 DEBUG [it.don.eve.ws.fil.TraceJaxRsRequestResponseFilter] (executor-thread-35) Pubblicazione del messaggio della risposta HTTP su Event Bus
2024-04-23 11:57:43,973 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Received event message from source virtual address: http-response and source component: it.dontesta.eventbus.consumers.http.HttpResponseConsumer for the target virtual addresses: sql-trace,nosql-trace,queue-trace
2024-04-23 11:57:43,976 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Sending event message to target virtual address: sql-trace
2024-04-23 11:57:43,977 ERROR [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Failed to receive response from target virtual address: sql-trace with failure: (NO_HANDLERS,-1) No handlers for address sql-trace
2024-04-23 11:57:43,977 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Sending event message to target virtual address: nosql-trace
2024-04-23 11:57:43,977 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Sending event message to target virtual address: queue-trace
2024-04-23 11:57:43,982 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Received response from target virtual address: nosql-trace with result: Documents inserted successfully with Id BsonObjectId{value=6627a2378d1ed50d0c256e95}
2024-04-23 11:57:43,982 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Received response from target virtual address: nosql-trace with result: Documents inserted successfully with Id BsonObjectId{value=6627a2378d1ed50d0c256e96}
2024-04-23 11:57:43,984 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Received response from target virtual address: queue-trace with result: Message sent to AMQP queue successfully!
2024-04-23 11:57:43,984 DEBUG [it.don.eve.con.eve.han.Dispatcher] (vert.x-eventloop-thread-0) Received response from target virtual address: queue-trace with result: Message sent to AMQP queue successfully!
```
Log 1 - Esempio di log dell'applicazione Quarkus

A questo punto puoi eseguire lo shutdown dell'applicazione Quarkus e dei servizi di supporto utilizzando il comando
`podman-compose -f src/main/docker/docker-compose.yml down` o il relativo docker-compose.

Su asciinema.org è disponibile un video che mostra come eseguire l'applicazione Quarkus in un container utilizzando 
Podman.

[![asciicast](https://asciinema.org/a/655929.svg)](https://asciinema.org/a/655929)

Potresti anche essere curioso a fare un semplice benchmark dell'applicazione Quarkus in un container. Per fare ciò,
puoi utilizzare il comando [h2load](https://github.com/nghttp2/nghttp2?tab=readme-ov-file#benchmarking-tool) che è un benchmarking tool per HTTP/2 e HTTP/1.1. Ecco un esempio di come fare.

```shell script
# Esegui il benchmark dell'applicazione Quarkus utilizzando il protocollo HTTP/2
# -n 100 indica il numero di richieste da inviare
# -c 5 indica il numero di connessioni da mantenere aperte
# -H "Content-Type: application/json" indica l'header Content-Type
# -H 'Accept-Encoding: gzip, deflate, br, zstd' indica l'header Accept-Encoding
# -d src/test/resources/payload-1.json indica il payload da inviare
h2load -n 100 -c 5 \
        -H "Content-Type: application/json" \
        -H 'Accept-Encoding: gzip, deflate, br, zstd' \
        -d src/test/resources/payload-1.json \
        https://0.0.0.0:8443/api/rest/echo
```
Console 6 - Esempio di benchmark dell'applicazione Quarkus

A seguire un esempio di output del comando h2load.

```shell script
starting benchmark...
spawning thread #0: 5 total client(s). 100 total requests
TLS Protocol: TLSv1.3
Cipher: TLS_AES_256_GCM_SHA384
Server Temp Key: X25519 253 bits
Application protocol: h2
progress: 10% done
progress: 20% done
progress: 30% done
progress: 40% done
progress: 50% done
progress: 60% done
progress: 70% done
progress: 80% done
progress: 90% done
progress: 100% done

finished in 315.73ms, 316.73 req/s, 97.43KB/s
requests: 100 total, 100 started, 100 done, 100 succeeded, 0 failed, 0 errored, 0 timeout
status codes: 100 2xx, 0 3xx, 0 4xx, 0 5xx
traffic: 30.76KB (31500) total, 18.83KB (19280) headers (space savings 34.86%), 9.57KB (9800) data
                     min         max         mean         sd        +/- sd
time for request:     2.57ms     42.56ms     10.97ms      6.84ms    72.00%
time for connect:    20.96ms     73.93ms     42.60ms     19.80ms    60.00%
time to 1st byte:    49.82ms     82.68ms     65.11ms     16.23ms    60.00%
req/s           :      63.44      101.74       79.87       19.92    60.00%
```
Log 2 - Esempio di output del comando h2load

## Esecuzione dell'applicazione in dev mode

Puoi eseguire l'applicazione in modalità sviluppo che abilita il live coding utilizzando:
```shell script
./mvnw compile quarkus:dev
```
Console 7 - Esecuzione dell'applicazione in modalità sviluppo

> **_NOTE:_**  Quarkus ora include una UI di sviluppo, disponibile solo in modalità sviluppo all'indirizzo http://localhost:8080/q/dev/.

## Packaging e avvio dell'applicazione

L'applicazione può essere impacchettata utilizzando:
```shell script
./mvnw package
```
Console 8 - Impacchettamento dell'applicazione

Il processo produrrà il file `quarkus-run.jar` in `target/quarkus-app/`.
Questo non è un _über-jar_ in quanto le dipendenze sono copiate nella 
directory `target/quarkus-app/lib/`.

L'applicazione è ora eseguibile utilizzando `java -jar target/quarkus-app/quarkus-run.jar`.

Se vuoi creare un _über-jar_, esegui il seguente comando:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```
Console 9 - Impacchettamento dell'applicazione come _über-jar_

L'applicazione, impacchettata come un _über-jar_, è ora eseguibile utilizzando `java -jar target/*-runner.jar`.

## Creazione di un eseguibile nativo

Puoi creare un eseguibile nativo utilizzando: 
```shell script
./mvnw package -Dnative
```
Console 10 - Creazione di un eseguibile nativo

Nel caso in cui tu non avessi GraalVM installato, puoi eseguire la build dell'eseguibile nativo in un container 
utilizzando:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```
Console 11 - Creazione di un eseguibile nativo in un container

Puoi eseguire l'eseguibile nativo con: `./target/eventbus-logging-filter-jaxrs-1.0.0-SNAPSHOT-runner`

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


[![alt tag](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/project/overview?id=amusarra_eventbus-logging-filter-jaxrs)

This project is using SonarCloud for code quality.
Thanks to SonarQube Team for free analysis solution for open source projects.