package eu.dissco.webflux.demo.properties;

import eu.dissco.webflux.demo.domain.Enrichment;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("enrichment")
public class EnrichmentProperties {

  private List<Enrichment> list;

}
