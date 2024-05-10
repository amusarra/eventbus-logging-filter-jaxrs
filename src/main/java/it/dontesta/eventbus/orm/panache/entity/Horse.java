package it.dontesta.eventbus.orm.panache.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

/**
 * This class represents the Horse entity.
 */
@Entity
@Cacheable
public class Horse extends PanacheEntity {

  @Column(length = 60, nullable = false)
  public String name;

  @Column(length = 1, nullable = false)
  @Pattern(regexp = "^[MF]$", message = "The admitted values for the sex attribute are: 'M' or 'F'")
  public String sex;

  @Column(length = 15, nullable = false)
  public String coat;

  @Column(length = 60, nullable = false)
  public String breed;

  @Column(nullable = false)
  public LocalDate dateOfBirth;

  @Column(length = 15)
  public String registrationNumber;

  @Column(length = 15)
  public String microchipNumber;

  @Column(length = 15)
  public String passportNumber;

  @Column(length = 3)
  public int height;

  @ManyToMany
  public List<Owner> owners;
}
