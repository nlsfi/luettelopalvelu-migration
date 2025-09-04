package fi.nls.codetransform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import fi.nls.codetransform.koodistotsuomi.CodeScheme;
import fi.nls.codetransform.koodistotsuomi.CodeSchemeCodesResponse;
import fi.nls.codetransform.koodistotsuomi.CodeSchemeResponse;
import fi.nls.codetransform.koodistotsuomi.CodeValue;
import fi.nls.codetransform.koodistotsuomi.CodeValueReference;
import fi.nls.codetransform.koodistotsuomi.KoodistotSuomiFi;
import fi.nls.codetransform.koodistotsuomi.Status;
import fi.nls.codetransform.luettelopalvelu.Codelist;
import fi.nls.codetransform.luettelopalvelu.Luettelopalvelu;
import fi.nls.codetransform.transform.Configuration;
import fi.nls.codetransform.transform.Transformer;

public class Update {

    public static void update(KoodistotSuomiFi koodistot, List<Configuration> configs, int verbosity, boolean dryRun, boolean setValid) throws Exception {
        for (Configuration config : configs) {
            update(koodistot, config, verbosity, dryRun, setValid);
        }
    }

    private static void update(KoodistotSuomiFi koodistot, Configuration config, int verbosity, boolean dryRun, boolean setValid) throws Exception {
        String codeRegistry = config.codeRegistry();

        Map<String, Codelist> codelistsByLang = Luettelopalvelu.getCodelistByLang(config.codelist());

        CodeScheme codeScheme = Transformer.transformCodelist(codelistsByLang, config, Status.VALID, koodistot.getOrganizationId());
        List<CodeValue> codeValues = Transformer.transformCodes(codelistsByLang, config, null);

        String codeSchemeIdentifier = codeScheme.codeValue();
        System.out.println("Checking " + codeSchemeIdentifier);

        Optional<CodeScheme> maybeExisting = koodistot.getCodeScheme(codeRegistry, codeSchemeIdentifier);
        if (!maybeExisting.isPresent()) {
            Reload.reload(koodistot, config, verbosity, dryRun, setValid);
            // No need to check if codes differ
            return;
        }

        CodeScheme existing = maybeExisting.get();
        if (!equals(existing, codeScheme)) {
            if (codeScheme.status() != Status.INCOMPLETE && codeScheme.status() != Status.DRAFT && setValid) {
                if (verbosity > 0) {
                    System.out.println("Updating codeScheme " + existing.codeValue());
                }
                if (!dryRun) {
                    CodeSchemeResponse codeSchemeResponse = koodistot.addOrUpdateCodeScheme(codeRegistry, codeScheme);
                    if (verbosity > 1) {
                        System.out.println("Response: " + codeSchemeResponse.toString());
                    }
                }
            } else if (verbosity > 1) {
                System.out.println("Not Updating codeScheme " + existing.codeValue() + " as setValid is false");
            }
        }

        List<CodeValue> existingCodes = koodistot.getCodeSchemeCodes(codeRegistry, codeSchemeIdentifier);
        List<CodeValue> differingCodeValues = differingCodeValues(existingCodes, codeValues);
        for (CodeValue code : differingCodeValues) {
            if (code.status() != Status.INCOMPLETE && code.status() != Status.DRAFT && !setValid) {
                if (verbosity > 1) {
                    System.out.println("Not Updating codeValue " + code.codeValue() + " as setValid is false");
                }
                continue;
            }
            if (verbosity > 0) {
                System.out.println("Updating codeValue " + code.codeValue());
            }
            if (!dryRun) {
                CodeSchemeCodesResponse codesResponse = koodistot.addOrUpdateCodeSchemeCodes(codeRegistry, codeSchemeIdentifier, Arrays.asList(code));
                if (verbosity > 1) {
                    System.out.println("Response: " + codesResponse.toString());
                }
            }
        }
    }

    private static boolean equals(CodeScheme koodistot, CodeScheme luettelopalvelu) {
        return
            koodistot.status().equals(luettelopalvelu.status()) &&
            isEqualIgnoreOrder(koodistot.prefLabel(), luettelopalvelu.prefLabel()) &&
            isEqualIgnoreOrder(koodistot.description(), luettelopalvelu.description()) &&
            isEqualIgnoreOrder(koodistot.infoDomains(), luettelopalvelu.infoDomains(), Update::codeValueEqualsURI) &&
            isEqualIgnoreOrder(koodistot.organizations(), luettelopalvelu.organizations());
    }

    private static List<CodeValue> differingCodeValues(List<CodeValue> koodistot, List<CodeValue> luettelopalvelu) {
        List<CodeValue> differing = new ArrayList<>();
        for (CodeValue codeValue : luettelopalvelu) {
            Optional<CodeValue> maybeExistingCode = koodistot.stream().filter(x -> x.codeValue().equals(codeValue.codeValue())).findAny();
            if (!maybeExistingCode.isPresent() || !equals(codeValue, maybeExistingCode.get())) {
                differing.add(codeValue);
            }
        }
        return differing;
    }

    private static boolean equals(CodeValue luettelopalvelu, CodeValue koodistot) {
        return     
            koodistot.status().equals(luettelopalvelu.status()) &&
            koodistot.order() == luettelopalvelu.order() &&
            nullSafeEquals(koodistot.startDate(), luettelopalvelu.startDate()) &&
            nullSafeEquals(koodistot.endDate(), luettelopalvelu.endDate()) &&
            isEqualIgnoreOrder(koodistot.prefLabel(), luettelopalvelu.prefLabel()) &&
            isEqualIgnoreOrder(koodistot.description(), luettelopalvelu.description())
        ;
    }

    private static boolean isEqualIgnoreOrder(Map<String, String> a, Map<String, String> b) {
        if (a == null) {
            a = Collections.emptyMap();
        }
        if (b == null) {
            b = Collections.emptyMap();
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (String key : a.keySet()) {
            if (!a.get(key).equals(b.get(key))) {
                return false;
            }
        }
        return true;
    }

    private static <T> boolean isEqualIgnoreOrder(List<T> a, List<T> b) {
        if (a == null) {
            a = Collections.emptyList();
        }
        if (b == null) {
            b = Collections.emptyList();
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (T item : a) {
            if (!b.contains(item)) {
                return false;
            }
        }
        return true;
    }

    private static <T> boolean isEqualIgnoreOrder(List<T> a, List<T> b, BiPredicate<T, T> eq) {
        if (a == null) {
            a = Collections.emptyList();
        }
        if (b == null) {
            b = Collections.emptyList();
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (T item : a) {
            if (!contains(b, item, eq)) {
                return false;
            }
        }
        return true;
    }

    private static <T> boolean contains(List<T> list, T item, BiPredicate<T, T> eq) {
        return list.stream().anyMatch(x -> eq.test(item, x));
    }

    private static boolean codeValueEqualsURI(CodeValueReference a, CodeValueReference b) {
        return (a.codeValue() != null && b.codeValue() != null && a.codeValue().equals(b.codeValue()))
            || (a.uri() != null && a.uri().substring(a.uri().lastIndexOf('/') + 1).equals(b.codeValue()))
            || (b.uri() != null && b.uri().substring(b.uri().lastIndexOf('/') + 1).equals(a.codeValue()));
    }

    private static boolean nullSafeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

}
