package it.dontesta.eventbus.orm.panache.repository;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.dontesta.eventbus.orm.panache.entity.Horse;
import it.dontesta.eventbus.orm.panache.entity.Owner;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class PanacheRepositoryMockedTest {

  @InjectMock
  HorseRepository horseRepository;

  @InjectMock
  OwnerRepository ownerRepository;

  @Test
  void testOwnerRepository() {
    // Mocked classed always return a default value
    Assertions.assertEquals(0, ownerRepository.count());

    // Now let's specify the return value
    Mockito.when(ownerRepository.count()).thenReturn(1L);
    Assertions.assertEquals(1, ownerRepository.count());

    // Now let's specify the return value
    Mockito.when(ownerRepository.count()).thenReturn(2L);
    Assertions.assertEquals(2, ownerRepository.count());

    // Now let's call the original method
    Mockito.when(ownerRepository.count()).thenCallRealMethod();
    Assertions.assertEquals(2, ownerRepository.count());

    // Check that we called it 4 times
    Mockito.verify(ownerRepository, Mockito.times(4)).count();

    // Mock only with specific parameters
    Owner owner = new Owner();
    Mockito.when(ownerRepository.findById(1L)).thenReturn(owner);
    Assertions.assertSame(owner, ownerRepository.findById(1L));
    Assertions.assertNull(ownerRepository.findById(4L));

    Mockito.when(ownerRepository.findOrderedByName()).thenReturn(Collections.emptyList());
    Assertions.assertTrue(ownerRepository.findOrderedByName().isEmpty());

    // We can even mock your custom methods
    Mockito.verify(ownerRepository).findOrderedByName();
    Mockito.verify(ownerRepository, Mockito.atLeastOnce()).findById(Mockito.any());
    Mockito.verifyNoMoreInteractions(ownerRepository);
  }

  @Test
  void testHorseRepository() {
    // Mocked classed always return a default value
    Assertions.assertEquals(0, horseRepository.count());

    // Now let's specify the return value
    Mockito.when(horseRepository.count()).thenReturn(1L);
    Assertions.assertEquals(1, horseRepository.count());

    // Now let's specify the return value
    Mockito.when(horseRepository.count()).thenReturn(2L);
    Assertions.assertEquals(2, horseRepository.count());

    // Now let's call the original method
    Mockito.when(horseRepository.count()).thenCallRealMethod();
    Assertions.assertEquals(3, horseRepository.count());

    Horse horse = new Horse();
    horse.name = "Judio XXXV";
    Mockito.when(horseRepository.findById(1L)).thenReturn(horse);
    Assertions.assertEquals("Judio XXXV", horseRepository.findById(1L).name);

    // Check that we called it 4 times
    Mockito.verify(horseRepository, Mockito.times(4)).count();

    // Mock only with specific parameters
    horse = new Horse();
    horse.owners = List.of(new Owner());

    Mockito.when(horseRepository.findById(1L)).thenReturn(horse);
    Assertions.assertSame(horse, horseRepository.findById(1L));
    Assertions.assertNull(horseRepository.findById(4L));
    Assertions.assertEquals(1, horseRepository.findById(1L).owners.size());

    Mockito.when(horseRepository.findOrderedByName()).thenReturn(Collections.emptyList());
    Assertions.assertTrue(horseRepository.findOrderedByName().isEmpty());

    // We can even mock your custom methods
    Mockito.verify(horseRepository).findOrderedByName();
    Mockito.verify(horseRepository, Mockito.atLeastOnce()).findById(Mockito.any());
    Mockito.verifyNoMoreInteractions(horseRepository);
  }
}
