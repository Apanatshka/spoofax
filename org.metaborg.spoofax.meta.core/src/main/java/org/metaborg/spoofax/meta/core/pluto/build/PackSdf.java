package org.metaborg.spoofax.meta.core.pluto.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.metaborg.spoofax.meta.core.pluto.SpoofaxBuilder;
import org.metaborg.spoofax.meta.core.pluto.SpoofaxBuilderFactory;
import org.metaborg.spoofax.meta.core.pluto.SpoofaxBuilderFactoryFactory;
import org.metaborg.spoofax.meta.core.pluto.SpoofaxContext;
import org.metaborg.spoofax.meta.core.pluto.SpoofaxInput;
import org.metaborg.spoofax.meta.core.pluto.StrategoExecutor;
import org.metaborg.spoofax.meta.core.pluto.StrategoExecutor.ExecutionResult;
import org.metaborg.spoofax.meta.core.pluto.util.LoggingFilteringIOAgent;
import org.metaborg.util.file.FileUtils;
import org.strategoxt.tools.main_pack_sdf_0_0;
import org.sugarj.common.FileCommands;

import build.pluto.BuildUnit.State;
import build.pluto.output.None;
import build.pluto.stamp.LastModifiedStamper;

public class PackSdf extends SpoofaxBuilder<PackSdf.Input, None> {
    public static class Input extends SpoofaxInput {
        private static final long serialVersionUID = 2058684747897720328L;

        public final String sdfModule;
        public final String sdfArgs;


        public Input(SpoofaxContext context, String sdfModule, String sdfArgs) {
            super(context);
            this.sdfModule = sdfModule;
            this.sdfArgs = sdfArgs;
        }
    }


    public static SpoofaxBuilderFactory<Input, None, PackSdf> factory = SpoofaxBuilderFactoryFactory.of(PackSdf.class,
        Input.class);


    public PackSdf(Input input) {
        super(input);
    }


    @Override protected String description(Input input) {
        return "Pack SDF modules";
    }

    @Override public File persistentPath(Input input) {
        return context.depPath("pack-sdf." + input.sdfModule + ".dep");
    }

    @Override public None build(Input input) throws IOException {
        final String externalDef = context.settings.externalDef();
        if(externalDef != null) {
            final File externalDefFile = new File(externalDef);
            final File target =
                FileUtils.toFile(context.settings.getIncludeDirectory().resolveFile(input.sdfModule + ".def"));
            require(externalDefFile, LastModifiedStamper.instance);
            FileCommands.copyFile(externalDefFile, target, StandardCopyOption.COPY_ATTRIBUTES);
            provide(target);
            return None.val;
        }

        // requireBuild(CompileSpoofaxPrograms.factory, new CompileSpoofaxPrograms.Input(context));

        copySdf2();

        final File inputPath = FileUtils.toFile(context.settings.getSdfMainFile(input.sdfModule));
        final File outputPath = FileUtils.toFile(context.settings.getSdfCompiledDefFile(input.sdfModule));

        // TODO: put these includes in the parent POM, since they are always needed.
        final String syntaxInclude =
            context.settings.getSyntaxDirectory().exists() ? "-I "
                + context.settings.getSyntaxDirectory().getName().getPath() : "";
        final String libInclude =
            context.settings.getLibDirectory().exists() ? "-I "
                + context.settings.getLibDirectory().getName().getPath() : "";

        require(inputPath);

        final ExecutionResult result =
            StrategoExecutor.runStrategoCLI(StrategoExecutor.toolsContext(), main_pack_sdf_0_0.instance, "pack-sdf",
                new LoggingFilteringIOAgent(Pattern.quote("  including ") + ".*"), "-i", inputPath, "-o", outputPath,
                syntaxInclude, libInclude, input.sdfArgs);

        provide(outputPath);
        for(File required : extractRequiredPaths(result.errLog)) {
            require(required);
        }

        setState(State.finished(result.success));

        return None.val;
    }

    private List<File> extractRequiredPaths(String log) {
        final String prefix = "  including ";
        final String infix = " from ";

        List<File> paths = new ArrayList<>();
        for(String s : log.split("\\n")) {
            if(s.startsWith(prefix)) {
                String module = s.substring(prefix.length());
                int infixIndex = module.indexOf(infix);
                if(infixIndex < 0 && FileCommands.acceptableAsAbsolute(module)) {
                    paths.add(new File(s.substring(prefix.length())));
                } else if(infixIndex >= 0) {
                    String def = module.substring(infixIndex + infix.length());
                    if(FileCommands.acceptable(def))
                        paths.add(new File(def));
                }
            }
        }
        return paths;
    }

    /**
     * Copy SDF2 files from syntax/ to src-gen/syntax, to support projects that do not use SDF3.
     */
    private void copySdf2() {
        final File syntaxDir = FileUtils.toFile(context.settings.getSyntaxDirectory());
        final File genSyntaxDir = FileUtils.toFile(context.settings.getGenSyntaxDirectory());

        // TODO: identify sdf2 files using Spoofax core
        List<Path> srcSdfFiles = FileCommands.listFilesRecursive(syntaxDir.toPath(), new SuffixFileFilter("sdf"));
        for(Path p : srcSdfFiles) {
            require(p.toFile(), LastModifiedStamper.instance);
            File target =
                FileCommands.copyFile(syntaxDir, genSyntaxDir, p.toFile(), StandardCopyOption.COPY_ATTRIBUTES);
            provide(target);
        }
    }
}
