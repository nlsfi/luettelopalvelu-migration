package fi.nls.codetransform.koodistotsuomi;

import java.util.List;

public record CodeSchemeResponse(ResponseMeta meta, List<CodeScheme> results) {

}
