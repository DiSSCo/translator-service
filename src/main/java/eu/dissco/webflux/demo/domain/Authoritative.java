package eu.dissco.webflux.demo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Authoritative {

    @JsonProperty("ods:midsLevel")
    int midslevel;
    @JsonProperty("ods:curatedObjectID")
    String curatedObjectID;
    @JsonProperty("ods:physicalSpecimenId")
    String physicalSpecimenId;
    @JsonProperty("ods:institution")
    String institution;
    @JsonProperty("ods:institutionCode")
    String institutionCode;
    @JsonProperty("ods:materialType")
    String materialType;
    @JsonProperty("ods:name")
    String name;
}
