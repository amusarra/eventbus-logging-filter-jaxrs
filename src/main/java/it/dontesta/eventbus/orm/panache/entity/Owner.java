package it.dontesta.eventbus.orm.panache.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import java.util.List;

@Entity
@Cacheable
public class Owner extends PanacheEntity {

    public String name;
    public String surname;
    public String email;
    public String phoneNumber;
    public String address;
    public String city;
    public String state;
    public String zipCode;
    public String country;

    @ManyToMany(mappedBy = "owners")
    public List<Horse> horses;
}
