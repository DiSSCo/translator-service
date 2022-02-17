package eu.dissco.webflux.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.ExecutionException;
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
    try {
      var json = response.toFuture().get();
      if (json != null) {
        var items = json.get("items");
        if (items.size() > 0) {
          for (var item : items) {
            if (item.get("chosen").asBoolean()) {
              var rorId = item.get("organization").get("id");
              log.info("ROR for {} is {}", institutionCode, rorId);
              return rorId.asText();
            }
          }
        }
      }
    } catch (InterruptedException | ExecutionException e) {
      log.error("Failed to make request to RoR service", e);
      Thread.currentThread().interrupt();
    }
    log.warn("Could not match name to a ROR id for: {}", url);
    return "Unknown";
  }
}
