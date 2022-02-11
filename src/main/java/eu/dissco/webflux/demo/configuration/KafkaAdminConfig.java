package eu.dissco.webflux.demo.configuration;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import eu.dissco.webflux.demo.properties.KafkaProperties;

@Configuration
@AllArgsConstructor
public class KafkaAdminConfig {

  private final KafkaProperties properties;

  @Bean
  public KafkaAdmin kafkaAdmin() {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getHost());
    return new KafkaAdmin(configs);
  }

  @Bean
  public NewTopic topic() {
    return new NewTopic(properties.getTopic(), properties.getNumberOfPartitions(),
        properties.getNumberOfReplications());
  }

}
