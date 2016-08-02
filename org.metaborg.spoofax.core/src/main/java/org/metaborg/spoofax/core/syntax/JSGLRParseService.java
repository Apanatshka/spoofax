package org.metaborg.spoofax.core.syntax;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageCache;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.spoofax.core.unit.ParseContrib;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.NullTokenizer;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class JSGLRParseService implements ISpoofaxParser, ILanguageCache {
    public static final String name = "jsglr";

    private static final ILogger logger = LoggerUtils.logger(JSGLRParseService.class);

    private final ISpoofaxUnitService unitService;
    private final ITermFactoryService termFactoryService;
    private final JSGLRParserConfiguration defaultParserConfig;

    private final Map<ILanguageImpl, IParserConfig> parserConfigs = Maps.newHashMap();


    @Inject public JSGLRParseService(ISpoofaxUnitService unitService, ITermFactoryService termFactoryService,
        JSGLRParserConfiguration defaultParserConfig) {
        this.unitService = unitService;
        this.termFactoryService = termFactoryService;
        this.defaultParserConfig = defaultParserConfig;
    }


    @Override public ISpoofaxParseUnit parse(ISpoofaxInputUnit input) throws ParseException {
        final FileObject source = input.source();
        final ILanguageImpl langImpl;
        final ILanguageImpl base;
        if(input.dialect() != null) {
            langImpl = input.dialect();
            base = input.langImpl();
        } else {
            langImpl = input.langImpl();
            base = null;
        }
        final String text = input.text();

        final ITermFactory termFactory = termFactoryService.get(langImpl, null, false);

        // WORKAROUND: JSGLR can't handle an empty input string, return empty tuple with null tokenizer.
        if(text == null || text.isEmpty()) {
            final IStrategoTerm emptyTuple = termFactory.makeTuple();
            final String filename;
            if(input.detached()) {
                filename = "";
            } else {
                filename = input.source().getName().getURI();
            }
            final ITokenizer tokenizer = new NullTokenizer("", filename);
            final IToken token = tokenizer.currentToken();
            ImploderAttachment.putImploderAttachment(emptyTuple, false, "", token, token);
            return unitService.parseUnit(input, new ParseContrib(emptyTuple));
        }

        final IParserConfig config = getParserConfig(langImpl);
        try {
            logger.trace("Parsing {}", source);
            final JSGLRI parser;
            if(base != null) {
                parser = new JSGLRI(config, termFactory, base, langImpl, source, text);
            } else {
                parser = new JSGLRI(config, termFactory, langImpl, null, source, text);
            }
            JSGLRParserConfiguration parserConfig = input.config();
            if(parserConfig == null) {
                parserConfig = defaultParserConfig;
            }
            final ParseContrib contrib = parser.parse(parserConfig);
            final ISpoofaxParseUnit unit = unitService.parseUnit(input, contrib);
            return unit;
        } catch(IOException e) {
            throw new ParseException(input, e);
        }
    }

    @Override public Collection<ISpoofaxParseUnit> parseAll(Iterable<ISpoofaxInputUnit> inputs) throws ParseException {
        final Collection<ISpoofaxParseUnit> parseUnits = Lists.newArrayList();
        for(ISpoofaxInputUnit input : inputs) {
            parseUnits.add(parse(input));
        }
        return parseUnits;
    }

    public IParserConfig getParserConfig(ILanguageImpl lang) {
        IParserConfig config = parserConfigs.get(lang);
        if(config == null) {
            final ITermFactory termFactory =
                termFactoryService.getGeneric().getFactoryWithStorageType(IStrategoTerm.MUTABLE);
            final SyntaxFacet facet = lang.facet(SyntaxFacet.class);
            final IParseTableProvider provider = new FileParseTableProvider(facet.parseTable, termFactory);
            config = new ParserConfig(Iterables.get(facet.startSymbols, 0), provider);
            parserConfigs.put(lang, config);
        }
        return config;
    }


    @Override public void invalidateCache(ILanguageImpl impl) {
        logger.debug("Removing cached parse table for {}", impl);
        parserConfigs.remove(impl);
    }

    @Override public void invalidateCache(ILanguageComponent component) {

    }
}
