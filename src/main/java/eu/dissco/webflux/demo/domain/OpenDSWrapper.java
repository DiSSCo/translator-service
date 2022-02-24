package eu.dissco.webflux.demo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  private String sourceId;

}
