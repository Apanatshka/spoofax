package org.metaborg.spoofax.core.analysis.constraint;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.scopegraph.indices.TermIndex;
import org.metaborg.solver.ISolution;
import org.metaborg.solver.Solver;
import org.metaborg.solver.constraints.IConstraint;
import org.metaborg.spoofax.core.analysis.AnalysisCommon;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalyzeResults;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalyzer;
import org.metaborg.spoofax.core.analysis.SpoofaxAnalyzeResults;
import org.metaborg.spoofax.core.context.scopegraph.ISpoofaxScopeGraphContext;
import org.metaborg.spoofax.core.context.scopegraph.ISpoofaxScopeGraphUnit;
import org.metaborg.spoofax.core.stratego.IStrategoCommon;
import org.metaborg.spoofax.core.stratego.IStrategoRuntimeService;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.AnalyzeContrib;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnitUpdate;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.time.Timer;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.HybridInterpreter;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class ConstraintSingleFileAnalyzer extends AbstractConstraintAnalyzer implements ISpoofaxAnalyzer {

    public static final ILogger logger = LoggerUtils.logger(ConstraintSingleFileAnalyzer.class);
    public static final String name = "constraint-singlefile";

    private final ISpoofaxUnitService unitService;
    private final ResultBuilder resultBuilder;

    @Inject public ConstraintSingleFileAnalyzer(final AnalysisCommon analysisCommon,
            final ISpoofaxUnitService unitService, final IStrategoRuntimeService runtimeService,
            final IStrategoCommon strategoCommon, final ITermFactoryService termFactoryService,
            final ISpoofaxTracingService tracingService) {
        super(analysisCommon, runtimeService, strategoCommon, termFactoryService, tracingService);
        this.unitService = unitService;
        this.resultBuilder = new ResultBuilder();
    }


    @Override protected ISpoofaxAnalyzeResults analyzeAll(Map<String,ISpoofaxParseUnit> changed,
            Map<String,ISpoofaxParseUnit> removed, ISpoofaxScopeGraphContext context, HybridInterpreter runtime,
            String strategy) throws AnalysisException {
        for (String input : removed.keySet()) {
            context.removeUnit(input);
        }

        final Collection<ISpoofaxAnalyzeUnit> results = Lists.newArrayList();
        for (Map.Entry<String,ISpoofaxParseUnit> input : changed.entrySet()) {
            String source = input.getKey();
            ISpoofaxParseUnit parseUnit = input.getValue();

            try {
                ISpoofaxScopeGraphUnit unit = context.getOrCreateUnit(source);
                unit.reset();

                IStrategoTerm sourceTerm = termFactory.makeString(source);
                TermIndex.put(sourceTerm, source, 0);

                IStrategoTerm initialResultTerm = doAction(strategy, termFactory.makeAppl(analyzeInitial, sourceTerm),
                        context, runtime);
                InitialResult initialResult;
                initialResult = resultBuilder.initialResult(initialResultTerm);

                IStrategoTerm unitResultTerm = doAction(strategy,
                        termFactory.makeAppl(analyzeUnit, sourceTerm, parseUnit.ast(), initialResult.analysis), context,
                        runtime);
                UnitResult unitResult = resultBuilder.unitResult(unitResultTerm);

                IStrategoTerm finalResultTerm = doAction(strategy, termFactory.makeAppl(analyzeFinal, sourceTerm,
                        initialResult.analysis, termFactory.makeList(unitResult.analysis)), context, runtime);

                Iterable<IConstraint> constraints = Iterables2.from(initialResult.constraint, unitResult.constraint);
                logger.info(">>> Solving <<<");
                Timer t = new Timer(true);
                Solver solver = new Solver(constraints);
                ISolution solution = solver.solve();
                double time = ((double) t.stop()) / TimeUnit.SECONDS.toNanos(1);
                logger.info(">>> Done solving ({}s) <<<", time);

                FinalResult finalResult = resultBuilder.finalResult(finalResultTerm, solution);
                unit.setScopeGraph(finalResult.scopeGraph);
                unit.setNameResolution(finalResult.nameResolution);
                unit.setAnalysis(finalResult.analysis);
                applySolution(unit.processRawData(), finalResult.analysis, strategy, context, runtime);

                final Collection<IMessage> errors = analysisCommon.messages(parseUnit.source(), MessageSeverity.ERROR,
                        finalResult.errors);
                final Collection<IMessage> warnings = analysisCommon.messages(parseUnit.source(),
                        MessageSeverity.WARNING, finalResult.warnings);
                final Collection<IMessage> notes = analysisCommon.messages(parseUnit.source(), MessageSeverity.NOTE,
                        finalResult.notes);
                final Collection<IMessage> ambiguities = analysisCommon.ambiguityMessages(parseUnit.source(),
                        unitResult.ast);
                final Collection<IMessage> messages = Lists
                        .newArrayListWithCapacity(errors.size() + warnings.size() + notes.size() + ambiguities.size());
                messages.addAll(errors);
                messages.addAll(warnings);
                messages.addAll(notes);
                messages.addAll(ambiguities);

                results.add(unitService.analyzeUnit(parseUnit,
                        new AnalyzeContrib(true, errors.isEmpty(), true, unitResult.ast, messages, -1), context));
            } catch (MetaborgException e) {
                logger.warn("Skipping {}, because analysis failed\n{}", source, e);
            }
        }
        return new SpoofaxAnalyzeResults(results, Collections.<ISpoofaxAnalyzeUnitUpdate> emptyList(), context);
    }

}
