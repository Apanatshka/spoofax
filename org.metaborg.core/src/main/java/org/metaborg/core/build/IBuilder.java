package org.metaborg.core.build;

import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.analysis.IAnalyzeUnitUpdate;
import org.metaborg.core.processing.ICancellationToken;
import org.metaborg.core.processing.IProgressReporter;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.transform.ITransformUnit;

/**
 * Incrementally parses, analyses, and compiles source files.
 * 
 * @param <P>
 *            Type of parse units.
 * @param <A>
 *            Type of analyze units.
 * @param <AU>
 *            Type of analyze unit updates.
 * @param <T>
 *            Type of transform units with any input.
 */
public interface IBuilder<P extends IParseUnit, A extends IAnalyzeUnit, AU extends IAnalyzeUnitUpdate, T extends ITransformUnit<?>> {
    /**
     * Parses, analyses, and compiles changed resources.
     * 
     * @param input
     *            Build input.
     * @return Result of building.
     * @throws InterruptedException
     *             When build is cancelled.
     * @throws MetaborgRuntimeException
     *             When {@code input.throwOnErrors} is set to true and errors occur.
     */
    IBuildOutput<P, A, AU, T> build(BuildInput input, IProgressReporter progressReporter,
        ICancellationToken cancellationToken) throws InterruptedException;

    /**
     * Cleans derived resources and contexts from given location.
     * 
     * @param input
     *            Clean input.
     * @throws InterruptedException
     *             When clean is cancelled.
     */
    void clean(CleanInput input, IProgressReporter progressReporter, ICancellationToken cancellationToken)
        throws InterruptedException;
}
