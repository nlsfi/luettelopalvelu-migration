package fi.nls.codetransform.koodistotsuomi;

import java.util.List;
import java.util.Map;

public record CodeValue(
    String codeValue,
    Map<String, String> prefLabel,
    Map<String, String> description,
    Map<String, String> definition,
    String shortName,
    Status status,
    int order,
    String conceptUriInVocabularies,
    String startDate,
    String endDate,
    String broaderCode,
    String subCodeScheme,
    List<String> externalReferences) {

}
