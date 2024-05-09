package it.dontesta.eventbus.orm.panache.repository;

import io.quarkus.test.junit.QuarkusTest;
import it.dontesta.eventbus.orm.panache.entity.Horse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HorseRepositoryIntegrationTest {

  @Inject
  HorseRepository horseRepository;

  @Test
  @Order(1)
  void testCount() {
    Assertions.assertEquals(5, horseRepository.count());
  }

  @Test
  @Order(2)
  void testFindById() {
    Horse horse = horseRepository.findById(1L);
    Assertions.assertNotNull(horse);
    Assertions.assertEquals("Judio XXXV", horse.name);
  }

  @Test
  @Order(3)
  void testFindAll() {
    List<Horse> horses = horseRepository.listAll();
    Assertions.assertFalse(horses.isEmpty());
  }

  @Test
  @Order(4)
  void testFindOrderedByName() {
    List<Horse> horses = horseRepository.findOrderedByName();
    Assertions.assertFalse(horses.isEmpty());
    Assertions.assertEquals("Artemis", horses.getFirst().name);
    Assertions.assertInstanceOf(LocalDate.class, horses.getFirst().dateOfBirth);
  }

  @Test
  @Order(5)
  void testFindByName() {
    Horse horse = horseRepository.find("name", "Francisco").firstResult();
    Assertions.assertNotNull(horse);
    Assertions.assertEquals("Francisco", horse.name);
    Assertions.assertEquals("Mario", horse.owners.getFirst().name);
  }

  @Test
  @Order(6)
  @Transactional
  void testCreateHorse() {
    Horse horse = new Horse();
    horse.name = "Test Horse";
    horse.coat = "Bay";
    horse.breed = "Arabian";
    horse.dateOfBirth = LocalDate.of(2010, 1, 1);
    horseRepository.persist(horse);
    Assertions.assertNotNull(horse.id);
  }

  @Test
  @Order(7)
  @Transactional
  void testUpdateHorse() {
    Horse horse = horseRepository.findById(5L);
    horse.name = "Updated Horse";
    horseRepository.persist(horse);
    Assertions.assertEquals("Updated Horse", horseRepository.findById(5L).name);
  }

  @Test
  @Order(8)
  @Transactional
  void testDeleteById() {
    horseRepository.deleteById(5L);
    Assertions.assertNull(horseRepository.findById(5L));
  }
}
