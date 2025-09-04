package fi.nls.codetransform;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fi.nls.codetransform.koodistotsuomi.CodeScheme;
import fi.nls.codetransform.koodistotsuomi.CodeSchemeCodesResponse;
import fi.nls.codetransform.koodistotsuomi.CodeSchemeResponse;
import fi.nls.codetransform.koodistotsuomi.CodeValue;
import fi.nls.codetransform.koodistotsuomi.KoodistotSuomiFi;
import fi.nls.codetransform.koodistotsuomi.Status;
import fi.nls.codetransform.luettelopalvelu.Codelist;
import fi.nls.codetransform.luettelopalvelu.Luettelopalvelu;
import fi.nls.codetransform.transform.Configuration;
import fi.nls.codetransform.transform.Transformer;

public class Reload {

    public static void reload(KoodistotSuomiFi koodistot, List<Configuration> configs, int verbosity, boolean dryRun, boolean setValid) throws Exception {
        for (Configuration config : configs) {
            reload(koodistot, config, verbosity, dryRun, setValid);
        }
    }

    public static void reload(KoodistotSuomiFi koodistot, Configuration config, int verbosity, boolean dryRun, boolean setValid) throws Exception {
        String codeRegistry = config.codeRegistry();

        Map<String, Codelist> codelistsByLang = Luettelopalvelu.getCodelistByLang(config.codelist());

        CodeScheme codeScheme = Transformer.transformCodelist(codelistsByLang, config, Status.DRAFT, koodistot.getOrganizationId());
        List<CodeValue> codeValues = Transformer.transformCodes(codelistsByLang, config, Status.DRAFT);

        String codeSchemeIdentifier = codeScheme.codeValue();
        System.out.println(codeSchemeIdentifier);

        CodeSchemeResponse codeSchemeResponse;
        CodeSchemeCodesResponse codesResponse;

        if (verbosity > 0) {
            System.out.println("Deleting codeScheme");
        }
        if (!dryRun) {
            boolean deleteSucceeded = koodistot.deleteCodeScheme(codeRegistry, codeSchemeIdentifier);
            if (verbosity > 1) {
                if (deleteSucceeded) {
                    System.out.println("Deleted codeScheme");
                } else {
                    System.out.println("Failed to delete codeScheme");
                }
            }
        }

        if (verbosity > 0) {
            System.out.println("Adding codeScheme as DRAFT");
        }
        if (!dryRun) {
            codeSchemeResponse = koodistot.addOrUpdateCodeScheme(codeRegistry, codeScheme);
            if (verbosity > 1) {
                System.out.println("Response: " + codeSchemeResponse.toString());
            }
        }

        if (verbosity > 0) {
            System.out.println("Adding codeScheme codes as DRAFT");
        }
        if (!dryRun) {
            codesResponse = koodistot.addOrUpdateCodeSchemeCodes(codeRegistry, codeSchemeIdentifier, codeValues);
            if (verbosity > 1) {
                System.out.println("Response: " + codesResponse.toString());
            }
        }

        if (setValid) {
            codeValues = Transformer.transformCodes(codelistsByLang, config, Status.VALID);
            if (verbosity > 0) {
                System.out.println("Updating codeScheme codes status to VALID");
            }
            if (!dryRun) {
                codesResponse = koodistot.addOrUpdateCodeSchemeCodes(codeRegistry, codeSchemeIdentifier, codeValues);
                if (verbosity > 1) {
                    System.out.println("Response: " + codesResponse.toString());
                }
            }

            codeValues = Transformer.transformCodes(codelistsByLang, config, null).stream()
                .filter(v -> v.status() != Status.VALID)
                .collect(Collectors.toList());
            if (codeValues.size() > 0) {
                if (verbosity > 0) {
                    System.out.println("Updating certain codeScheme codes status to RETIRED/INVALID");
                }
                if (!dryRun) {
                    codesResponse = koodistot.addOrUpdateCodeSchemeCodes(codeRegistry, codeSchemeIdentifier, codeValues);
                    if (verbosity > 1) {
                        System.out.println("Response: " + codesResponse.toString());
                    }
                }
            } else if (verbosity > 1) {
                System.out.println("No codes in RETIRED/INVALID state");
            }
        } else if (verbosity > 1) {
            System.out.println("Not updating codeScheme codeScheme to VALID/RETIERD/INVALID as setValid is false");
        }


        if (setValid) {
            codeScheme = Transformer.transformCodelist(codelistsByLang, config, Status.VALID, koodistot.getOrganizationId());
            if (verbosity > 0) {
                System.out.println("Updating codeScheme status to VALID");
            }
            if (!dryRun) {
                codeSchemeResponse = koodistot.addOrUpdateCodeScheme(codeRegistry, codeScheme);
                if (verbosity > 1) {
                    System.out.println("Response: " + codeSchemeResponse.toString());
                }
            }
        } else if (verbosity > 1) {
            System.out.println("Not updating codeScheme status to VALID as setValid is false");
        }
    }

}
