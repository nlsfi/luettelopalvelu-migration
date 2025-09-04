package fi.nls.codetransform.luettelopalvelu;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import fi.nls.codetransform.Http;

public class Luettelopalvelu {

    private static final String BASE_URL = "https://luettelopalvelu.fi/codelist";
    private static final ObjectMapper OM = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
    
    private static final List<String> LANGUAGES = Arrays.asList("fi", "en", "sv");

    public static Map<String, Codelist> getCodelistByLang(String codelist) throws Exception {
        Map<String, Codelist> codelistByLang = new LinkedHashMap<>();
        for (String lang : LANGUAGES) {
            String url = BASE_URL + "/" + codelist + "/" + codelist + "." + lang + ".json";
            byte[] bytes = Http.get(url);
            CodelistFile codelistFile = OM.readValue(bytes, CodelistFile.class);
            codelistByLang.put(lang, codelistFile.codelist());
        }
        return codelistByLang;
    }

}
