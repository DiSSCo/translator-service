package eu.dissco.webflux.demo.service.webclients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.domain.TranslatorEventData;
import eu.dissco.webflux.demo.properties.EnrichmentProperties;
import eu.dissco.webflux.demo.properties.OpenDSProperties;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.RorService;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public abstract class AbstractWebClientService {

  private static final List<String> MATERIAL_TYPES_ACCEPTED = List.of("PreservedSpecimen",
      "Preservedspecimen", "preservedspecimen", "Fossil", "Other", "Rock", "Mineral", "Meteorite",
      "FossilSpecimen", "LivingSpecimen, MaterialSample");

  protected final ObjectMapper mapper;
  protected final OpenDSProperties openDSProperties;
  protected final WebClientProperties webClientProperties;
  protected final KafkaService kafkaService;
  protected final RorService rorService;
  protected final WebClient webClient;
  protected final EnrichmentProperties enrichmentProperties;

  public abstract void retrieveData();

  protected void publishRecords(Collection<OpenDSWrapper> openDsRecords) {
    openDsRecords.stream()
        .filter(this::checkMaterialType)
        .map(this::addRoR)
        .map(this::addEnrichmentQueues)
        .map(this::createCloudEvent)
        .filter(Objects::nonNull)
        .forEach(kafkaService::sendMessage);
  }

  private OpenDSWrapper addRoR(OpenDSWrapper openDSWrapper) {
    if (openDSProperties.getOrganisationId() != null) {
      openDSWrapper.getAuthoritative()
          .setInstitution(openDSProperties.getOrganisationId());
    } else {
      openDSWrapper.getAuthoritative()
          .setInstitution(
              rorService.getRoRId(openDSWrapper.getAuthoritative().getInstitutionCode()));
    }
    return openDSWrapper;
  }

  protected CloudEvent createCloudEvent(TranslatorEventData translatorEventData) {
    try {
      return CloudEventBuilder.v1()
          .withId(UUID.randomUUID().toString())
          .withType(openDSProperties.getEvenType())
          .withSource(URI.create(webClientProperties.getEndpoint()))
          .withSubject(openDSProperties.getServiceName())
          .withTime(OffsetDateTime.now(ZoneOffset.UTC))
          .withDataContentType("application/json")
          .withData(mapper.writeValueAsBytes(translatorEventData))
          .build();
    } catch (JsonProcessingException e) {
      log.error("Unable to deserialize the object: {}", translatorEventData);
    }
    return null;
  }

  protected TranslatorEventData addEnrichmentQueues(OpenDSWrapper openDSWrapper) {
    return TranslatorEventData.builder().enrichment(enrichmentProperties.getList())
        .openDS(openDSWrapper).build();
  }

  private boolean checkMaterialType(OpenDSWrapper openDS) {
    var materialType = openDS.getAuthoritative().getMaterialType();
    for (var type : MATERIAL_TYPES_ACCEPTED) {
      if (type.equals(materialType)) {
        return true;
      }
    }
    log.warn("Specimen not of an accepted materialType, material type was: {}", materialType);
    return false;
  }

}
