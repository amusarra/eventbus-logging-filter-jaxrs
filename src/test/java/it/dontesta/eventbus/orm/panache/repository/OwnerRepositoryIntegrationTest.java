package it.dontesta.eventbus.orm.panache.repository;

import io.quarkus.test.junit.QuarkusTest;
import it.dontesta.eventbus.orm.panache.entity.Owner;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OwnerRepositoryIntegrationTest {

  @Inject
  OwnerRepository ownerRepository;

  @Test
  @Order(1)
  void testCount() {
    Assertions.assertEquals(3, ownerRepository.count());
  }

  @Test
  @Order(2)
  void testFindById() {
    Owner owner = ownerRepository.findById(1L);
    Assertions.assertNotNull(owner);
    Assertions.assertEquals("John", owner.name);
  }

  @Test
  @Order(3)
  void testFindAll() {
    List<Owner> owners = ownerRepository.listAll();
    Assertions.assertFalse(owners.isEmpty());
  }

  @Test
  @Order(4)
  void testFindOrderedByName() {
    List<Owner> owners = ownerRepository.findOrderedByName();
    Assertions.assertEquals("John", owners.getFirst().name);
    Assertions.assertFalse(owners.isEmpty());
  }

  @Test
  @Order(5)
  void testFindByName() {
    Owner owner = ownerRepository.find("name", "Mario").firstResult();
    Assertions.assertNotNull(owner);
    Assertions.assertNotNull(owner.horses);
    Assertions.assertEquals(3, owner.horses.size());
    Assertions.assertEquals("Thunder", owner.horses.getFirst().name);
    Assertions.assertEquals("Mario", owner.name);
  }
}
