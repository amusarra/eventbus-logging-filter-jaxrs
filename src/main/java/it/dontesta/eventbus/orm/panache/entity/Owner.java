package it.dontesta.eventbus.orm.panache.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import java.util.List;

/**
 * This class represents the Owner entity.
 */
@Entity
@Cacheable
public class Owner extends PanacheEntity {

  /**
   * Il nome del proprietario.
   */
  @Column(length = 60, nullable = false)
  public String name;

  /**
   * Il cognome del proprietario.
   */
  @Column(length = 60, nullable = false)
  public String surname;

  /**
   * L'indirizzo email del proprietario.
   */
  @Column(length = 60, nullable = false, unique = true)
  public String email;

  /**
   * Il numero di telefono del proprietario.
   */
  @Column(length = 20)
  public String phoneNumber;

  /**
   * L'indirizzo del proprietario.
   */
  @Column(length = 60)
  public String address;

  /**
   * La città del proprietario.
   */
  @Column(length = 60)
  public String city;

  /**
   * Lo stato del proprietario.
   */
  @Column(length = 2)
  public String state;

  /**
   * Il codice postale del proprietario.
   */
  @Column(length = 10)
  public String zipCode;

  /**
   * Il paese del proprietario.
   */
  @Column(length = 60)
  public String country;

  /**
   * La lista dei cavalli di proprietà del proprietario.
   */
  @ManyToMany(mappedBy = "owners")
  @JsonBackReference
  public List<Horse> horses;
}
