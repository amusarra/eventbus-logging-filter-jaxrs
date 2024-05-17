package it.dontesta.eventbus.ws.input.wrapper;

import it.dontesta.eventbus.orm.panache.entity.Horse;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * The class is a wrapper class that contains a list of {@link Horse} objects.
 */
public class HorseListWrapper {

  @Size(max = 10, message = "The number of horses must not exceed 10")
  private List<Horse> horses;

  /**
   * Return the list of the horses.
   *
   * @see Horse
   * @return The list of the horses.
   */
  public List<Horse> getHorses() {
    return horses;
  }
}