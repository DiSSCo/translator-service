package eu.dissco.webflux.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@AllArgsConstructor
@Slf4j
public class RorService {

  private final WebClient webClient;

  @Cacheable("rorid")
  public String getRoRId(String institutionCode) {
    log.info("Requesting ROR for organization: {}", institutionCode);
    String url = "https://api.ror.org/organizations?affiliation=" + institutionCode;
    var uriSpec = webClient.get().uri(url).retrieve();
    var response = uriSpec.bodyToMono(JsonNode.class);
    var json = response.blockOptional(Duration.ofSeconds(5));
    if (json.isPresent()) {
      var items = json.get().get("items");
      if (items.size() > 0) {
        var score = items.get(0).get("score");
        if (score.asDouble() >= 1.0) {
          var rorId = items.get(0).get("organization").get("id");
          log.info("ROR for {} is {}", institutionCode, rorId);
          return rorId.asText();
        }
      }
    }
    log.warn("Could not match name to a ROR id for: {}", institutionCode);
    return "Unknown";
  }
}
