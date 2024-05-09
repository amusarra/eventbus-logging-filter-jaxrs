package it.dontesta.eventbus.orm.panache.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import java.util.List;

@Entity
@Cacheable
public class Owner extends PanacheEntity {

  @Column(length = 60)
  public String name;

  @Column(length = 60)
  public String surname;

  @Column(length = 60, unique = true)
  public String email;

  @Column(length = 20)
  public String phoneNumber;

  @Column(length = 60)
  public String address;

  @Column(length = 60)
  public String city;

  @Column(length = 2)
  public String state;

  @Column(length = 10)
  public String zipCode;

  @Column(length = 60)
  public String country;

  @ManyToMany(mappedBy = "owners")
  @JsonBackReference
  public List<Horse> horses;
}
