package it.dontesta.eventbus.ws.resources.endpoint.repository.v1;

import it.dontesta.eventbus.orm.panache.entity.Horse;
import it.dontesta.eventbus.orm.panache.repository.HorseRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * This class represents the REST endpoint for the Horse repository.
 */
@Path("rest/repository/horse/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HorseRepositoryResources {

  @Inject
  HorseRepository horseRepository;

  @Inject
  Logger log;

  /**
   * Retrieves all horses from the repository.
   *
   * @return A list of horses.
   */
  @GET
  public List<Horse> getAllHorses() {
    return horseRepository.findAll().list();
  }

  /**
   * Retrieves horse by ID from the repository.
   *
   * @param id The ID of the horse.
   * @return The horse.
   */
  @GET
  @Path("{id}")
  public Horse getHorseById(@NotNull Long id) {
    Horse horse = horseRepository.findById(id);

    if (horse == null) {
      throw new WebApplicationException("Horse with id of " + id + " not found",
          Response.Status.NOT_FOUND);
    }

    return horse;
  }

  /**
   * This method creates a new horse.
   *
   * @param horse The horse to create.
   * @return The created horse.
   */
  @POST
  @Transactional
  public Response createHorse(@NotNull Horse horse) {
    // Return a 422 Unprocessable Entity if an ID was provided
    // Not used the constant Response.Status.UNPROCESSABLE_ENTITY because it is not available in the
    // version of the Jakarta EE API used by Quarkus
    if (horse.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    horseRepository.persist(horse);
    return Response.ok(horse).status(Response.Status.CREATED).build();
  }

  /**
   * This method deletes a horse by its ID.
   *
   * @param id The ID of the horse.
   * @return The response.
   */
  @DELETE
  @Path("{id}")
  @Transactional
  public Response deleteHorse(@NotNull Long id) {
    Horse horse = horseRepository.findById(id);

    if (horse == null) {
      throw new WebApplicationException("Horse with id of " + id + " not found",
          Response.Status.NOT_FOUND);
    }

    horseRepository.delete(horse);
    return Response.status(Response.Status.NO_CONTENT).build();
  }
}
