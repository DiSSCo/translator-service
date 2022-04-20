package eu.dissco.webflux.demo.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class TranslatorEventData {

  @JsonProperty("openDS")
  OpenDSWrapper openDS;
  @JsonProperty("enrichment")
  List<Enrichment> enrichment;

}
