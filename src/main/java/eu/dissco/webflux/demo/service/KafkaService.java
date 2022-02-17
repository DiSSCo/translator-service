package eu.dissco.webflux.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.KafkaProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaService {

  private final ObjectMapper mapper;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final KafkaProperties properties;

  public void sendMessage(OpenDSWrapper openDSWrapper) {
    try {
      var json = mapper.writeValueAsString(openDSWrapper);
      ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(
          properties.getTopic(), json);
      future.addCallback(new ListenableFutureCallback<>() {

        @Override
        public void onSuccess(SendResult<String, String> result) {
          var offset = result.getRecordMetadata().offset();
          if (offset % properties.getLogAfterLines() == 0) {
            log.info("Currently at offset: {}", offset);
          }
        }

        @Override
        public void onFailure(Throwable ex) {
          log.error("Unable to send message: {}", json, ex);
        }
      });
    } catch (JsonProcessingException e) {
      log.error("Failed to pars Objects to Json: {}", openDSWrapper);
    }
  }
}
