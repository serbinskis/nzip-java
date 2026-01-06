import compression.deflate.Deflate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class Main {
    public static String ABOUT = "000RDB000 Jānis Programmētājs";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String sourceFile, resultFile, firstFile, secondFile;
        System.out.println("Enter command (comp, decomp, size, equal, about, gui, exit): ");

        while (true) {
            switch (sc.next()) {
                case "comp":
                    System.out.print("source file name: ");
                    sourceFile = sc.next();
                    System.out.print("archive name: ");
                    resultFile = sc.next();
                    comp(sourceFile, resultFile);
                    break;
                case "decomp":
                    System.out.print("archive name: ");
                    sourceFile = sc.next();
                    System.out.print("file name: ");
                    resultFile = sc.next();
                    decomp(sourceFile, resultFile);
                    break;
                case "size":
                    System.out.print("file name: ");
                    sourceFile = sc.next();
                    size(sourceFile);
                    break;
                case "equal":
                    System.out.print("first file name: ");
                    firstFile = sc.next();
                    System.out.print("second file name: ");
                    secondFile = sc.next();
                    System.out.println(equal(firstFile, secondFile));
                    break;
                case "about": about(); break;
                case "gui": Form.createWindow(); break;
                case "exit": System.exit(0); break;
            }
        }
    }

    public static void comp(String sourceFile, String resultFile) {
        try {
            String filename = Paths.get(sourceFile).getFileName().toString();
            byte[] data = Files.readAllBytes(Paths.get(sourceFile));
            data = Deflate.compress(data, progress -> System.out.printf(Locale.US, "\rCompressing %s: %.2f%%%s", filename, progress, (progress == 100 ? "\n" : "")));
            Files.write(Paths.get(resultFile), data);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void decomp(String sourceFile, String resultFile) {
        try {
            String filename = Paths.get(sourceFile).getFileName().toString();
            byte[] data = Files.readAllBytes(Paths.get(sourceFile));
            data = Deflate.decompress(data, progress -> System.out.printf(Locale.US, "\rDecompressing %s: %.2f%%%s", filename, progress, (progress == 100 ? "\n" : "")));
            Files.write(Paths.get(resultFile), data);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void size(String filename) {
        System.out.println("size: " + new File(filename).length());
    }

    public static boolean equal(String firstFile, String secondFile) {
        long f_size1 = new File(firstFile).length();
        long f_size2 = new File(secondFile).length();
        if (f_size1 != f_size2) { return false; }

        try {
            FileInputStream f1 = new FileInputStream(firstFile);
            FileInputStream f2 = new FileInputStream(secondFile);

            byte[] buffer1 = new byte[1024];
            byte[] buffer2 = new byte[1024];

            while ((f1.read(buffer1) != -1) && (f2.read(buffer2) != -1)) {
                if (!Arrays.equals(buffer1, buffer2)) { f1.close(); f2.close(); return false; }
            }

            f1.close();
            f2.close();
            return true;
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    public static void about() {
        System.out.println(ABOUT);
    }
}
