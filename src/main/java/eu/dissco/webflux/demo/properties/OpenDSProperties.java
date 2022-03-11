package eu.dissco.webflux.demo.properties;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("opends")
public class OpenDSProperties {

  private String organisationId;

  @NotBlank
  private String serviceName;

}
