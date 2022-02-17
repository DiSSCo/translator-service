package eu.dissco.webflux.demo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class OpenDSWrapper {

    @JsonProperty("ods:authoritative")
    Authoritative authoritative;
    @JsonProperty("ods:images")
    List<Image> images;
    String sourceId;

}
