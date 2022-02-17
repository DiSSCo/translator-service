package eu.dissco.webflux.demo.service.webclients;

import eu.dissco.webflux.demo.Profiles;
import eu.dissco.webflux.demo.domain.DarwinCore;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@AllArgsConstructor
@Profile(Profiles.BIO_CASE)
public class BioCaseService implements WebClientInterface {

  private static final String START_AT = "startAt";
  private static final String LIMIT = "limit";

  private final WebClient webClient;

  private final WebClientProperties properties;

  private final XMLInputFactory factory;

  private final Configuration configuration;

  @Override
  public Stream<DarwinCore> retrieveData() {
    var recordList = new ArrayList<DarwinCore>();
    var uri = properties.getEndpoint();
    var templateProperties = getTemplateProperties();
    configuration.setNumberFormat("computer");
    var finished = false;
    while (!finished) {
      log.info("Currently at: {} still collecting...", recordList.size());
      StringWriter writer = fillTemplate(templateProperties);
      finished = webClient.get().uri(uri + properties.getQueryParams() + writer).retrieve()
          .bodyToMono(String.class).map(xml -> mapToDarwin(xml, recordList)).block();
      updateStartAtParameter(templateProperties);
    }
    log.info("Gathered: {} records from bioCase", recordList.size());
    return recordList.stream();
  }

  private StringWriter fillTemplate(Map<String, Object> templateProperties) {
    var writer = new StringWriter();
    try {
      var template = configuration.getTemplate("biocase-request.ftl");
      template.process(templateProperties, writer);
    } catch (IOException | TemplateException e) {
      log.error("Failed to retrieve template", e);
    }
    return writer;
  }

  private void updateStartAtParameter(Map<String, Object> templateProperties) {
    templateProperties.put(START_AT,
        ((int) templateProperties.get(START_AT) + (int) templateProperties.get(LIMIT)));
  }

  private Map<String, Object> getTemplateProperties() {
    var map = new HashMap<String, Object>();
    map.put("contentNamespace", properties.getContentNamespace());
    map.put(LIMIT, properties.getItemsPerRequest());
    map.put(START_AT, 0);
    return map;
  }

  private boolean mapToDarwin(String xml, ArrayList<DarwinCore> list) {
    var recordCount = 0;
    var organisation = "Unknown";
    try {
      var xmlEventReader = factory.createXMLEventReader(new StringReader(xml));
      while (xmlEventReader.hasNext()) {
        var element = xmlEventReader.nextEvent();
        if (isStartElement(element, "content")) {
          recordCount = Integer.parseInt(
              element.asStartElement().getAttributeByName(new QName("recordCount")).getValue());
        }
        if (isStartElement(element, "Organisation")) {
          organisation = retrieveOrganisation(xmlEventReader);
        }
        retrieveUnitData(list, xmlEventReader, element, organisation);
      }
    } catch (XMLStreamException e) {
      log.info("Error converting response tot XML", e);
    }
    return recordCount % properties.getItemsPerRequest() != 0;
  }

  private String retrieveOrganisation(XMLEventReader xmlEventReader) throws XMLStreamException {
    while (xmlEventReader.hasNext()) {
      var element = xmlEventReader.nextEvent();
      if (isStartElement(element, "Text")) {
        return getData(xmlEventReader);
      }
    }
    throw new XMLStreamException("Missing Organisation Text element");
  }

  private void retrieveUnitData(ArrayList<DarwinCore> list, XMLEventReader xmlEventReader,
      XMLEvent element, String organisation) {
    try {
      if (isStartElement(element, "Unit")) {
        list.add(getUnitProperties(xmlEventReader, organisation));
      }
    } catch (XMLStreamException e) {
      log.error("Failed to process Unit xml to object", e);
    }
  }

  private DarwinCore getUnitProperties(XMLEventReader xmlEventReader, String organisation)
      throws XMLStreamException {
    var darwinCore = DarwinCore.builder().institutionID(organisation);
    while (xmlEventReader.hasNext()) {
      var element = xmlEventReader.nextEvent();
      if (isStartElement(element, "UnitID")) {
        darwinCore.id(getData(xmlEventReader));
      }
      if (isStartElement(element, "FullScientificNameString")) {
        darwinCore.scientificName(getData(xmlEventReader));
      }
      if (isStartElement(element, "RecordBasis")) {
        darwinCore.basisOfRecord(getData(xmlEventReader));
      }
      if (isEndElement(element, "Unit")) {
        return darwinCore.build();
      }
    }
    throw new XMLStreamException("No data found in Unit XML");
  }

  private String getData(XMLEventReader xmlEventReader) throws XMLStreamException {
    return xmlEventReader.nextEvent().asCharacters().getData();
  }

  private boolean isStartElement(XMLEvent element, String field) {
    return element.isStartElement() && element.asStartElement().getName().getLocalPart()
        .equals(field);
  }

  private boolean isEndElement(XMLEvent element, String field) {
    return element.isEndElement() && element.asEndElement().getName().getLocalPart()
        .equals(field);
  }
}
