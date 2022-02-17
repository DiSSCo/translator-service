package eu.dissco.webflux.demo.service.webclients;

import com.fasterxml.jackson.databind.JsonNode;
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
      int total = 0;
      try {
        total = webClient.get()
            .uri(uri + "&start=" + start + "&rows=" + properties.getItemsPerRequest())
            .retrieve().bodyToMono(JsonNode.class).map(response -> mapMono(response, result))
            .toFuture().get();
      } catch (InterruptedException | ExecutionException e) {
        log.error("Failed to get response from uri", e);
        Thread.currentThread().interrupt();
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
      var object = OpenDSWrapper.builder()
          .authoritative(
              Authoritative.builder()
                  .physicalSpecimenId(item.get("unitid").asText())
                  .name(item.has("fullscientificname") ? item.get("fullscientificname").asText()
                      : null)
                  .midslevel(1)
                  .materialType(item.has("recordbasis") ? item.get("recordbasis").asText() : null)
                  .curatedObjectID(item.has("recordURI") ? item.get("recordURI").asText() : null)
//                  .institution(rorService.getRoRId(
//                      item.has("datasetowner") ? item.get("datasetowner").asText() : null))
                  .institutionCode(
                      item.has("datasetowner") ? item.get("datasetowner").asText() : null)
                  .build())
          .images(item.has("images") ? getImages(item.get("images")) : null)
          .sourceId("translator-service")
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
