package eu.dissco.webflux.demo.service.webclients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.webflux.demo.Profiles;
import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.Image;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.DwcaProperties;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.service.KafkaService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gbif.dwc.Archive;
import org.gbif.dwc.ArchiveFile;
import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.UnknownTerm;
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

  private static final List<Term> MAPPED_TERMS = List.of(DwcTerm.scientificName,
      DwcTerm.basisOfRecord, DwcTerm.catalogNumber, DwcTerm.occurrenceID, DwcTerm.institutionCode,
      DwcTerm.institutionID);
  private final ObjectMapper mapper;
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
      openDsRecords.values().stream()
          .filter(openDS -> openDS.getAuthoritative().getMaterialType().equals("PreservedSpecimen"))
          .forEach(kafkaService::sendMessage);
    } catch (IOException e) {
      log.error("Failed to open output stream for download file", e);
    } catch (ExecutionException e) {
      log.error("Failed during downloading file with exception", e.getCause());
    } catch (InterruptedException e) {
      log.error("Failed during downloading file due to interruption", e);
      Thread.currentThread().interrupt();
    }
  }

  private Map<String, OpenDSWrapper> mapToOpenDS(Path file) {
    try {
      var archive = DwcFiles.fromCompressed(file, Path.of(dwcaProperties.getTempFolder()));
      var openDsRecords = new HashMap<String, OpenDSWrapper>();
      extractCoreFile(archive, openDsRecords);
      extractExtensions(archive, openDsRecords);
      log.info("Total discovered records are: {}", openDsRecords.size());
      return openDsRecords;
    } catch (IOException e) {
      log.error("Failed to open Dwca from temp folder, cannot process records");
      return Map.of();
    }
  }

  private void extractExtensions(Archive archive, Map<String, OpenDSWrapper> openDsRecords) {
    for (var extension : archive.getExtensions()) {
      log.info("Processing extension: {}", extension.getRowType().qualifiedName());
      for (var rec : extension) {
        extractedRecord(openDsRecords, extension, rec);
      }
    }
  }

  private void extractedRecord(Map<String, OpenDSWrapper> openDsRecords, ArchiveFile extension,
      Record rec) {
    var existingRecord = openDsRecords.get(rec.id());
    var unmapped = (ObjectNode) existingRecord.getUnmapped();
    var extensionUnmapped = mapper.createObjectNode();
    for (var term : rec.terms()) {
      if ((extension.getRowType().equals(GbifTerm.Multimedia) || extension.getRowType().equals(
          UnknownTerm.build("http://rs.tdwg.org/ac/terms/Multimedia", "Multimedia", true)))
          && term.equals(DcTerm.identifier)) {
        setImages(rec, existingRecord);
      } else {
        var value = rec.value(term);
        if (value != null) {
          extensionUnmapped.put(term.simpleName(), rec.value(term));
        }
      }
    }
    addUnmappedData(extension.getRowType().simpleName(), unmapped, extensionUnmapped);
  }


  private void addUnmappedData(String extensionName, ObjectNode unmapped,
      ObjectNode extensionUnmapped) {
    if (unmapped.get(extensionName) == null) {
      unmapped.set(extensionName, extensionUnmapped);
    } else {
      setUnmappedWithPrefix(extensionName, unmapped, extensionUnmapped, 1);
    }
  }

  private void setUnmappedWithPrefix(String extensionName, ObjectNode unmapped,
      ObjectNode extensionUnmapped, int i) {
    if (unmapped.get(extensionName + "_" + i) == null) {
      unmapped.set(extensionName + "_" + i, extensionUnmapped);
    } else {
      i = i + 1;
      setUnmappedWithPrefix(extensionName, unmapped, extensionUnmapped, i);
    }
  }

  private void setImages(Record rec, OpenDSWrapper existingRecord) {
    if (existingRecord.getImages() != null) {
      existingRecord.getImages()
          .add(Image.builder().imageUri(rec.value(DcTerm.identifier)).build());
    } else {
      var images = new ArrayList<Image>();
      images.add(Image.builder().imageUri(rec.value(DcTerm.identifier)).build());
      existingRecord.setImages(images);
    }
  }

  private void extractCoreFile(Archive archive, Map<String, OpenDSWrapper> openDsRecords) {
    for (var rec : archive.getCore()) {
      var unmapped = mapper.createObjectNode();
      addUnmappedTerms(archive, rec, unmapped);
      openDsRecords.put(rec.id(), buildOpenDS(rec, unmapped));
    }
  }

  private OpenDSWrapper buildOpenDS(Record rec, ObjectNode unmapped) {
    return OpenDSWrapper.builder()
        .authoritative(Authoritative.builder()
            .midslevel(1)
            .name(determineFullName(rec))
            .materialType(rec.value(DwcTerm.basisOfRecord))
            .physicalSpecimenId(rec.value(DwcTerm.catalogNumber))
            .curatedObjectID(rec.value(DwcTerm.occurrenceID))
            .institutionCode(rec.value(DwcTerm.institutionCode))
            .institution(rec.value(DwcTerm.institutionID))
            .build())
        .sourceId("translator-service")
        .unmapped(unmapped)
        .build();
  }

  private String determineFullName(Record rec) {
    if (rec.value(DwcTerm.scientificName) != null) {
      return rec.value(DwcTerm.scientificName);
    } else {
      return rec.value(DwcTerm.acceptedNameUsage);
    }
  }

  private void addUnmappedTerms(Archive archive, Record rec, ObjectNode unmapped) {
    for (Term term : archive.getCore().getFields().keySet()) {
      if (!MAPPED_TERMS.contains(term)) {
        var value = rec.value(term);
        if (value != null) {
          unmapped.put(term.simpleName(), value);
        }
      }
    }
  }

}
