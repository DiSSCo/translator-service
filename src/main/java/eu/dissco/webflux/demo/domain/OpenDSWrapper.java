package eu.dissco.webflux.demo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OpenDSWrapper {

  @JsonProperty("ods:authoritative")
  private Authoritative authoritative;
  @JsonProperty("ods:images")
  private List<Image> images;
  @JsonProperty("ods:unmapped")
  private JsonNode unmapped;
  private String sourceId;

}
