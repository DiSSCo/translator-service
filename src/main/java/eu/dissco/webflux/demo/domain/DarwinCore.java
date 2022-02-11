package eu.dissco.webflux.demo.domain;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DarwinCore {

  String id;
  String occurrenceID;
  String basisOfRecord;
  String scientificName;
  String institutionID;
  List<Image> images;
}
