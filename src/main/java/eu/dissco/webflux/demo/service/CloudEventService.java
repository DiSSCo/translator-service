package eu.dissco.webflux.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.OpenDSProperties;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudEventService {

  private final ObjectMapper mapper;
  private final OpenDSProperties openDSProperties;
  private final WebClientProperties webClientProperties;

  public CloudEvent createCloudEvent(OpenDSWrapper openDS) {
    try {
      return CloudEventBuilder.v1()
          .withId(UUID.randomUUID().toString())
          .withType(openDSProperties.getEvenType())
          .withSource(URI.create(webClientProperties.getEndpoint()))
          .withSubject(openDSProperties.getServiceName())
          .withTime(OffsetDateTime.now())
          .withDataContentType("application/json")
          .withData(mapper.writeValueAsBytes(openDS))
          .build();
    } catch (JsonProcessingException e) {
      log.error("Unable to deserialize the object: {}", openDS);
    }
    return null;
  }

}
