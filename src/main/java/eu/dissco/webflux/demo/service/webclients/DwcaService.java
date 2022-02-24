package eu.dissco.webflux.demo.service.webclients;

import eu.dissco.webflux.demo.Profiles;
import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.DwcaProperties;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.service.KafkaService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DwcTerm;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@AllArgsConstructor
@Profile(Profiles.DWCA)
public class DwcaService implements WebClientInterface {

  private final WebClient webClient;

  private final WebClientProperties properties;

  private final DwcaProperties dwcaProperties;

  private final KafkaService kafkaService;

  @Override
  public void retrieveData() {
    try {
      var file = Path.of(dwcaProperties.getDownloadFile());
      var buffer = webClient.get().uri(properties.getEndpoint()).retrieve().bodyToFlux(
          DataBuffer.class);
      DataBufferUtils.write(buffer, Files.newOutputStream(file)).map(DataBufferUtils::release)
          .then().toFuture().get();
      var openDsRecords = mapToOpenDS(file);
      openDsRecords.forEach(kafkaService::sendMessage);
    } catch (IOException e) {
      log.error("Failed to open output stream for download file", e);
    } catch (ExecutionException e) {
      log.error("Failed during downloading file with exception", e.getCause());
    } catch (InterruptedException e) {
      log.error("Failed during downloading file due to interruption", e);
      Thread.currentThread().interrupt();
    }
  }

  private List<OpenDSWrapper> mapToOpenDS(Path file) {
    try {
      var archive = DwcFiles.fromCompressed(file, Path.of(dwcaProperties.getTempFolder()));
      var openDsRecords = new ArrayList<OpenDSWrapper>();
      for (Record rec : archive.getCore()) {
        openDsRecords.add(OpenDSWrapper.builder()
            .authoritative(Authoritative.builder()
                .midslevel(1)
                .name(rec.value(DwcTerm.scientificName))
                .materialType(rec.value(DwcTerm.basisOfRecord))
                .physicalSpecimenId(rec.value(DwcTerm.catalogNumber))
                .curatedObjectID(rec.value(DwcTerm.occurrenceID))
                .institutionCode(rec.value(DwcTerm.institutionCode))
                .institution(rec.value(DwcTerm.institutionID))
                .build())
            .sourceId("translator-service")
            .build());
      }
      log.info("Total discovered records are: {}", openDsRecords.size());
      return openDsRecords;
    } catch (IOException e) {
      log.error("Failed to open Dwca from temp folder, cannot process records");
      return List.of();
    }
  }

}
