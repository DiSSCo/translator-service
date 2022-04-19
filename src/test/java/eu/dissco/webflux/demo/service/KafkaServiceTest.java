package eu.dissco.webflux.demo.service;

import static eu.dissco.webflux.demo.util.TestUtil.testCloudEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.webflux.demo.properties.KafkaProperties;
import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@ExtendWith(MockitoExtension.class)
class KafkaServiceTest {

  @Mock
  private KafkaProperties properties;
  @Mock
  private KafkaTemplate<String, CloudEvent> kafkaTemplate;
  @Mock
  private ListenableFuture<SendResult<String, CloudEvent>> listenableFuture;
  @Mock
  private SendResult<String, CloudEvent> sendResult;
  private KafkaService service;

  @BeforeEach
  void setup() {
    this.service = new KafkaService(kafkaTemplate, properties);
  }

  @Test
  void testSendMessage() throws JsonProcessingException {
    // Given
    var recordMetadata = new RecordMetadata(new TopicPartition("naturalis", 1), 100L, 0, 0L, 0, 0);
    given(kafkaTemplate.send(anyString(), any(CloudEvent.class))).willReturn(listenableFuture);
    given(sendResult.getRecordMetadata()).willReturn(recordMetadata);
    given(properties.getTopic()).willReturn("naturalis");
    given(properties.getLogAfterLines()).willReturn(100);
    doAnswer(invocation -> {
      ListenableFutureCallback callBack = invocation.getArgument(0);
      callBack.onSuccess(sendResult);
      assertThat(sendResult.getRecordMetadata().offset()).isEqualTo(100);
      return null;
    }).when(listenableFuture).addCallback(any(ListenableFutureCallback.class));

    // When
    service.sendMessage(testCloudEvent());

    // Then
    then(kafkaTemplate).should().send(anyString(), any(CloudEvent.class));
  }
}
