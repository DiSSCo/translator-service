package eu.dissco.webflux.demo.service.webclients;

import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.webflux.demo.Profiles;
import eu.dissco.webflux.demo.domain.DarwinCore;
import eu.dissco.webflux.demo.domain.Image;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
@Slf4j
@Profile(Profiles.NATURALIS)
public class NaturalisService implements WebClientInterface{

  private static final String IDENTIFICATIONS = "identifications";
  private static final String SCIENTIFIC_NAME = "scientificName";
  private static final String FULL_SCIENTIFIC_NAME = "fullScientificName";
  private static final String ASSOCIATED_MULTI_MEDIA_URIS = "associatedMultiMediaUris";

  private final WebClientProperties properties;

  private final WebClient webClient;

  public Stream<DarwinCore> retrieveData() {
    var uriSpec = webClient.get().uri(properties.getEndpoint()).retrieve()
        .bodyToFlux(JsonNode.class);
    return uriSpec.toStream().map(this::parseJson)
        .filter(object -> !object.getBasisOfRecord().equals("HumanObservation"));
  }

  private DarwinCore parseJson(JsonNode item) {
    log.debug("Received a message {}", item.toString());
    var images = gatherImages(item);
    return DarwinCore.builder()
        .basisOfRecord(item.get("recordBasis").asText())
        .institutionID(item.get("owner").asText())
        .occurrenceID(item.get("unitGUID").asText())
        .id(item.get("id").asText())
        .scientificName(getScientificName(item))
        .images(images.isEmpty() ? null : images)
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
