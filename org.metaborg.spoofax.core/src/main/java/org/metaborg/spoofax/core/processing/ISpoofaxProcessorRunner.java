package org.metaborg.spoofax.core.processing;

import javax.annotation.Nullable;

import org.metaborg.core.build.BuildInput;
import org.metaborg.core.processing.ICancellationToken;
import org.metaborg.core.processing.IProcessorRunner;
import org.metaborg.core.processing.IProgressReporter;
import org.metaborg.core.processing.ITask;
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnitUpdate;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;

/**
 * Typedef interface for {@link IProcessorRunner} with Spoofax interfaces.
 */
public interface ISpoofaxProcessorRunner extends
    IProcessorRunner<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ISpoofaxAnalyzeUnitUpdate, ISpoofaxTransformUnit<?>> {
    /**
     * {@inheritDoc}
     */
    ITask<ISpoofaxBuildOutput> build(BuildInput input, @Nullable IProgressReporter progressReporter,
        @Nullable ICancellationToken cancellationToken);
}
