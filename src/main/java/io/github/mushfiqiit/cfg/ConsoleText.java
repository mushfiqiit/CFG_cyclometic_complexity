package io.github.mushfiqiit.cfg;

/** Prints titles and section headings, so every report has the same layout. */
final class ConsoleText {

    /** Prevents instantiation; this class only has static methods. */
    private ConsoleText() {
    }

    /**
     * Prints a report title underlined with {@code =}.
     *
     * @param title the title to print
     */
    static void printTitle(String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("=".repeat(title.length()));
    }

    /**
     * Prints a section heading underlined with {@code -}, after a blank line.
     *
     * @param heading the heading to print
     */
    static void printSection(String heading) {
        System.out.println();
        System.out.println(heading);
        System.out.println("-".repeat(heading.length()));
    }

    /**
     * Shortens text to a maximum length, ending it with {@code ...} if it had to be cut.
     *
     * @param text the text to shorten
     * @param maxLength the longest result allowed; at least 4
     * @return the text unchanged if it fits, or its start followed by {@code ...}
     */
    static String shorten(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Formats a count with the singular or plural form of a word, such as {@code "1 loop"} or
     * {@code "2 loops"}.
     *
     * @param count the number of items
     * @param singular the word for one item
     * @param plural the word for any other number of items
     * @return the count followed by the matching word
     */
    static String count(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }
}
