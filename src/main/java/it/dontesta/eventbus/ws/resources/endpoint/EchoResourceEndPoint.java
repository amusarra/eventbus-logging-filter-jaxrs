package it.dontesta.eventbus.ws.resources.endpoint;

import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
      @Size(min = 32, max = 4096,
          message = "Il parametro di input deve essere compreso tra 32 byte e 4 KB")
      String input) {
    return Response.ok(input).build();
  }
}
