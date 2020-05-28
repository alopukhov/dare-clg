package io.github.alopukhov.dare.clg.impl;

import io.github.alopukhov.dare.clg.ClassLoaderNode;
import lombok.NonNull;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ListAssert;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertClassLoader extends AbstractObjectAssert<AssertClassLoader, ClassLoader> {
    public AssertClassLoader(ClassLoader classLoader) {
        super(classLoader, AssertClassLoader.class);
    }

    public static AssertClassLoader assertClassLoader(ClassLoader classLoader) {
        return new AssertClassLoader(classLoader);
    }

    public static AssertClassLoader assertClassLoader(ClassLoaderNode node) {
        return new AssertClassLoader(node.getClassLoader());
    }

    public AssertClassLoader doesNotHaveClass(@NonNull String className) {
        try {
            actual.loadClass(className);
            failWithMessage("Expected classloader does not contain class <%s>", className);
        } catch (ClassNotFoundException ignore) {
        }
        return this;
    }

    public AssertClassLoader doesNotHaveClasses(String... classNames) {
        for (String className : classNames) {
            doesNotHaveClass(className);
        }
        return this;
    }

    public AssertClassLoader hasClass(@NonNull String className) {
        try {
            actual.loadClass(className);
        } catch (ClassNotFoundException ignore) {
            failWithMessage("Expected classloader contains class <%s>", className);
        }
        return this;
    }

    public AssertClassLoader hasClasses(String... classNames) {
        for (String className : classNames) {
            hasClass(className);
        }
        return this;
    }

    public AbstractObjectAssert<?, Object> classInstance(@NonNull String className) throws ReflectiveOperationException {
        isNotNull();
        return assertThat(requiredInstance(className));
    }

    public ListAssert<Object> classInstances(String... classNames) throws ReflectiveOperationException {
        isNotNull();
        List<Object> instances = new ArrayList<>(classNames.length);
        for (String className : classNames) {
            instances.add(requiredInstance(className));
        }
        return assertThat(instances);
    }


    private Object requiredInstance(String className) throws ReflectiveOperationException {
        Class<?> clazz = null;
        try {
            clazz = actual.loadClass(className);
        } catch (ClassNotFoundException e) {
            failWithMessage("Expected classloader to contain class <%s>", className);
        }
        assert clazz != null;
        return clazz.getConstructor().newInstance();
    }
}
