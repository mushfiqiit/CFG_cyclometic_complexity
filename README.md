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

The program then asks you to choose an option:

- `1` prints the graph and cyclomatic complexity, then opens the graph window. Double-click a node to see its source line.
- `2` prints the variables and conditions it found.

Run `./gradlew run --args="--help"` to see the usage message.

### Running without Gradle

To run the program from any directory, with paths relative to where you are, build a launcher script once. Unlike `./gradlew run`, the script does not download a JDK: it needs Java 17 or newer, found through `JAVA_HOME` or your `PATH`.

```bash
./gradlew installDist
build/install/CFG_cyclometic_complexity/bin/CFG_cyclometic_complexity path/to/program.c
```

On Windows, run `build\install\CFG_cyclometic_complexity\bin\CFG_cyclometic_complexity.bat` instead.
