/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.ecmascript;

import net.sourceforge.pmd.cpd.Tokenizer;
import net.sourceforge.pmd.lang.LanguagePropertyBundle;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.ecmascript.ast.EcmascriptParser;
import net.sourceforge.pmd.lang.ecmascript.cpd.EcmascriptTokenizer;
import net.sourceforge.pmd.lang.impl.SimpleLanguageModuleBase;

/**
 * Created by christoferdutz on 20.09.14.
 */
public class EcmascriptLanguageModule extends SimpleLanguageModuleBase {

    public static final String NAME = "Ecmascript";
    public static final String TERSE_NAME = "ecmascript";

    public EcmascriptLanguageModule() {
        super(LanguageMetadata.withId(TERSE_NAME).name(NAME).extensions("js")
                              .addDefaultVersion("ES6"),
              properties -> () -> new EcmascriptParser(properties));
    }

    public static EcmascriptLanguageModule getInstance() {
        return (EcmascriptLanguageModule) LanguageRegistry.PMD.getLanguageByFullName(NAME);
    }

    @Override
    public Tokenizer createCpdTokenizer(LanguagePropertyBundle bundle) {
        return new EcmascriptTokenizer();
    }
}
