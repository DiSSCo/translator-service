package eu.dissco.webflux.demo.service.webclients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.Profiles;
import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.Image;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.EnrichmentProperties;
import eu.dissco.webflux.demo.properties.OpenDSProperties;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.RorService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@Profile(Profiles.NATURALIS)
public class NaturalisService extends AbstractWebClientService {

  private static final String IDENTIFICATIONS = "identifications";
  private static final String SCIENTIFIC_NAME = "scientificName";
  private static final String FULL_SCIENTIFIC_NAME = "fullScientificName";
  private static final String ASSOCIATED_MULTI_MEDIA_URIS = "associatedMultiMediaUris";

  public NaturalisService(ObjectMapper mapper,
      OpenDSProperties openDSProperties,
      WebClientProperties webClientProperties,
      KafkaService kafkaService, RorService rorService,
      WebClient webClient,
      EnrichmentProperties enrichmentProperties) {
    super(mapper, openDSProperties, webClientProperties, kafkaService, rorService, webClient,
        enrichmentProperties);
  }

  public void retrieveData() {
    var uriSpec = webClient.get().uri(webClientProperties.getEndpoint()).retrieve()
        .bodyToFlux(JsonNode.class);
    uriSpec.toStream()
        .filter(object -> !object.get("recordBasis").asText().equals("HumanObservation"))
        .map(this::parseJson)
        .map(this::addEnrichmentQueues)
        .map(this::createCloudEvent)
        .filter(Objects::nonNull)
        .forEach(kafkaService::sendMessage);
  }

  private OpenDSWrapper parseJson(JsonNode item) {
    log.debug("Received a message {}", item.toString());
    var images = gatherImages(item);
    return OpenDSWrapper.builder().authoritative(
            Authoritative.builder()
                .materialType(item.get("recordBasis").asText())
                .institutionCode(item.get("owner").asText())
                .institution(rorService.getRoRId(item.get("owner").asText()))
                .curatedObjectID(item.get("unitGUID").asText())
                .physicalSpecimenId(item.get("id").asText())
                .name(getScientificName(item))
                .midslevel(1)
                .build()
        ).images(images.isEmpty() ? null : images)
        .build();
  }

  private String getScientificName(JsonNode item) {
    if (item.has(IDENTIFICATIONS) && item.get(IDENTIFICATIONS).size() > 0) {
      var identification = item.get(IDENTIFICATIONS).get(0);
      if (identification.has(SCIENTIFIC_NAME)) {
        var scientificName = identification.get(SCIENTIFIC_NAME);
        if (scientificName.has(FULL_SCIENTIFIC_NAME)) {
          return scientificName.get(FULL_SCIENTIFIC_NAME).asText();
        }
      }
    }
    return "Unknown";
  }

  private List<Image> gatherImages(JsonNode item) {
    var images = new ArrayList<Image>();
    if (item.has(ASSOCIATED_MULTI_MEDIA_URIS)) {
      log.debug("Found associated media for item: {}", item.get("id").asText());
      for (JsonNode image : item.get(ASSOCIATED_MULTI_MEDIA_URIS)) {
        images.add(Image.builder()
            .imageUri(image.get("accessUri").asText())
            .build());
      }
    }
    return images;
  }

}
