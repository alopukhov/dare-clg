package io.github.alopukhov.dare.clg.impl;

import lombok.RequiredArgsConstructor;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
class CompoundEnumeration<T> implements Enumeration<T> {
    private final Iterator<Enumeration<T>> enumerations;
    private Enumeration<T> current;

    @Override
    public boolean hasMoreElements() {
        return updateCurrent();
    }

    @Override
    public T nextElement() {
        if (!updateCurrent()) {
            throw new NoSuchElementException();
        }
        return current.nextElement();
    }

    private boolean updateCurrent() {
        for (;;) {
            if (current != null && current.hasMoreElements()) {
                return true;
            } else if (enumerations.hasNext()){
                current = enumerations.next();
            } else {
                current = null;
                return false;
            }
        }
    }
}
