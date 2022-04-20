package eu.dissco.webflux.demo.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@JsonInclude(Include.NON_NULL)
public class Authoritative {

  @JsonProperty("ods:midsLevel")
  private int midslevel;
  @JsonProperty("ods:curatedObjectID")
  private String curatedObjectID;
  @JsonProperty("ods:physicalSpecimenId")
  private String physicalSpecimenId;
  @JsonProperty("ods:institution")
  private String institution;
  @JsonProperty("ods:institutionCode")
  private String institutionCode;
  @JsonProperty("ods:materialType")
  private String materialType;
  @JsonProperty("ods:name")
  private String name;
}
