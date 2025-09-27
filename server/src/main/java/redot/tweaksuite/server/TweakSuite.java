package redot.tweaksuite.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.bytebuddy.agent.ByteBuddyAgent;
import redot.tweaksuite.server.compile.SuiteCompiler;
import redot.tweaksuite.server.log.SuiteLogger;
import redot.tweaksuite.server.path.ClassPathWorker;
import redot.tweaksuite.server.path.ClassResolver;
import redot.tweaksuite.server.registry.PermRegistry;
import redot.tweaksuite.server.registry.ThreadRegistry;

import java.lang.instrument.Instrumentation;

@Getter
@RequiredArgsConstructor
public class TweakSuite {

    @Setter
    private ClassLoader baseClassLoader;
    private final SuiteLogger logger;
    private final SuiteCompiler compiler;
    private final PermRegistry permRegistry;
    private final ThreadRegistry threadRegistry;
    private final ClassPathWorker classPathWorker;
    private final Instrumentation instrumentation;

    public TweakSuite(SuiteLogger logger, ClassResolver resolver) {
        this.logger = logger;
        this.permRegistry = new PermRegistry();
        this.threadRegistry = new ThreadRegistry();
        this.compiler = new SuiteCompiler(this);
        this.baseClassLoader = this.getClass().getClassLoader();
        this.classPathWorker = new ClassPathWorker(this);

        logger.info("Installing ByteBuddy agent...");
        this.instrumentation = ByteBuddyAgent.install();

        logger.info("Resolving ClassPaths...");
        this.classPathWorker.resolve(resolver);
    }

}
