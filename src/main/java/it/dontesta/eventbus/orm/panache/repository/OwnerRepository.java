package it.dontesta.eventbus.orm.panache.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.dontesta.eventbus.orm.panache.entity.Owner;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class OwnerRepository implements PanacheRepository<Owner> {
  public List<Owner> findOrderedByName() {
    return find("ORDER BY name").list();
  }
}
