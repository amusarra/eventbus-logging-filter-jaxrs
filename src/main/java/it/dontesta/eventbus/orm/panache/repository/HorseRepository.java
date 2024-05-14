package it.dontesta.eventbus.orm.panache.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.dontesta.eventbus.orm.panache.entity.Horse;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * This class is a Panache repository for the Horse entity.
 */
@ApplicationScoped
public class HorseRepository implements PanacheRepository<Horse> {

  /**
   * This method finds all the horses ordered by name.
   *
   * @return the list of horses ordered by name
   */
  public List<Horse> findOrderedByName() {
    return find("ORDER BY name").list();
  }
}
