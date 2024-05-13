package it.dontesta.eventbus.application.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import it.dontesta.eventbus.application.configuration.converter.EventHandlerAddressConverter;
import java.util.ArrayList;
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

    eventHandlerAddresses = new ArrayList<>();
    eventHandlerAddresses.add(new EventHandlerAddress("testAddress1", true));
    eventHandlerAddresses.add(new EventHandlerAddress("testAddress2", false));
    eventHandlerAddresses.add(new EventHandlerAddress("testAddress3", true));

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
    Assertions.assertTrue(eventHandlerVirtualAddresses.size() >= 2);
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

  @Test
  void testIsAddressAndExistsEnabled() {
    // Test when address exists and is enabled
    assertTrue(EventHandlerAddress.isAddressAndExistsEnabled(eventHandlerAddresses, "testAddress1"));

    // Test when address exists but is not enabled
    assertFalse(EventHandlerAddress.isAddressAndExistsEnabled(eventHandlerAddresses, "testAddress2"));

    // Test when address does not exist
    assertFalse(EventHandlerAddress.isAddressAndExistsEnabled(eventHandlerAddresses, "nonExistingAddress"));
  }


  private List<EventHandlerAddress> eventHandlerAddresses;

  private EventHandlerAddress eventHandlerAddress;

  private EventHandlerAddressConverter converter;
}
