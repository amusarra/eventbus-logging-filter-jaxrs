/*
 * SPDX-FileCopyrightText: 2024 Antonio Musarra's Blog <https://www.dontesta.it>
 * SPDX-License-Identifier: MIT
 */
package it.dontesta.eventbus.ws.resources.endpoint;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Risorsa JAX-RS <b>solo per i test</b> usata per esercitare il ramo
 * "header di risposta multi-valore" nel metodo
 * {@link it.dontesta.eventbus.ws.filter.TraceJaxRsRequestResponseFilter#getResponseHeaders}.
 *
 * <p>
 * Il path {@code /api/rest/test-multi-header} inizia con {@code /api/rest}, quindi
 * viene filtrato normalmente dal {@code responseFilter}. La risposta include l'header
 * {@code X-Test-Multi} con due valori distinti; quando {@code getResponseHeaders}
 * itera su quell'header, la condizione {@code if (!sb.isEmpty())} a linea 385 è
 * {@code true} e viene eseguito {@code sb.append(", ")} a linea 386.
 */
@Path("rest")
@ApplicationScoped
@Produces(MediaType.TEXT_PLAIN)
public class TestRestMultiHeaderResource {

    /**
     * Endpoint di test raggiungibile a {@code GET /api/rest/test-multi-header}.
     * Restituisce due valori per lo stesso header {@code X-Test-Multi} in modo da
     * esercitare il join con separatore "{@code , }" in
     * {@code TraceJaxRsRequestResponseFilter#getResponseHeaders} (linea 386).
     *
     * @return risposta 200 con {@code X-Test-Multi: value1} e {@code X-Test-Multi: value2}
     */
    @GET
    @Path("test-multi-header")
    public Response multiHeader() {
        return Response.ok("pong")
                .header("X-Test-Multi", "value1")
                .header("X-Test-Multi", "value2")
                .build();
    }
}
