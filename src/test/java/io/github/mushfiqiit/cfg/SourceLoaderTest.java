package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.print;
import static io.github.mushfiqiit.cfg.TestSupport.programPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks that a file is loaded when it can be analyzed, and that problems are reported otherwise. */
class SourceLoaderTest {

    @TempDir
    Path folder;

    private Optional<ParsedSource> loaded;
    private String errors;

    private String load(String path) {
        String[] err = new String[1];
        String output = print(out -> err[0] = print(errStream -> loaded = SourceLoader.load(path, out, errStream)));
        errors = err[0];
        return output;
    }

    @Test
    void loadsASupportedFileWithoutComments() throws IOException {
        Path file = folder.resolve("ok.c");
        Files.writeString(file, "int main(void) { // entry\n    return 0;\n}\n", StandardCharsets.UTF_8);

        String output = load(file.toString());

        assertEquals("Source file: " + file + " (3 lines)\n", output);
        assertEquals("", errors);
        assertEquals("int main(void) {", loaded.orElseThrow().lines().get(0));
        assertEquals(1, loaded.orElseThrow().function().orElseThrow().body().statements().size());
    }

    @Test
    void loadsTheSampleProgram() {
        load(programPath("sample.c").toString());

        assertEquals(17, loaded.orElseThrow().lines().size());
    }

    @Test
    void reportsAMissingFile() {
        String output = load(folder.resolve("nope.c").toString());

        assertEquals(Optional.empty(), loaded);
        assertEquals("", output);
        assertTrue(errors.startsWith("Error: cannot open source file: "), errors);
    }

    @Test
    void reportsAnUnsupportedLayoutAfterTheLineCount() throws IOException {
        Path file = folder.resolve("bad.c");
        Files.writeString(file, "int main(void) {\n    if (a) { b = 1; }\n}\n", StandardCharsets.UTF_8);

        String output = load(file.toString());

        assertEquals(Optional.empty(), loaded);
        assertEquals("Source file: " + file + " (3 lines)\n", output);
        assertTrue(errors.contains("  line 2: put each } on a line of its own"), errors);
    }
}
