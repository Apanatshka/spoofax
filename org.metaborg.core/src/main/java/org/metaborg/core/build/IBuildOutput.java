package org.metaborg.core.build;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.core.transform.TransformResult;

/**
 * Output generated by a build.
 * 
 * @param <P>
 *            Type of parsed fragments.
 * @param <A>
 *            Type of analyzed fragments.
 * @param <T>
 *            Type of transformed fragments.
 */
public interface IBuildOutput<P, A, T> {
    /**
     * @return If the build was successful.
     */
    boolean success();

    /**
     * @return State produced by the build.
     */
    BuildState state();

    /**
     * @return Resources that were removed prior to the build.
     */
    Set<FileName> removedResources();

    /**
     * @return Resources that were included into the build, but not used as source files. These files were parsed and
     *         analyzed, but not transformed.
     */
    Set<FileName> includedResources();

    /**
     * @return Resources that were added or changed prior to the build.
     */
    Iterable<FileObject> changedResources();

    /**
     * @return Parse results for changed resources. If parsing fails exceptionally by a {@link ParseException} or
     *         {@link IOException}, there is no parse result for that resource.
     */
    Iterable<ParseResult<P>> parseResults();

    /**
     * @return Analysis results for changed resources. Resources that could not be parsed are not analyzed. Resources in
     *         the same context are analyzed together and create a single analysis result.
     */
    Iterable<AnalysisResult<P, A>> analysisResults();

    /**
     * @return Transformation results for changed resources. Resources that could not be parsed or analyzed, or that do
     *         not require compilation, are not transformed.
     */
    Iterable<TransformResult<A, T>> transformResults();

    /**
     * @return Extra messages generated by exceptions.
     */
    Iterable<IMessage> extraMessages();

    /**
     * @return All messages. Includes parse, analysis, transformation, and extra messages.
     */
    Iterable<IMessage> allMessages();
}