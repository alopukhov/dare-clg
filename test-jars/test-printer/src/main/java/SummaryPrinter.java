import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Arrays.asList;

public class SummaryPrinter {
    private static final List<String> CLASSES_TO_TEST = asList("A", "B", "C", "Base", "Source");

    public static void main(String[] args) throws Exception {
        PrintStream out = System.out;
        boolean close = false;
        if (args.length == 1) {
            close = true;
            out = new PrintStream(Files.newOutputStream(Paths.get(args[0])));
        }
        try {
            printSummary(out);
        } finally {
            if (close) {
                out.close();
            }
        }
    }

    public static void printSummary(PrintStream out) throws Exception {
        for (String className : CLASSES_TO_TEST) {
            out.println(classSummary(className));
        }
    }

    private static String classSummary(String className) throws Exception {
        StringBuilder sb = new StringBuilder(50);
        sb.append(className).append(": ");
        Class<?> clazz = getClass(className);
        if (clazz == null) {
            sb.append("not found");
            return sb.toString();
        }
        sb.append("found;");
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            return sb.toString();
        }
        Method source, superSource;
        try {
            source = clazz.getMethod("getSource");
            superSource = clazz.getMethod("getSuperSource");
        } catch (NoSuchMethodException e) {
            return sb.toString();
        }
        Object obj = clazz.getConstructor().newInstance();
        sb.append(" source: ").append(source.invoke(obj))
                .append("; super-source: ").append(superSource.invoke(obj))
                .append(";");
        return sb.toString();
    }

    private static Class<?> getClass(String name) {
        try {
            return SummaryPrinter.class.getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}
