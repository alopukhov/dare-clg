package io.github.alopukhov.dare.clg.impl;

import io.github.alopukhov.dare.clg.ClassLoaderGraph;
import io.github.alopukhov.dare.clg.ClassLoaderGraphDefinition;
import io.github.alopukhov.dare.clg.UnpackedTestJarsRule;
import lombok.Cleanup;
import org.junit.ClassRule;
import org.junit.Test;

import static io.github.alopukhov.dare.clg.impl.AssertClassLoader.assertClassLoader;
import static org.assertj.core.groups.Tuple.tuple;

public class ClassLoaderGraphDefinitionImplTest {
    @ClassRule
    public static UnpackedTestJarsRule jars = new UnpackedTestJarsRule();

    @Test
    public void testThreeOrphanNodes() throws Exception {
        ClassLoaderGraphDefinition graphDefinition = new ClassLoaderGraphDefinitionImpl();
        graphDefinition.getOrCreateNode("a").addSource(jars.jarUrlA().toString());
        graphDefinition.getOrCreateNode("b").addSource(jars.jarUrlB().toString());
        graphDefinition.getOrCreateNode("c").addSource(jars.jarUrlC().toString());
        @Cleanup ClassLoaderGraph graph = graphDefinition.materialize();
        AssertClassLoader nodeA = assertClassLoader(graph.getNode("a"));
        AssertClassLoader nodeB = assertClassLoader(graph.getNode("b"));
        AssertClassLoader nodeC = assertClassLoader(graph.getNode("c"));
        nodeA.hasClasses("A", "B", "C");
        nodeB.doesNotHaveClass("A").hasClasses("B", "C");
        nodeC.doesNotHaveClasses("A", "B").hasClass("C");
        nodeA.classInstance("C").extracting("source", "superSource").containsOnly("jar-a");
        nodeB.classInstance("C").extracting("source", "superSource").containsOnly("jar-b");
        nodeC.classInstance("C").extracting("source", "superSource").containsOnly("jar-c");
    }

    @Test
    public void testDefaultClassInheritance() throws Exception {
        ClassLoaderGraphDefinition gd = new ClassLoaderGraphDefinitionImpl();
        gd.getOrCreateNode("a").addSource(jars.jarUrlA().toString()).addChild("b");
        gd.getOrCreateNode("b").addSource(jars.jarUrlB().toString());
        @Cleanup ClassLoaderGraph graph = gd.materialize();
        AssertClassLoader nodeA = assertClassLoader(graph.getNode("a"));
        AssertClassLoader nodeB = assertClassLoader(graph.getNode("b"));
        nodeA.hasClasses("A", "B", "C");
        nodeB.hasClasses("A", "B", "C");
        nodeA.classInstances("A", "B", "C").flatExtracting("source", "superSource").containsOnly("jar-a");
        nodeB.classInstances("A", "B", "C").flatExtracting("source", "superSource").containsOnly("jar-a");
    }

    @Test
    public void testSelfFirstClassInheritance() throws Exception {
        ClassLoaderGraphDefinition gd = new ClassLoaderGraphDefinitionImpl();
        gd.setDefaultLoadingStrategy("spi");
        gd.getOrCreateNode("a").addSource(jars.jarUrlA().toString()).addChild("b");
        gd.getOrCreateNode("b").addSource(jars.jarUrlB().toString());
        @Cleanup ClassLoaderGraph graph = gd.materialize();
        AssertClassLoader nodeA = assertClassLoader(graph.getNode("a"));
        AssertClassLoader nodeB = assertClassLoader(graph.getNode("b"));
        nodeA.hasClasses("A", "B", "C");
        nodeB.hasClasses("A", "B", "C");
        nodeA.classInstances("A", "B", "C").flatExtracting("source", "superSource").containsOnly("jar-a");
        nodeB.classInstance("A").extracting("source", "superSource").containsOnly("jar-a");
        nodeB.classInstances("B", "C").flatExtracting("source", "superSource").containsOnly("jar-b");
    }

    @Test
    public void testSimpleImport() throws Exception {
        ClassLoaderGraphDefinition gd = new ClassLoaderGraphDefinitionImpl();
        gd.getOrCreateNode("a").addSource(jars.jarUrlA().toString());
        gd.getOrCreateNode("b").addSource(jars.jarUrlB().toString()).addImportClasses("a", "A");
        @Cleanup ClassLoaderGraph graph = gd.materialize();
        AssertClassLoader nodeA = assertClassLoader(graph.getNode("a"));
        AssertClassLoader nodeB = assertClassLoader(graph.getNode("b"));
        nodeA.hasClasses("A", "B", "C");
        nodeB.hasClasses("A", "B", "C");
        nodeA.classInstances("A", "B", "C").flatExtracting("source", "superSource").containsOnly("jar-a");
        nodeB.classInstance("A").extracting("source", "superSource").containsOnly("jar-a");
        nodeB.classInstances("B", "C").flatExtracting("source", "superSource").containsOnly("jar-b");
    }

    @Test
    public void testImportBaseClass() throws Exception {
        ClassLoaderGraphDefinition gd = new ClassLoaderGraphDefinitionImpl();
        gd.getOrCreateNode("a").addSource(jars.jarUrlA().toString());
        gd.getOrCreateNode("b").addSource(jars.jarUrlB().toString()).addImportClasses("a", "Base");
        @Cleanup ClassLoaderGraph graph = gd.materialize();
        AssertClassLoader nodeA = assertClassLoader(graph.getNode("a"));
        AssertClassLoader nodeB = assertClassLoader(graph.getNode("b"));
        nodeA.hasClasses("A", "B", "C");
        nodeB.doesNotHaveClass("A").hasClasses("B", "C");
        nodeA.classInstances("A", "B", "C").flatExtracting("source", "superSource").containsOnly("jar-a");
        nodeB.classInstances("B", "C").extracting("source", "superSource")
                .containsOnly(tuple("jar-b", "jar-a"));
    }


}