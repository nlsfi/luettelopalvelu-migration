package fi.nls.codetransform.koodistotsuomi;

import java.util.List;
import java.util.Map;

public record CodeScheme(
    String codeValue,
    Map<String, String> prefLabel,
    Map<String, String> description,
    Map<String, String> definition,
    Map<String, String> changeNote,
    String version,
    String source,
    String legalBase,
    String governancePolicy,
    Status status,
    Boolean cumulative,
    CodeValueReference codeRegistry,
    List<CodeValueReference> infoDomains,
    List<CodeValueReference> languageCodes,
    List<String> externalReferences,
    List<IdReference> organizations) {

}
