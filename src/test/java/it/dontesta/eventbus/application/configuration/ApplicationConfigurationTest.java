package it.dontesta.eventbus.application.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import it.dontesta.eventbus.application.configuration.converter.EventHandlerAddressConverter;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ApplicationConfigurationTest {

  @ConfigProperty(name = "app.eventbus.consumer.event.handler.addresses")
  List<EventHandlerAddress> eventHandlerVirtualAddresses;

  @BeforeEach
  void setUp() {
    converter = new EventHandlerAddressConverter();
    eventHandlerAddress = new EventHandlerAddress("testAddress", true);
  }

  @Test
  void testConvertWithInvalidInput() {
    String input = "invalidInput";
    assertThrows(IllegalArgumentException.class, () -> converter.convert(input));
  }

  @Test
  void testEventBusConsumerEventHandlerAddresses() {
    Assertions.assertNotNull(eventHandlerVirtualAddresses);
    Assertions.assertFalse(eventHandlerVirtualAddresses.isEmpty());
    Assertions.assertTrue(eventHandlerVirtualAddresses.size() > 0);
  }

  @Test
  void getAddress() {
    assertEquals("testAddress", eventHandlerAddress.getAddress());
  }

  @Test
  void setAddress() {
    eventHandlerAddress.setAddress("newAddress");
    assertEquals("newAddress", eventHandlerAddress.getAddress());
  }

  @Test
  void isEnabled() {
    assertTrue(eventHandlerAddress.isEnabled());
  }

  @Test
  void setEnabled() {
    eventHandlerAddress.setEnabled(false);
    assertFalse(eventHandlerAddress.isEnabled());
  }

  private EventHandlerAddress eventHandlerAddress;

  private EventHandlerAddressConverter converter;
}
