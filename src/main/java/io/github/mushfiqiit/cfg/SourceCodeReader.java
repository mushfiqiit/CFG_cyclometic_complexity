package io.github.mushfiqiit.cfg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Reads UTF-8 text files line by line. */
public final class SourceCodeReader {

    /** Prevents instantiation; this class only has static methods. */
    private SourceCodeReader() {
    }

    /**
     * Reads every line of a UTF-8 text file. Unlike {@link java.util.Scanner}, every failure is
     * reported: nothing is silently skipped.
     *
     * @param fileName path of the file, relative to the working directory unless absolute
     * @return the file's lines in order, without line terminators
     * @throws java.nio.file.NoSuchFileException if the file does not exist
     * @throws java.nio.charset.MalformedInputException if the file is not valid UTF-8 text
     * @throws IOException if the file cannot be read for any other reason, such as being a
     *         folder or not being readable
     */
    public static List<String> readLines(String fileName) throws IOException {
        return Files.readAllLines(Path.of(fileName), StandardCharsets.UTF_8);
    }
}
