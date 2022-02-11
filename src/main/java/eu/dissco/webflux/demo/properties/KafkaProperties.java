package eu.dissco.webflux.demo.properties;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("kafka")
public class KafkaProperties {

  @NotBlank
  private String host;

  @NotBlank
  private String topic;

  private int numberOfPartitions = 1;

  private short numberOfReplications = 1;

  @Positive
  private int logAfterLines = 100;
}
