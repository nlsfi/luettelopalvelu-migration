package fi.nls.codetransform.koodistotsuomi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import fi.nls.codetransform.Http;

public class KoodistotSuomiFi {

    private static final ObjectMapper OM = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);

    private String publicApi;
    private String intakeApi;
    private String apiKey;
    private String organizationId;

    public KoodistotSuomiFi(String baseUrl, String apiKey, String organizationId) {
        this.publicApi = baseUrl + "/codelist-api";
        this.intakeApi = baseUrl + "/codelist-intake";
        this.apiKey = apiKey;
        this.organizationId = organizationId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public List<CodeScheme> getCodeSchemes(String codeRegistry) throws Exception {
        String url = publicApi + "/api/v1/coderegistries/" + codeRegistry + "/codeschemes/";
        byte[] response = Http.get(url);
        return OM.readValue(response, CodeSchemeResponse.class).results();
    }

    public Optional<CodeScheme> getCodeScheme(String codeRegistry, String codeScheme) throws Exception {
        String url = publicApi + "/api/v1/coderegistries/" + codeRegistry + "/codeschemes/" + codeScheme;
        if (Http.head(url) == 404) {
            return Optional.empty();
        }
        byte[] response = Http.get(url);
        return Optional.of(OM.readValue(response, CodeScheme.class));
    }

    public CodeSchemeResponse addOrUpdateCodeScheme(String codeRegistry, CodeScheme codeScheme) throws Exception {
        byte[] payload = OM.writeValueAsBytes(Collections.singletonList(codeScheme));
        String url = intakeApi + "/api/v1/coderegistries/" + codeRegistry + "/codeschemes/";
        byte[] response = Http.post(url, payload, apiKey);
        return OM.readValue(response, CodeSchemeResponse.class);
    }

    public boolean deleteCodeScheme(String codeRegistry, String codeScheme) {
        String url = intakeApi + "/api/v1/coderegistries/" + codeRegistry + "/codeschemes/" + codeScheme;
        try {
            return Http.delete(url, apiKey);
        } catch (Exception e) {
            return false;
        }
    }

    public List<CodeValue> getCodeSchemeCodes(String codeRegistry, String codeScheme) throws Exception {
        String url = publicApi + "/api/v1/coderegistries/" + codeRegistry + "/codeschemes/" + codeScheme + "/codes";
        byte[] response = Http.get(url);
        return OM.readValue(response, CodeSchemeCodesResponse.class).results();
    }

    public CodeSchemeCodesResponse addOrUpdateCodeSchemeCodes(String codeRegistry, String codeScheme, List<CodeValue> codeValues) throws Exception {
        byte[] payload = OM.writeValueAsBytes(codeValues);
        String url = intakeApi + "/api/v1/coderegistries/" + codeRegistry + "/codeschemes/" + codeScheme + "/codes";
        byte[] response = Http.post(url, payload, apiKey);
        return OM.readValue(response, CodeSchemeCodesResponse.class);
    }

    public boolean deleteCodeSchemeCode(String codeRegistry, String codeScheme, String codeValue) throws Exception {
        String url = intakeApi + "/api/v1/coderegistries/" + codeRegistry + "/codeschemes/" + codeScheme + "/codes/" + codeValue;
        try {
            return Http.delete(url, apiKey);
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> tryDeleteAllCodeSchemesFromRegistry(String codeRegistry) throws Exception {
        List<String> deleted = new ArrayList<>();
        for (CodeScheme codeScheme : getCodeSchemes(codeRegistry)) {
            if (codeScheme.status() != Status.INCOMPLETE && codeScheme.status() != Status.DRAFT) {
                continue;
            }
            if (deleteCodeScheme(codeRegistry, codeScheme.codeValue())) {
                deleted.add(codeScheme.codeValue());
            }
        }
        return deleted;            
    }

}
