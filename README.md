# CFG

Control Flow Graph (CFG) is a graph representation of a program, where statements are represented by nodes and statement dependency or jumps are represented by directed edges.
This project aims to develop a program that will take a source code file in C as input and implement and generate its Control Flow Graph.

## Build and run

The project uses the Gradle Wrapper, so Gradle itself does not need to be installed. The first build downloads Gradle, JDK 17 and the JavaFX libraries for your OS. The only prerequisite is any Java 8 or newer on your `PATH`, which is what starts the wrapper.

macOS and Linux:

```bash
./gradlew build
./gradlew run --args="path/to/program.c" --console=plain
```

Windows:

```bat
gradlew.bat build
gradlew.bat run --args="path\to\program.c" --console=plain
```

Pass the C file to analyze with `--args`. Relative paths are resolved from the project root, because that is where Gradle runs the program; use an absolute path for a file elsewhere. Without `--args`, the program analyzes `test.txt` in the project root.

The program then asks you to choose an analysis:

- `1` prints the control flow graph and the cyclomatic complexity, then opens the graph window. Nodes are labelled with their line numbers; double-click a node to see its source line.
- `2` prints the test case data: the declared `int` variables with their test values, and the conditions to cover.

Line numbers in the output count from 1, as in an editor. For example, option 2 on `test.txt` prints:

```
Test Case Data
==============

Test input values
-----------------
  Variable  Value
  --------  -----
  a             0
  b             0
  c             0
  k             0

Conditions to cover
-------------------
  #  Line  Condition  Operator  Operands
  -  ----  ---------  --------  --------
  1     4  k>0        >         k, 0
  2     5  a>b        >         a, b
  3     8  a==b       ==        a, b
  4    11  a>c        >         a, c
```

Option 1 ends with both complexity results. They are worked out independently, from the graph and from the decision points (`if`, `else if`, `for` and `while`), and they agree for well-structured code:

```
Cyclomatic complexity
---------------------
  From the graph:            M = E - N + 2 = 19 - 16 + 2 = 5
  From the decision points:  M = D + 1 = 4 + 1 = 5
  Decision points: 1 if, 2 else ifs, 1 loop
```

The program expects one statement per line, with each block opened at the end of its `if`, `else`, `for` or `while` line and closed by a `}` on its own line. Blank lines are fine.

Run `./gradlew run --args="--help"` to see the usage message.

## Testing

```bash
./gradlew test           # runs the automated tests
./gradlew build          # compiles, runs the tests and packages the program
./gradlew knownBugTest   # runs the tests for known bugs, which fail until those bugs are fixed
```

The tests live in `src/test/java` and use JUnit 5. Most of them pin down the program's current behavior, so a change that alters the graph, the counts or the printed reports makes them fail. Tests for bugs that are not fixed yet are tagged `known-bug`; they state the correct behavior, are left out of `./gradlew test`, and run only with `./gradlew knownBugTest`. When a bug is fixed, remove the tag from its test. There are currently no known-bug tests.

The test programs are in `src/test/resources/cfg`. The tests do not use `test.txt`, so you can change it freely. HTML reports are written to `build/reports/tests/`.

GitHub Actions runs `./gradlew build` on Linux, macOS and Windows for every push and pull request, and reports the known-bug tests without failing the workflow (`.github/workflows/build.yml`).

### Running without Gradle

To run the program from any directory, with paths relative to where you are, build a launcher script once. Unlike `./gradlew run`, the script does not download a JDK: it needs Java 17 or newer, found through `JAVA_HOME` or your `PATH`.

```bash
./gradlew installDist
build/install/CFG_cyclometic_complexity/bin/CFG_cyclometic_complexity path/to/program.c
```

On Windows, run `build\install\CFG_cyclometic_complexity\bin\CFG_cyclometic_complexity.bat` instead.
