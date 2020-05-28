package io.github.alopukhov.dare.clg.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
abstract class ImportItem {
    private static final Pattern VALID_WILDCARD = Pattern.compile("^[^*]*[*]{0,2}$");
    private static final char PACKAGE_NAME_DELIMITER = '.';
    private static final char RESOURCE_PATH_DELIMITER = '/';
    private static final String WILDCARD = "*";
    private static final String DOUBLE_WILDCARD = "**";

    public static ImportItem createClassImport(String importPath) {
        return createImportItem(importPath, PACKAGE_NAME_DELIMITER);
    }

    public static ImportItem createResourceImport(String importPath) {
        return createImportItem(importPath, RESOURCE_PATH_DELIMITER);
    }

    private static ImportItem createImportItem(String importPath, char delimeter) {
        validateImportPath(importPath);
        if (importPath.endsWith(DOUBLE_WILDCARD)) {
            return new DoubleWildcardImportItem(importPath.substring(0, importPath.length() - 2));
        } else if (importPath.endsWith(WILDCARD)) {
            return new WildcardImportItem(importPath.substring(0, importPath.length() - 1), delimeter);
        } else {
            return new RegularImportItem(importPath);
        }
    }

    abstract boolean accepts(String path);

    private static void validateImportPath(String importPath) {
        if (!VALID_WILDCARD.matcher(importPath).matches()) {
            throw new IllegalArgumentException("Bad import path " + importPath);
        }
    }

    @RequiredArgsConstructor
    private static class RegularImportItem extends ImportItem {
        private final String importPath;

        @Override
        boolean accepts(String path) {
            return importPath.equals(path);
        }
    }

    @RequiredArgsConstructor
    private static class WildcardImportItem extends ImportItem {
        private final String importPath;
        private final char delimiter;

        @Override
        boolean accepts(String path) {
            return path != null && path.startsWith(importPath) && path.lastIndexOf(delimiter) <= importPath.length();
        }
    }

    @RequiredArgsConstructor
    private static class DoubleWildcardImportItem extends ImportItem {
        private final String importPath;

        @Override
        boolean accepts(String path) {
            return path != null && path.startsWith(importPath);
        }
    }
}