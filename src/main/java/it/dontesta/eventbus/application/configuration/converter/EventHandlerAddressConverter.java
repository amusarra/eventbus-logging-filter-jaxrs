package it.dontesta.eventbus.application.configuration.converter;

import it.dontesta.eventbus.application.configuration.EventHandlerAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * Questa classe implementa l'interfaccia {@code Converter} per convertire una stringa in un oggetto
 * {@code EventHandlerAddress}.
 *
 * <p>Questo converter è utilizzato in modo specifico per convertire le proprietà di configurazione
 * relative agli indirizzi degli event handler. La proprietà di configurazione si chiama
 * {@code app.eventbus.consumer.event.handler.addresses[i]} dove i è un numero intero che
 * rappresenta l'indice dell'indirizzo dell'event handler.
 *
 * <p>Il formato della stringa è il seguente: {@code address=address,enabled=enabled} dove address è
 * l'indirizzo dell'event handler ed enabled è un flag booleano che indica se l'event handler è
 * abilitato o meno.
 */
public class EventHandlerAddressConverter implements Converter<EventHandlerAddress> {

  @Override
  public EventHandlerAddress convert(String value) {
    // Definisci il pattern per estrarre i valori di address ed enabled dalle properties
    Pattern pattern = Pattern.compile("address=(.*),enabled=(.*)");
    Matcher matcher = pattern.matcher(value.trim());

    // Itera su tutte le occorrenze del pattern nella stringa value
    while (matcher.find()) {
      String address = matcher.group(1);
      boolean enabled = Boolean.parseBoolean(matcher.group(2));
      if (address != null && !address.isEmpty()) {
        return (new EventHandlerAddress(address, enabled));
      }
    }
    throw new IllegalArgumentException(
        "Failed to parse Event Handler Address {%s}".formatted(value));
  }
}