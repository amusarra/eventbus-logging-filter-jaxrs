/*
 * SPDX-FileCopyrightText: 2024 Antonio Musarra's Blog <https://www.dontesta.it>
 * SPDX-License-Identifier: MIT
 */
package it.dontesta.eventbus.orm.panache.repository;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.dontesta.eventbus.orm.panache.entity.Horse;

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
