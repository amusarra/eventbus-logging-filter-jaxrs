package it.dontesta.eventbus.orm.panache.repository;

import io.quarkus.test.junit.QuarkusTest;
import it.dontesta.eventbus.orm.panache.entity.Horse;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class HorseRepositoryIntegrationTest {

  @Inject
  HorseRepository horseRepository;

  @Test
  void testFindById() {
    Horse horse = horseRepository.findById(1L);
    Assertions.assertNotNull(horse);
    Assertions.assertEquals("Judio XXXV", horse.name);
  }

  @Test
  void testFindAll() {
    List<Horse> horses = horseRepository.listAll();
    Assertions.assertFalse(horses.isEmpty());
  }

  @Test
  void testFindOrderedByName() {
    List<Horse> horses = horseRepository.findOrderedByName();
    Assertions.assertFalse(horses.isEmpty());
    Assertions.assertEquals("Francisco", horses.get(0).name);
    Assertions.assertInstanceOf(LocalDate.class, horses.get(0).dateOfBirth);
  }

  @Test
  void testFindByName() {
    Horse horse = horseRepository.find("name", "Francisco").firstResult();
    Assertions.assertNotNull(horse);
    Assertions.assertEquals("Francisco", horse.name);
    Assertions.assertEquals("Mario", horse.owners.get(0).name);
  }
}
