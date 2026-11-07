package cfg;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Reads text files line by line. */
public final class SourceCodeReader {

    private SourceCodeReader() {
    }

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
