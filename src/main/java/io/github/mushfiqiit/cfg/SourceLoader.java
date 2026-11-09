package io.github.mushfiqiit.cfg;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

/**
 * Loads a C source file for analysis: reads it, removes its comments, reads its structure and
 * checks that it uses the supported layout. Problems are reported instead of being thrown.
 */
final class SourceLoader {

    private static final int MAX_PROBLEMS_SHOWN = 10;

    /** Prevents instantiation; this class only has static methods. */
    private SourceLoader() {
    }

    // ***************************************************************************
    // Loading a file
    // ***************************************************************************

    /**
     * Loads a source file and prints its line count. If the file cannot be opened, or its layout
     * is not supported, prints why instead.
     *
     * @param sourceFile the file's path
     * @param out where to print the line count
     * @param err where to print problems
     * @return the parsed source, or an empty value if it cannot be analyzed
     */
    static Optional<ParsedSource> load(String sourceFile, PrintStream out, PrintStream err) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Path.of(sourceFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            err.println(describeReadFailure(sourceFile, e));
            return Optional.empty();
        }
        out.println("Source file: " + sourceFile + " (" + lines.size() + " lines)");

        ParsedSource source = ParsedSource.of(lines);
        List<SourceFormatChecker.Problem> problems = SourceFormatChecker.check(source);
        if (!problems.isEmpty()) {
            printProblems(err, sourceFile, problems);
            return Optional.empty();
        }
        return Optional.of(source);
    }

    // ***************************************************************************
    // Reporting problems
    // ***************************************************************************

    /**
     * Explains why a source file could not be read.
     *
     * @param sourceFile the file's path, as given
     * @param failure what went wrong
     * @return the error message to print
     */
    static String describeReadFailure(String sourceFile, IOException failure) {
        if (failure instanceof MalformedInputException) {
            return "Error: cannot read source file: " + sourceFile + " is not valid UTF-8 text";
        }
        String reason;
        if (failure instanceof NoSuchFileException) {
            reason = "No such file or directory";
        } else if (failure instanceof AccessDeniedException) {
            reason = "Permission denied";
        } else if (failure instanceof FileSystemException fileFailure && fileFailure.getReason() != null) {
            reason = fileFailure.getReason();
        } else {
            reason = failure.getMessage();
        }
        return "Error: cannot open source file: " + sourceFile + " (" + reason + ")";
    }

    /**
     * Prints why the source file cannot be analyzed, with at most {@value #MAX_PROBLEMS_SHOWN}
     * problems and a summary of the supported layout.
     *
     * @param err where to print
     * @param sourceFile the file's path, as given
     * @param problems the problems found
     */
    private static void printProblems(PrintStream err, String sourceFile, List<SourceFormatChecker.Problem> problems) {
        err.println("Error: " + sourceFile + " cannot be analyzed, because it uses a layout this program"
                + " does not support:");
        problems.stream().limit(MAX_PROBLEMS_SHOWN).forEach(problem -> err.println("  " + problem));
        if (problems.size() > MAX_PROBLEMS_SHOWN) {
            err.println("  ... and " + (problems.size() - MAX_PROBLEMS_SHOWN) + " more");
        }
        err.println("Supported layout: one function, one statement per line, { at the end of each if,"
                + " else, for and while line, and each } on a line of its own.");
    }
}
