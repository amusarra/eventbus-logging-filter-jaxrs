package it.dontesta.eventbus.application.configuration;

/**
 * Questa classe rappresenta l'indirizzo dell'event handler.
 *
 * <p>Questo componente è usato per definire l'indirizzo dell'event handler e se è abilitato o meno
 * e usato dal converter {@code EventHandlerAddressConverter} per convertire la stringa di
 * configurazione in un oggetto {@code EventHandlerAddress}.
 *
 * @see it.dontesta.eventbus.application.configuration.converter.EventHandlerAddressConverter
 */
public class EventHandlerAddress {

  private String address;

  private boolean enabled;

  /**
   * Costruttore di default.
   */
  public EventHandlerAddress(String address, boolean enabled) {
    this.address = address;
    this.enabled = enabled;
  }

  /**
   * Restituisce l'indirizzo dell'event handler.
   *
   * @return l'indirizzo dell'event handler
   */
  public String getAddress() {
    return address;
  }

  /**
   * Imposta l'indirizzo dell'event handler.
   *
   * @param address l'indirizzo dell'event handler
   */
  public void setAddress(String address) {
    this.address = address;
  }

  /**
   * Restituisce se l'event handler è abilitato o meno.
   *
   * @return true se l'event handler è abilitato, false altrimenti
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Imposta se l'event handler è abilitato o meno.
   *
   * @param enabled true se l'event handler è abilitato, false altrimenti
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
