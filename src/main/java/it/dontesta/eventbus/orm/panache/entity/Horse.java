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

  /**
   * Il nome del cavallo.
   */
  @Column(length = 60, nullable = false)
  public String name;

  /**
   * Il sesso del cavallo.
   */
  @Column(length = 1, nullable = false)
  @Pattern(regexp = "^[MF]$", message = "The admitted values for the sex attribute are: 'M' or 'F'")
  public String sex;

  /**
   * Il colore del mantello del cavallo.
   */
  @Column(length = 15, nullable = false)
  public String coat;

  /**
   * La razza del cavallo.
   */
  @Column(length = 60, nullable = false)
  public String breed;

  /**
   * La data di nascita del cavallo.
   */
  @Column(nullable = false)
  public LocalDate dateOfBirth;

  /**
   * Il numero di registrazione del cavallo.
   */
  @Column(length = 15)
  public String registrationNumber;

  /**
   * Il numero del microchip del cavallo.
   */
  @Column(length = 15)
  public String microchipNumber;

  /**
   * Il numero del passaporto del cavallo.
   */
  @Column(length = 15)
  public String passportNumber;

  /**
   * L'altezza del cavallo in centimetri.
   */
  @Column(length = 3)
  public int height;

  /**
   * La lista dei proprietari del cavallo.
   */
  @ManyToMany
  public List<Owner> owners;
}
