package eu.dissco.webflux.demo.properties;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "naturalis")
public class NaturalisProperties {

  @NotBlank
  private String url = "https://api.biodiversitydata.nl/v2/specimen";

  @NotBlank
  private String endpoint = "https://api.biodiversitydata.nl/v2/specimen/download";

}
