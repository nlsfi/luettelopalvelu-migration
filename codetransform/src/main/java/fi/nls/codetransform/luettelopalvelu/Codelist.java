package fi.nls.codetransform.luettelopalvelu;

import java.util.List;

public record Codelist(String id, String language, LanguageString label, LanguageString definition, List<ContainedItem> containeditems) {

}
