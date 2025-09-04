package fi.nls.codetransform.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import fi.nls.codetransform.koodistotsuomi.CodeScheme;
import fi.nls.codetransform.koodistotsuomi.CodeValue;
import fi.nls.codetransform.koodistotsuomi.CodeValueReference;
import fi.nls.codetransform.koodistotsuomi.IdReference;
import fi.nls.codetransform.koodistotsuomi.Status;
import fi.nls.codetransform.luettelopalvelu.Codelist;
import fi.nls.codetransform.luettelopalvelu.ContainedItem;

public class Transformer {

    public static CodeScheme transformCodelist(Map<String, Codelist> codelistsByLang, Configuration config, Status status, String organizationId) {
        String codeSchemeIdentifier = codelistsByLang.values().stream().map(v -> Arrays.stream(v.id().split("/")).reduce((a, b) -> b).get()).findAny().get();
        Map<String, String> prefLabel  = codelistsByLang.values().stream()
            .collect(Collectors.toMap(codelist -> codelist.label().lang(), codelist -> codelist.label().text(), (a, b) -> a));
        Map<String, String> description = codelistsByLang.values().stream()
            .filter(codelist -> codelist.definition() != null)
            .collect(Collectors.toMap(codelist -> codelist.definition().lang(), codelist -> codelist.definition().text(), (a, b) -> a));
        Map<String, String> definition = null;
        Map<String, String> changeNote = null;
        String version = null;
        String source = null;
        String legalBase = null;
        String governancePolicy = null;
        Boolean cumulative = null;
        CodeValueReference codeRegistry = new CodeValueReference(config.codeRegistry(), null);
        List<CodeValueReference> infoDomains = Arrays.asList(config.infoDomain()).stream().map(x -> new CodeValueReference(x, null)).toList();
        List<CodeValueReference> languageCodes = codelistsByLang.keySet().stream().map(lang -> new CodeValueReference(lang, null)).toList();
        List<String> externalReferences = Collections.emptyList();
        List<IdReference> organizations = Arrays.asList(new IdReference(organizationId));

        return new CodeScheme(
            codeSchemeIdentifier,
            prefLabel,
            description,
            definition,
            changeNote,
            version,
            source,
            legalBase,
            governancePolicy,
            status,
            cumulative,
            codeRegistry,
            infoDomains,
            languageCodes,
            externalReferences,
            organizations
        );
    }

    private static Comparator<String> getCodeOrder(String howTo) {
        howTo = howTo.toLowerCase();
        switch (howTo) {
            case "number":
                return (a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
            case "string":
                return (a, b) -> a.compareTo(b);
            case "json":
                return null;
            default:
                throw new IllegalArgumentException("Unknown value to determine how to order codes");
        }
    }

    public static List<CodeValue> transformCodes(Map<String, Codelist> codelistsByLang, Configuration config, Status overrideStatus) {
        List<CodeValue> codeValues = new ArrayList<>();

        Comparator<String> orderCmp = getCodeOrder(config.codeOrderStrategy());
        Map<String, String> idToIdentifier = orderCmp == null ? new LinkedHashMap<>() : new TreeMap<>(orderCmp);

        for (Map.Entry<String, Codelist> entry : codelistsByLang.entrySet()) {
            Codelist codelist = entry.getValue();
            for (ContainedItem ci : codelist.containeditems()) {
                String id = ci.value().id();
                String identifier = id.substring(id.lastIndexOf('/') + 1);
                idToIdentifier.putIfAbsent(identifier, id);
            }
        }

        int order = 1;
        for (Map.Entry<String, String> entry : idToIdentifier.entrySet()) {
            String codeValueCode = entry.getKey();
            String id = entry.getValue();
            Map<String, String> prefLabel = new HashMap<>();
            Map<String, String> description = new HashMap<>();
            Map<String, String> definition = null;
            String shortName = null;
            MutableRef<Status> status = new MutableRef<>();
            status.item = overrideStatus;
            String conceptUriInVocabularies = null;
            String startDate = null;
            String endDate = null;
            String broaderCode = null;
            String subCodeScheme = null;
            List<String> externalReferences = new ArrayList<>();

            for (String lang : codelistsByLang.keySet()) {
                Codelist codelist = codelistsByLang.get(lang);
                codelist.containeditems().stream()
                    .map(ContainedItem::value)
                    .filter(item -> item.id().equals(id))
                    .findAny()
                    .ifPresent(v -> {
                        prefLabel.putIfAbsent(v.label().lang(), v.label().text());
                        if (v.description() != null) {
                            description.putIfAbsent(v.description().lang(), v.description().text());
                        }
                        if (status.item == null) {
                            status.item = mapStatus(v.status().id());
                        }
                    });
            }

            CodeValue codeValue = new CodeValue(
                codeValueCode,
                prefLabel,
                description,
                definition,
                shortName,
                status.item,
                order,
                conceptUriInVocabularies,
                startDate,
                endDate,
                broaderCode,
                subCodeScheme,
                externalReferences
            );
            codeValues.add(codeValue);

            order++;
        }

        return codeValues;
    }

    public static Status mapStatus(String id) {
        switch (id) {
            case "http://www.luettelopalvelu.fi/registry/status/valid":
                return Status.VALID;
            case "http://www.luettelopalvelu.fi/registry/status/retired":
                return Status.RETIRED;
            default:
                throw new IllegalArgumentException("Unknown status " + id);
        }
    }

}
