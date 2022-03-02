package eu.dissco.webflux.demo.service.webclients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.webflux.demo.Profiles;
import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.Image;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.RorService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@AllArgsConstructor
@Profile(Profiles.GEO_CASE)
public class GeoCaseService implements WebClientInterface {

  private static final String UNIT_ID = "unitid";
  private static final String FULL_SCIENTIFIC_NAME = "fullscientificname";
  private static final String RECORD_BASIS = "recordbasis";
  private static final String RECORD_URI = "recordURI";
  private static final String DATA_SET_OWNER = "datasetowner";
  private static final String IMAGES = "images";

  private static final List<String> MAPPED_TERMS = List.of(UNIT_ID, FULL_SCIENTIFIC_NAME,
      RECORD_BASIS, RECORD_URI, DATA_SET_OWNER, IMAGES);

  private static final String RESPONSE = "response";

  private final WebClient webClient;
  private final WebClientProperties properties;
  private final RorService rorService;
  private final KafkaService kafkaService;

  public void retrieveData() {
    var uri = properties.getEndpoint() + properties.getQueryParams();
    var start = 0;
    var finished = false;
    while (!finished) {
      var result = new ArrayList<OpenDSWrapper>();
      log.info("Currently at: {} still collecting...", start);
      int total;
      try {
        total = webClient.get()
            .uri(uri + "&start=" + start + "&rows=" + properties.getItemsPerRequest())
            .retrieve().bodyToMono(JsonNode.class).map(response -> mapMono(response, result))
            .toFuture().get();
      } catch (InterruptedException | ExecutionException e) {
        log.error("Failed to get response from uri", e);
        Thread.currentThread().interrupt();
        return;
      }
      if (start + properties.getItemsPerRequest() >= total) {
        finished = true;
      } else {
        start = start + properties.getItemsPerRequest();
      }
      result.stream().map(this::addRoR).forEach(kafkaService::sendMessage);
    }
  }

  private OpenDSWrapper addRoR(OpenDSWrapper openDSWrapper) {
    openDSWrapper.getAuthoritative()
        .setInstitution(rorService.getRoRId(openDSWrapper.getAuthoritative().getInstitutionCode()));
    return openDSWrapper;
  }

  private int mapMono(JsonNode response, ArrayList<OpenDSWrapper> result) {
    var total = response.get(RESPONSE).get("numFound").asInt();
    var items = response.get(RESPONSE).get("docs");
    items.forEach(item -> {
      var unmapped = (ObjectNode) item.deepCopy();
      unmapped.remove(MAPPED_TERMS);
      var object = OpenDSWrapper.builder()
          .authoritative(
              Authoritative.builder()
                  .physicalSpecimenId(item.get(UNIT_ID).asText())
                  .name(item.has(FULL_SCIENTIFIC_NAME) ? item.get(FULL_SCIENTIFIC_NAME).asText()
                      : null)
                  .midslevel(1)
                  .materialType(item.has(RECORD_BASIS) ? item.get(RECORD_BASIS).asText() : null)
                  .curatedObjectID(item.has(RECORD_URI) ? item.get(RECORD_URI).asText() : null)
                  .institutionCode(
                      item.has(DATA_SET_OWNER) ? item.get(DATA_SET_OWNER).asText() : null)
                  .build())
          .images(item.has(IMAGES) ? getImages(item.get(IMAGES)) : null)
          .sourceId("translator-service")
          .unmapped(unmapped)
          .build();
      result.add(object);
    });
    return total;
  }

  private List<Image> getImages(JsonNode imagesNode) {
    var images = new ArrayList<Image>();
    for (JsonNode image : imagesNode) {
      images.add(Image.builder().imageUri(image.asText()).build());
    }
    return images;
  }

}
