package io.github.alopukhov.dare.clg.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
class TargetImportItem {
    @Setter
    private ClassLoader target;
    private final ImportItem importItem;
}


