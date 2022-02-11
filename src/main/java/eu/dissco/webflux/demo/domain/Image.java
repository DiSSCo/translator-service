package eu.dissco.webflux.demo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Image {

  @JsonProperty("ods:imageURI")
  String imageUri;

}
