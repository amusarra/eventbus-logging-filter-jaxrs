package it.dontesta.eventbus.orm.panache.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.time.LocalDate;
import java.util.List;

@Entity
@Cacheable
public class Horse extends PanacheEntity {

  public String name;
  public String coat;
  public String breed;
  public LocalDate dateOfBirth;

  @ManyToMany
  @JoinTable(
      name = "horse_owner",
      joinColumns = @JoinColumn(name = "horse_id"),
      inverseJoinColumns = @JoinColumn(name = "owner_id")
  )
  public List<Owner> owners;
}
