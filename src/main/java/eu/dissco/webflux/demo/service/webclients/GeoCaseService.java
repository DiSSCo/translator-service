package eu.dissco.webflux.demo.service.webclients;

import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.webflux.demo.Profiles;
import eu.dissco.webflux.demo.domain.DarwinCore;
import eu.dissco.webflux.demo.domain.Image;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@AllArgsConstructor
@Profile(Profiles.GEO_CASE)
public class GeoCaseService implements WebClientInterface {

  private static final String RESPONSE = "response";

  private final WebClient webClient;
  private final WebClientProperties properties;

  public Stream<DarwinCore> retrieveData() {
    var uri = properties.getEndpoint() + properties.getQueryParams();
    return webClient.get().uri(uri + "&start=0&rows=" + properties.getItemsPerRequest()).retrieve()
        .bodyToFlux(JsonNode.class).expand(
            response -> {
              var total = response.get(RESPONSE).get("numFound").asInt();
              var currentPosition = response.get(RESPONSE).get("start").asInt();
              if (currentPosition + properties.getItemsPerRequest() <= total) {
                currentPosition = currentPosition + properties.getItemsPerRequest();
                return webClient.get().uri(uri + "&start=" + currentPosition).retrieve()
                    .bodyToFlux(JsonNode.class);
              }
              return Flux.empty();
            }).map(this::mapToDarwin).toStream().flatMap(Collection::stream);
  }

  private List<DarwinCore> mapToDarwin(JsonNode response) {
    var items = response.get(RESPONSE).get("docs");
    var list = new ArrayList<DarwinCore>();
    items.forEach(item -> {
      var darwin = DarwinCore.builder()
          .id(item.get("unitid").asText())
          .scientificName(
              item.has("fullscientificname") ? item.get("fullscientificname").asText() : null)
          .basisOfRecord(item.has("recordbasis") ? item.get("recordbasis").asText() : null)
          .institutionID(item.has("datasetowner") ? item.get("datasetowner").asText() : null)
          .occurrenceID(item.has("recordURI") ? item.get("recordURI").asText() : null)
          .images(item.has("images") ? getImages(item.get("images")) : null)
          .build();
      list.add(darwin);
    });
    return list;
  }

  private List<Image> getImages(JsonNode imagesNode) {
    var images = new ArrayList<Image>();
    for (JsonNode image : imagesNode) {
      images.add(Image.builder().imageUri(image.asText()).build());
    }
    return images;
  }

}
