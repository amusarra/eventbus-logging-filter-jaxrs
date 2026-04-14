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

/**
 * Risorsa JAX-RS <b>solo per i test</b> usata per esercitare il ramo
 * "URI non filtrata" nel filtro
 * {@link it.dontesta.eventbus.ws.filter.TraceJaxRsRequestResponseFilter}.
 *
 * <p>
 * Il path {@code /api/test-filter/ping} NON inizia con {@code /api/rest},
 * quindi quando il filtro JAX-RS viene invocato su questo endpoint (risorsa
 * correttamente registrata, non un 404) la condizione a linea 163
 * {@code if (!requestUriIsFiltered(requestUri))} è {@code true} e viene
 * eseguito il {@code return} a linea 164 (early-return).
 */
@Path("test-filter")
@ApplicationScoped
@Produces(MediaType.TEXT_PLAIN)
public class TestNotFilteredResource {

    /**
     * Endpoint di test raggiungibile a {@code GET /api/test-filter/ping}.
     *
     * @return la stringa {@code "pong"}
     */
    @GET
    @Path("ping")
    public String ping() {
        return "pong";
    }
}
