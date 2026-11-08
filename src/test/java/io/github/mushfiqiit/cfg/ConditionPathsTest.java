package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks which conditions lead to each condition, and with which outcome. */
class ConditionPathsTest {

    /** Describes each site as "line: condition <- path", with each step as "+cond" (true) or "-cond" (false). */
    private static List<String> paths(List<String> lines) {
        return ConditionPaths.find(lines).stream().map(site -> site.lineNumber() + ": " + site.condition().getText()
                + " <- " + String.join(" ", site.path().stream()
                        .map(step -> (step.outcome() ? "+" : "-") + step.condition().getText()).toList()))
                .toList();
    }

    @Test
    void sampleConditionsAreInsideTheLoopAndAfterTheEarlierBranches() {
        assertEquals(List.of(
                "4: k>0 <- ",
                "5: a>b <- +k>0",
                "8: a==b <- +k>0 -a>b",
                "11: a>c <- +k>0 -a>b -a==b"), paths(program("sample.c")));
    }

    @Test
    void elseBlockRequiresEveryConditionInItsChainToBeFalse() {
        assertEquals(List.of(
                "2: a <- ",
                "4: b <- -a",
                "7: c <- -a -b"), paths(List.of(
                "int main(void) {",
                "    if (a) {",
                "    }",
                "    else if (b) {",
                "    }",
                "    else {",
                "        if (c) {",
                "        }",
                "    }",
                "}")));
    }

    @Test
    void forLoopsAndFinishedChainsAddNoRequirements() {
        assertEquals(List.of(
                "2: a <- ",
                "5: b <- ",
                "6: c <- +b"), paths(List.of(
                "int main(void) {",
                "    if (a) {",
                "    }",
                "    for (int i = 0; i < 3; i++) {",
                "        if (b) {",
                "            while (c) {",
                "            }",
                "        }",
                "    }",
                "}")));
    }
}
