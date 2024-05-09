package it.dontesta.eventbus.orm.panache.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import java.time.LocalDate;
import java.util.List;

@Entity
@Cacheable
public class Horse extends PanacheEntity {

  @Column(length = 60)
  public String name;

  @Column(length = 15)
  public String coat;

  @Column(length = 60)
  public String breed;

  public LocalDate dateOfBirth;

  @ManyToMany
  public List<Owner> owners;
}
