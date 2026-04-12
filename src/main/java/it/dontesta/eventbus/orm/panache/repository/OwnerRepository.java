package it.dontesta.eventbus.orm.panache.repository;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.dontesta.eventbus.orm.panache.entity.Owner;

/**
 * This class represents the Panache repository for the Owner entity.
 */
@ApplicationScoped
public class OwnerRepository implements PanacheRepository<Owner> {

    /**
     * This method finds all the owners ordered by name.
     *
     * @return the list of owners ordered by name
     */
    public List<Owner> findOrderedByName() {
        return find("ORDER BY name").list();
    }
}
