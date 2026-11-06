# CFG

Control Flow Graph (CFG) is a graph representation of a program, where statements are represented by nodes and statement dependency or jumps are represented by directed edges.
This project aims to develop a program that will take a source code file in C as input and implement and generate its Control Flow Graph.

## Build and run

The project uses the Gradle Wrapper, so Gradle itself does not need to be installed. The first build downloads Gradle, JDK 17 and the JavaFX libraries for your OS. The only prerequisite is any Java 8 or newer on your `PATH`, which is what starts the wrapper.

macOS and Linux:

```bash
./gradlew build
./gradlew run --console=plain
```

Windows:

```bat
gradlew.bat build
gradlew.bat run --console=plain
```

`run` analyzes `test.txt` in the project root and asks you to choose an option:

- `1` prints the graph and cyclomatic complexity, then opens the graph window. This rewrites `Edges.txt` and `LeveledNodes.txt`.
- `2` prints the variables and conditions it found.
