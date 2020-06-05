import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HelloWorld {
    private static final String TEXT = "Dare to say 'hello world'?";

    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            try (PrintStream ps = new PrintStream(Files.newOutputStream(Paths.get(args[0])))){
                ps.println(TEXT);
            }
        } else {
            System.out.println(TEXT);
        }
    }
}
