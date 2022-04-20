package eu.dissco.webflux.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.properties.EnrichmentProperties;
import eu.dissco.webflux.demo.properties.KafkaProperties;
import io.cloudevents.CloudEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaService {

  private final KafkaTemplate<String, CloudEvent> kafkaTemplate;
  private final KafkaProperties properties;

  public void sendMessage(CloudEvent event) {
    ListenableFuture<SendResult<String, CloudEvent>> future = kafkaTemplate.send(properties.getTopic(), event);
    future.addCallback(new ListenableFutureCallback<>() {

      @Override
      public void onSuccess(SendResult<String, CloudEvent> result) {
        var offset = result.getRecordMetadata().offset();
        if (offset % properties.getLogAfterLines() == 0) {
          log.info("Currently at offset: {}", offset);
        }
      }

      @Override
      public void onFailure(Throwable ex) {
        log.error("Unable to send message: {}", event, ex);
      }
    });
  }
}
