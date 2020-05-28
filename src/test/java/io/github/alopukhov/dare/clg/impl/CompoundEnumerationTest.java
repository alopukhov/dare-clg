package io.github.alopukhov.dare.clg.impl;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;

public class CompoundEnumerationTest {
    @Test
    public void emptyIteratorTest() {
        // given
        Iterator<Enumeration<Object>> enumerations = Collections.emptyIterator();
        // when
        Enumeration<Object> enumeration = new CompoundEnumeration<>(enumerations);
        // then
        assertElements(enumeration, emptyList());
    }

    @Test
    public void singleEnumerationTest() {
        // given
        List<Enumeration<String>> enumerations = singletonList(enumerate("a", "b", "c", "d"));
        // when
        Enumeration<String> enumeration = new CompoundEnumeration<>(enumerations.iterator());
        // then
        assertElements(enumeration, asList("a", "b", "c", "d"));
    }

    @Test
    public void multipleEnumerationsTest() {
        // given
        List<Enumeration<String>> enumerations = asList(
                enumerate("a"),
                Collections.<String>emptyEnumeration(),
                enumerate("b","c"),
                Collections.<String>emptyEnumeration(),
                enumerate("d"));
        // when
        Enumeration<String> enumeration = new CompoundEnumeration<>(enumerations.iterator());
        // then
        assertElements(enumeration, asList("a", "b", "c", "d"));
    }

    @SuppressWarnings("unchecked")
    private static <T> Enumeration<T> enumerate(T... objects) {
        return enumeration(asList(objects));
    }

    private static <T> void assertElements(Enumeration<T> enumeration, List<T> expectedContent) {
        ArrayList<T> content = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            content.add(enumeration.nextElement());
        }
        assertThat(content).as("Enumeration's content").containsExactlyElementsOf(expectedContent);
        assertNextElementCauseException(enumeration);
    }

    private static <T> void assertNextElementCauseException(Enumeration<T> enumeration) {
        try {
            enumeration.nextElement();
            Assertions.fail("No more elements expected");
        } catch (NoSuchElementException e) {
            assertThat(e).isExactlyInstanceOf(NoSuchElementException.class);
        }
    }
}