package clubflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDirFactory;

/**
 * Creates JUnit-managed temporary directories under the build directory for relative-path tests.
 * Keeping fixtures on the project drive also works when Windows puts the system temp directory
 * on a different drive, since relative paths cannot cross drive boundaries.
 */
public class ProjectTempDirectory implements TempDirFactory {
    @Override
    public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
            throws IOException {
        Path parent = Path.of("build", "test-temp").toAbsolutePath();
        Files.createDirectories(parent);
        return Files.createTempDirectory(parent, "clubflow-");
    }
}
