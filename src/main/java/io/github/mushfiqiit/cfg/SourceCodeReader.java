package io.github.mushfiqiit.cfg;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Reads text files line by line. */
public final class SourceCodeReader {

    /** Prevents instantiation; this class only has static methods. */
    private SourceCodeReader() {
    }

    /**
     * Reads every line of a text file.
     *
     * @param fileName path of the file, relative to the working directory unless absolute
     * @return the file's lines in order, without line terminators
     * @throws FileNotFoundException if the file does not exist or cannot be opened
     */
    public static List<String> readLines(String fileName) throws FileNotFoundException {
        List<String> lines = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(fileName))) {
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
        }
        return lines;
    }
}
