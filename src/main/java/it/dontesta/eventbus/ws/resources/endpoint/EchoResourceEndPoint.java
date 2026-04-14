/*
 * SPDX-FileCopyrightText: 2024 Antonio Musarra's Blog <https://www.dontesta.it>
 * SPDX-License-Identifier: MIT
 */
package it.dontesta.eventbus.ws.resources.endpoint;

import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;

/**
 * This class represents the REST endpoint for the Echo resource.
 */
@Path("rest")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EchoResourceEndPoint {

    /**
     * This method echoes the input parameter.
     *
     * @param input the input parameter
     *
     * @return the response
     */
    @Path("echo")
    @POST
    public Response echo(
            @Size(min = 32, max = 4096, message = "Il parametro di input deve essere compreso tra 32 byte e 4 KB") String input) {
        return Response.ok(input).build();
    }

    /**
     * Versione reattiva dell'endpoint echo. Restituisce {@link Uni}{@code <Response>}, quindi
     * viene eseguita in modalità <b>non-blocking</b> sull'I/O event loop thread di Vert.x.
     *
     * <p>
     * Utile per verificare il comportamento del filtro
     * {@link it.dontesta.eventbus.ws.filter.TraceJaxRsRequestResponseFilter} con endpoint
     * non-blocking: in questo caso il filtro di richiesta gira anch'esso sull'event loop e
     * il body deve essere disponibile tramite il fast-path {@code RoutingContext#body()}.
     *
     * @param input il parametro di input da restituire come echo
     * @return {@code Uni<Response>} con il corpo della risposta uguale all'input
     */
    @Path("echo/reactive")
    @POST
    public Uni<Response> echoReactive(
            @Size(min = 32, max = 4096, message = "Il parametro di input deve essere compreso tra 32 byte e 4 KB") String input) {
        return Uni.createFrom().item(() -> Response.ok(input).build());
    }
}
