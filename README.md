# CFG

A command-line tool that analyzes one C function. It reads a C source file and offers two analyses:

- **Control flow graph and cyclomatic complexity.** It builds the function's control flow graph, where each source line is a node and each edge leads to a line that can run right after it. It prints the graph as a table and as an adjacency matrix, works out the cyclomatic complexity in two independent ways, and opens a window that draws the graph.
- **Test case data.** It lists the function's `int` variables and the conditions of its `if`, `else if` and `while` statements. Then it generates a test case for each outcome of each condition: input values that reach the condition and make it true, or false.

## Build and run

The project uses the Gradle Wrapper, so Gradle itself does not need to be installed. The first build downloads Gradle and the JavaFX libraries for your OS, and JDK 17 too if it is not already installed. The only prerequisite is any Java 8 or newer on your `PATH`, which is what starts the wrapper.

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

Pass the C file to analyze with `--args`. Relative paths are resolved from the project root, because that is where Gradle runs the program; use an absolute path for a file elsewhere. Without `--args`, the program analyzes `test.txt` in the project root. The file must be UTF-8 text.

The program first checks that the file uses the [supported layout](#supported-layout), then asks you to choose an analysis:

- `1` prints the control flow graph and the cyclomatic complexity, then opens the graph window.
- `2` prints the test case data.

The menu accepts only `1` or `2` and asks again after any other answer. Line numbers in the output count from 1, as in an editor.

Run `./gradlew run --args="--help"` to see the usage message. The program exits with status 0 on success, 1 if the file cannot be read or analyzed or no analysis is chosen, and 2 if the command-line arguments are invalid.

## Option 1: control flow graph and cyclomatic complexity

For `test.txt`, option 1 prints:

```
Cyclomatic Complexity
=====================
Entry point: line 2: int main(){

Control flow graph
------------------
  Each row is a source line, followed by the lines that can run right after it.

  Line  Statement                       Next lines
  ----  ------------------------------  ----------
     2  int main(){                     3
     3  int a, b, c, k;                 4
     4  while(k>0){                     5, 16
     5  if(a>b){                        6, 8
     6  printf("a is bigger than b");   7
     7  }                               14
     8  else if(a==b){                  9, 11
     9  printf("a and b is equal");     10
    10  }                               14
    11  else if(a>c) {                  12, 14
    12  printf("a is smaller than b");  13
    13  }                               14
    14  k--;                            15
    15  }                               4
    16  return 0;                       17
    17  }                               (end)
```

It then prints the same edges as an adjacency matrix, and ends with the cyclomatic complexity:

```
Cyclomatic complexity
---------------------
  From the graph:            M = E - N + 2 = 19 - 16 + 2 = 5
  From the decision points:  M = D + 1 = 4 + 1 = 5
  Decision points: 1 if, 2 else ifs, 1 loop

Opening the graph window. Close it to exit.
```

The two results are worked out independently: one from the edges and nodes of the graph, the other by counting the decision points (`if`, `else if`, `for` and `while`). A plain `else` is not a decision point. The two agree for code in the supported layout; if they ever differ, the report adds a warning that the graph may not match the code.

In the graph window, each node is labelled with its line number. Double-click a node to see its source line, and double-click again to go back to the number. Drag a node to move it. The program ends when you close the window.

## Option 2: test case data

For `test.txt`, option 2 prints:

```
Test Case Data
==============

Variables
---------
  Variable  Value
  --------  -----
  a         input
  b         input
  c         input
  k         input

Conditions to cover
-------------------
  #  Line  Condition  Operator  Operands
  -  ----  ---------  --------  --------
  1     4  k>0        >         k, 0
  2     5  a>b        >         a, b
  3     8  a==b       ==        a, b
  4    11  a>c        >         a, c

Test cases
----------
  Each condition is made true in one test case and false in another, using inputs
  that reach it. Inputs are the values at the start of the function; assignments
  made before a condition is checked are not taken into account.

  #  Covers        Outcome  Inputs
  -  ------------  -------  -------------------
  1  line 4: k>0   true     a=0, b=0, c=0, k=1
  2  line 4: k>0   false    a=0, b=0, c=0, k=0
  3  line 5: a>b   true     a=1, b=0, c=0, k=1
  4  line 5: a>b   false    a=0, b=0, c=0, k=1
  5  line 8: a==b  true     a=0, b=0, c=0, k=1
  6  line 8: a==b  false    a=0, b=1, c=0, k=1
  7  line 11: a>c  true     a=0, b=1, c=-1, k=1
  8  line 11: a>c  false    a=0, b=1, c=0, k=1

  Values found for 8 of 8 condition outcomes.
```

Each test case gives values for the inputs such that the condition is reached and has the stated outcome. For example, case 6 needs `k > 0` to enter the loop and `a > b` to be false before `a == b` is checked, and makes `a == b` false. The search tries small numbers (−2 to 2) and the numbers next to those in the conditions, such as 8 for `a > 7`, preferring values closest to 0.

### Variables

Only `int` variables are analyzed, including those declared in a `for` loop's initializer. Each variable is one of these:

| Kind | Example | Shown as | In test cases |
|---|---|---|---|
| Input | `int a;` | `input` | Chosen by the test case |
| Fixed | `int n = 3;` | `3 (set when declared)` | Keeps its value |
| Computed | `int s = a + 1;`, or a `for` loop counter | `computed in the code` | Cannot be chosen |
| Undeclared | used in a condition but never declared | `input (used but not declared)` | Chosen by the test case |

Values are those at the start of the function: assignments made later, such as `a = 5;` before a condition, are not taken into account.

### Conditions

The conditions of `if`, `else if` and `while` statements are covered. The condition of a `for` loop is not. A condition can be solved when it has one of these forms, where each operand is a variable or an integer that fits in an `int`:

| Form | Example | Read as |
|---|---|---|
| A comparison with `<`, `<=`, `>`, `>=`, `==` or `!=` | `a <= -5` | `a <= -5` |
| A single operand | `flag` | `flag != 0` |
| A negated operand | `!flag` | `flag == 0` |

Any other condition, such as `(a + b) > 3`, `a > b && b > 0` or `f(x) > 0`, is listed with `-` for its operator and operands, and gets no values.

### When there are no values

A test case without values says why instead. The possible reasons are:

- `the condition is too complex to solve`
- `the condition on line N, which leads here, is too complex to solve`
- `depends on X, which is computed in the code`
- `this outcome contradicts a condition on the way here`, when the outcome cannot be reached
- `too many variables to search; no values were tried`, when the combinations to try would exceed 200,000

For example, this function has each kind of variable and each solvable form of condition:

```c
int main(void) {
    int a, n = 3;
    int s = a + 1;
    for (int i = 0; i < n; i++) {
        if (a) {
            a = 0;
        }
    }
    if (!flag) {
        a = 1;
    }
    if (s > 2) {
        a = 2;
    }
    return 0;
}
```

Option 2 reports:

```
Variables
---------
  Variable  Value
  --------  -----------------------------
  a         input
  n         3 (set when declared)
  s         computed in the code
  i         computed in the code
  flag      input (used but not declared)

Conditions to cover
-------------------
  #  Line  Condition  Operator  Operands
  -  ----  ---------  --------  --------
  1     5  a          -         a
  2     9  !flag      !         flag
  3    12  s > 2      >         s, 2

Test cases
----------
  ...

  #  Covers          Outcome  Inputs
  -  --------------  -------  ------------------------------------------------------
  1  line 5: a       true     a=1, flag=0
  2  line 5: a       false    a=0, flag=0
  3  line 9: !flag   true     a=0, flag=0
  4  line 9: !flag   false    a=0, flag=1
  5  line 12: s > 2  true     no values: depends on s, which is computed in the code
  6  line 12: s > 2  false    no values: depends on s, which is computed in the code

  Values found for 4 of 6 condition outcomes.
```

## Supported layout

The program analyzes one function per file, written with one statement per line. The function can have any return type:

```c
#include <stdio.h>

int main(void) {
    int a = 1;              // comments are ignored
    if (a > 0) {            // { ends each if, else if, else, for and while line
        a--;
    }                       // each } is on a line of its own
    else {
        a++;
    }
    return 0;
}
```

Blank lines, comments, lines starting with `#` before the function (indented or not), and initializers such as `int v[2] = {1, 2};` are fine.

Before showing the menu, the program checks the file. It refuses to analyze anything it would misread, such as `} else {` on one line, an `if` without braces, a `{` on its own line, `switch` or `do` blocks, a second function, or a file with no function at all. Each problem is listed with its line number, up to 10 problems. For example, for a file `else.c` with `} else {` on line 5:

```
Error: else.c cannot be analyzed, because it uses a layout this program does not support:
  line 5: put each } on a line of its own, and start the next block on a new line, as in:  }  then  else {
Supported layout: one function, one statement per line, { at the end of each if, else, for and while line, and each } on a line of its own.
```

## Testing

```bash
./gradlew test           # runs the automated tests
./gradlew build          # compiles, runs the tests and packages the program
./gradlew knownBugTest   # runs the tests for known bugs, which fail until those bugs are fixed
```

The tests live in `src/test/java` and use JUnit 5. Most of them pin down the program's current behavior, so a change that alters the graph, the counts or the printed reports makes them fail. Tests for bugs that are not fixed yet are tagged `known-bug`; they state the correct behavior, are left out of `./gradlew test`, and run only with `./gradlew knownBugTest`. When a bug is fixed, remove the tag from its test. There are currently no known-bug tests.

The test programs are in `src/test/resources/io/github/mushfiqiit/cfg`. The tests do not use `test.txt`, so you can change it freely. HTML reports are written to `build/reports/tests/`.

GitHub Actions runs `./gradlew build` on Linux, macOS and Windows for every push and pull request, and reports the known-bug tests without failing the workflow (`.github/workflows/build.yml`).

### Running without Gradle

To run the program from any directory, with paths relative to where you are, build a launcher script once. Unlike `./gradlew run`, the script does not download a JDK: it needs Java 17 or newer, found through `JAVA_HOME` or your `PATH`.

```bash
./gradlew installDist
build/install/CFG_cyclometic_complexity/bin/CFG_cyclometic_complexity path/to/program.c
```

On Windows, run `build\install\CFG_cyclometic_complexity\bin\CFG_cyclometic_complexity.bat` instead.
