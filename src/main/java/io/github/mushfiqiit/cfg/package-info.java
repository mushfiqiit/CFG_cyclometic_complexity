/**
 * Builds the control flow graph of a C function, works out its cyclomatic complexity, and
 * generates test cases that cover each outcome of its conditions.
 *
 * <p>The program reads a C source file ({@link io.github.mushfiqiit.cfg.SourceLoader}), lets the
 * user choose an analysis ({@link io.github.mushfiqiit.cfg.Menu}) and prints its report. The entry
 * point is {@link io.github.mushfiqiit.cfg.Main}.
 *
 * <p>Unless a method or constructor states otherwise, passing {@code null} for any of its
 * arguments, or a collection containing {@code null}, throws {@link NullPointerException}.
 */
package io.github.mushfiqiit.cfg;
