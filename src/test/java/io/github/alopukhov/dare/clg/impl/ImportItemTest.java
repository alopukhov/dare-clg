package io.github.alopukhov.dare.clg.impl;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static io.github.alopukhov.dare.clg.impl.ImportItem.createClassImport;
import static io.github.alopukhov.dare.clg.impl.ImportItem.createResourceImport;
import static org.assertj.core.api.Assertions.assertThat;

public class ImportItemTest {
    @Test
    public void allClassImportItems_acceptsEqualPath() {
        assertClassImportItemAccept("java.util.List", "java.util.List");
        assertClassImportItemAccept("java.util.List*", "java.util.List");
        assertClassImportItemAccept("java.util.List**", "java.util.List");
    }

    @Test
    public void allResourceImportItems_acceptsEqualPath() {
        assertResourceImportItemAccept("foo/bar/baz", "foo/bar/baz");
        assertResourceImportItemAccept("foo/bar/baz*", "foo/bar/baz");
        assertResourceImportItemAccept("foo/bar/baz**", "foo/bar/baz");
    }

    @Test
    public void wildcardClassImport_acceptsPathsInSamePackage() {
        assertClassImportItemAccept("java.util.*", "java.util.List");
        assertClassImportItemAccept("java.util.*", "java.util.Queue");
        //yes "java.util." is valid path even if it is not valid classname.
        assertClassImportItemAccept("java.util.*", "java.util.");
        assertClassImportItemAccept("java.util.L*", "java.util.List");
        assertClassImportItemAccept("java.util.L*", "java.util.LinkedList");
    }

    @Test
    public void wildcardResourceImport_acceptsPathsInSameDirectory() {
        assertResourceImportItemAccept("foo/bar/*", "foo/bar/baz");
        assertResourceImportItemAccept("foo/bar/*", "foo/bar/xxx");
        assertResourceImportItemAccept("foo/bar/*", "foo/bar/");
        assertResourceImportItemAccept("foo/bar/b*", "foo/bar/bar");
        assertResourceImportItemAccept("foo/bar/b*", "foo/bar/baz");
    }

    @Test
    public void wildcardClassImport_rejectsPathsInSamePackageWithOtherPrefix() {
        assertClassImportItemRejects("java.util.Li*", "java.util.Deque");
        assertClassImportItemRejects("java.util.Li*", "java.util.Queue");
        assertClassImportItemRejects("java.util.Li*", "java.util.");
        assertClassImportItemRejects("java.util.Li*", "java.util.L");
        assertClassImportItemRejects("java.util.Li*", "java.util.Lp");
    }

    @Test
    public void wildcardResourceImport_rejectsPathsInSameDirectoryWithOtherPrefix() {
        assertResourceImportItemRejects("foo/bar/ba*", "foo/bar/foo");
        assertResourceImportItemRejects("foo/bar/ba*", "foo/bar/b");
        assertResourceImportItemRejects("foo/bar/ba*", "foo/bar/bq");
        assertResourceImportItemRejects("foo/bar/ba*", "foo/bar/f");
        assertResourceImportItemRejects("foo/bar/ba*", "foo/bar/");
        assertResourceImportItemRejects("foo/bar/ba*", "foo/bar");
    }

    @Test
    public void wildcardClassImport_rejectsPathsInOtherPackage() {
        assertClassImportItemRejects("java.util.*", "java.util.concurrent.ConcurrentMap");
        assertClassImportItemRejects("java.util.con*", "java.util.concurrent.ConcurrentMap");
        assertClassImportItemRejects("java.util.Li*", "java.List");
    }

    @Test
    public void wildcardImportResource_rejectsPathsInOtherDirectory() {
        assertResourceImportItemRejects("foo/bar/*", "foo/bar/foo/");
        assertResourceImportItemRejects("foo/bar/*", "foo/bar2/foo/");
        assertResourceImportItemRejects("foo/bar/ba*", "foo/baz/baz");
    }

    @Test
    public void doubleWildcardClassImport_acceptsSubdirectories() {
        assertClassImportItemAccept("**", "a");
        assertClassImportItemAccept("java.**", "java.util");
        assertClassImportItemAccept("java.**", "java.util.List");
        assertClassImportItemAccept("java.**", "java.util.concurrent.List");
        assertClassImportItemAccept("java.ut**", "java.util.List");
        assertClassImportItemAccept("java.ut**", "java.util.concurrent.List");
    }

    @Test
    public void wildcardsNotAsSuffix_causeIAE() {
        assertClassImportPathsCausesIAE("**.", "**.*", "**.**", "a.*.", "a.*a");
        assertResourceImportPathsCausesIAE("**.", "**/", "**/**", "a/*/", "a/*a");
    }

    @Test
    public void tooManyWildcards_causesIAE() {
        assertClassImportPathsCausesIAE("***", "java.***", "java.u***");
        assertResourceImportPathsCausesIAE("***", "foo/***", "foo/b***");
    }

    private static void assertClassImportItemAccept(String importPath, String testPath) {
        assertThat(createClassImport(importPath).accepts(testPath))
                .as("Class import item '%s' accepts '%s'", importPath, testPath)
                .isTrue();
    }

    private static void assertClassImportItemRejects(String importPath, String testPath) {
        assertThat(!createClassImport(importPath).accepts(testPath))
                .as("Class import item '%s' rejects '%s'", importPath, testPath)
                .isTrue();
    }

    private static void assertResourceImportItemAccept(String importPath, String testPath) {
        assertThat(createResourceImport(importPath).accepts(testPath))
                .as("Resource import item '%s' accepts '%s'", importPath, testPath)
                .isTrue();
    }

    private static void assertResourceImportItemRejects(String importPath, String testPath) {
        assertThat(!createResourceImport(importPath).accepts(testPath))
                .as("Resource import item '%s' rejects '%s'", importPath, testPath)
                .isTrue();
    }

    private static void assertClassImportPathsCausesIAE(String... importPaths) {
        for (String testImportPath : importPaths) {
            try {
                createClassImport(testImportPath);
                Assertions.fail("Expected import path '%s' to cause exception", testImportPath);
            } catch (IllegalArgumentException iae) {
                assertThat(iae).hasMessageContaining(testImportPath);
            }
        }
    }

    private static void assertResourceImportPathsCausesIAE(String... importPaths) {
        for (String testImportPath : importPaths) {
            try {
                createResourceImport(testImportPath);
                Assertions.fail("Expected import path '%s' to cause exception", testImportPath);
            } catch (IllegalArgumentException iae) {
                assertThat(iae).hasMessageContaining(testImportPath);
            }
        }
    }
}