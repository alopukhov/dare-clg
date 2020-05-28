package io.github.alopukhov.dare.clg.impl.validation;

import io.github.alopukhov.dare.clg.ClassLoaderGraphDefinition;
import io.github.alopukhov.dare.clg.ClassLoaderNodeDefinition;
import io.github.alopukhov.dare.clg.impl.ValidationResult;

import java.util.*;

import static io.github.alopukhov.dare.clg.impl.validation.DefaultValidationResult.success;

public class NoCycles {
    private static final String TEST_NAME = "Cycles detection";

    public ValidationResult validate(ClassLoaderGraphDefinition graph) {
        List<ClassLoaderNodeDefinition> cycle = findCycle(graph);
        if (cycle == null) {
            return success(TEST_NAME);
        }
        return new CycleDetected(cycle);
    }

    private List<ClassLoaderNodeDefinition> findCycle(ClassLoaderGraphDefinition graph) {
        Collection<ClassLoaderNodeDefinition> nodes = graph.getNodes();
        Map<ClassLoaderNodeDefinition, Color> colors = new IdentityHashMap<>(nodes.size());
        for (ClassLoaderNodeDefinition node : nodes) {
            if (colors.containsKey(node)) {
                continue;
            }
            for (ClassLoaderNodeDefinition curNode = node.getParent(); curNode != null; curNode = curNode.getParent()) {
                Color oldColor = colors.put(curNode, Color.GREY);
                if (oldColor == Color.GREY) {
                    return extractCycle(curNode);
                }
            }
            for (ClassLoaderNodeDefinition curNode = node.getParent(); curNode != null; curNode = curNode.getParent()) {
                colors.put(curNode, Color.BLACK);
            }

        }
        return null;
    }

    private static List<ClassLoaderNodeDefinition> extractCycle(ClassLoaderNodeDefinition node) {
        List<ClassLoaderNodeDefinition> cycle = new ArrayList<>();
        cycle.add(node);
        for (ClassLoaderNodeDefinition curNode = node.getParent(); curNode != node; curNode = curNode.getParent()) {
            cycle.add(curNode);
        }
        return cycle;
    }

    private static class CycleDetected extends DefaultValidationResult {
        private final List<ClassLoaderNodeDefinition> cycle;

        private CycleDetected(List<ClassLoaderNodeDefinition> cycle) {
            super(TEST_NAME, false);
            this.cycle = cycle;
        }

        @Override
        protected void describeDetails(StringBuilder to, String indent) {
            to.append(indent).append("Detected cycle:");
            for (ClassLoaderNodeDefinition node : cycle) {
                to.append("\n").append(indent).append(node.getName()).append(" <- ").append(node.getParent().getName());
            }
        }
    }

    private enum Color {
        GREY, BLACK
    }
}
