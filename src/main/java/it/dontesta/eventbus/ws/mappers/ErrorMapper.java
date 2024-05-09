package it.dontesta.eventbus.ws.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class ErrorMapper implements ExceptionMapper<Throwable> {

  @Inject
  Logger log;

  @Inject
  ObjectMapper objectMapper;

  @Override
  public Response toResponse(Throwable exception) {
    log.error("An error occurred", exception);

    int code = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    if (exception instanceof WebApplicationException webAppException) {
      code = webAppException.getResponse().getStatus();
    }

    ObjectNode exceptionJson = objectMapper.createObjectNode();
    exceptionJson.put("code", code);
    exceptionJson.put("exceptionType", exception.getClass().getName());

    if (exception.getMessage() != null) {
      exceptionJson.put("error", exception.getMessage());
    }

    return Response.status(code).entity(exceptionJson).build();
  }
}
