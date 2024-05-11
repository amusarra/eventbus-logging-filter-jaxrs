package it.dontesta.eventbus.ws.resources.endpoint.repository.v1;

import it.dontesta.eventbus.orm.panache.entity.Owner;
import it.dontesta.eventbus.orm.panache.repository.OwnerRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * This class represents the REST endpoint for the Owner repository.
 */
@Path("rest/repository/owner/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OwnerRepositoryResources {

  @Inject
  OwnerRepository ownerRepository;

  @Inject
  Logger log;

  @GET
  public List<Owner> getAllOwners() {
    return ownerRepository.findAll().list();
  }

  /**
   * Retrieves owner by ID from the repository.
   *
   * @param id The ID of the owner.
   * @return The owner.
   */
  @GET
  @Path("{id}")
  public Owner getOwnerById(@NotNull Long id) {
    Owner owner = ownerRepository.findById(id);

    if (owner == null) {
      throw new WebApplicationException("Owner with id of " + id + " not found",
          Response.Status.NOT_FOUND);
    }

    return owner;
  }

  /**
   * Creates a new owner.
   *
   * @param owner The owner to create.
   * @return The created owner.
   */
  @POST
  @Transactional
  public Response createOwner(@NotNull Owner owner) {
    // Return a 422 Unprocessable Entity if an ID was provided
    // Not used the constant Response.Status.UNPROCESSABLE_ENTITY because it is not available in the
    // version of the Jakarta EE API used by Quarkus
    if (owner.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    ownerRepository.persist(owner);
    return Response.ok(owner).status(Response.Status.CREATED).build();
  }

  /**
   * Update existing owner.
   *
   * @param owner The owner to update.
   * @return The updated owner.
   */
  @PUT
  @Transactional
  public Owner updateOwner(@NotNull Owner owner) {
    if (owner.id == null) {
      throw new WebApplicationException("Owner ID was not set on request.",
          Response.Status.BAD_REQUEST);
    }

    Owner existingOwner = ownerRepository.findById(owner.id);

    if (existingOwner == null) {
      throw new WebApplicationException("Owner with id of %d not found".formatted(owner.id),
          Response.Status.NOT_FOUND);
    }

    // Update the owner
    // This code is not the best way to update an entity
    if (owner.name != null) {
      existingOwner.name = owner.name;
    }
    if (owner.surname != null) {
      existingOwner.surname = owner.surname;
    }
    if (owner.email != null) {
      existingOwner.email = owner.email;
    }
    if (owner.phoneNumber != null) {
      existingOwner.phoneNumber = owner.phoneNumber;
    }
    if (owner.address != null) {
      existingOwner.address = owner.address;
    }
    if (owner.city != null) {
      existingOwner.city = owner.city;
    }
    if (owner.state != null) {
      existingOwner.state = owner.state;
    }
    if (owner.zipCode != null) {
      existingOwner.zipCode = owner.zipCode;
    }
    if (owner.country != null) {
      existingOwner.country = owner.country;
    }

    ownerRepository.persist(existingOwner);

    return existingOwner;
  }

  /**
   * Delete existing owner.
   *
   * @param id The owner id to delete.
   * @return The updated owner.
   */
  @DELETE
  @Path("{id}")
  @Transactional
  public Response deleteOwner(@NotNull Long id) {
    Owner owner = ownerRepository.findById(id);

    if (owner == null) {
      throw new WebApplicationException("Owner with id of " + id + " not found",
          Response.Status.NOT_FOUND);
    }

    ownerRepository.delete(owner);
    return Response.status(Response.Status.NO_CONTENT).build();
  }
}
