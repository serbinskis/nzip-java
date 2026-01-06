import compression.BitCarry;
import compression.CompressionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;


public class Test {
    public static void main(String[] args) throws IOException {
        testCompress(CompressionType.DEFLATE);
        //testBitCarry();
    }

    public static void testBitCarry() {
        BitCarry bitCarry = new BitCarry();
        //bitCarry.pushBytes(false, (byte) 0b10000011, (byte) 0b10000011);
        //bitCarry.pushBytes(false, (byte) 0xFF, (byte) 0xFF, (byte) 0b01000011);
        for (int i = 0; i < 1; i++) {
            bitCarry.pushBytes((byte) 0b10000001, (byte) 0b11101111, (byte) 0b01000011);
            bitCarry.pushBytes((byte) 0b00001111);
            bitCarry.pushBytes((byte) 0b00001111);
            bitCarry.pushBytes((byte) 0b00001111, (byte) 0b11111111, (byte) 0b01000011);
            bitCarry.pushBytes((byte) 0b00001111, (byte) 0b11111111, (byte) 0b01000011);
        }

        System.out.println();
        byte[] buffer = bitCarry.getBytes(true);

        for (byte b : buffer) {
            System.out.println(BitCarry.formatByte(b));
        }

        System.out.println();
        System.out.println(BitCarry.formatLong(new BitCarry(buffer).getBits(32)));

        //BitCarry bitCarry1 = new BitCarry(buffer);
        //System.out.println("T: " + BitCarry.formatByte(bitCarry1.getBits(8)));
        //System.out.println("T: " + BitCarry.formatByte(bitCarry1.getBits(8)));
    }

    public static void compress(String filename, CompressionType compressionType) throws IOException {
        byte[] rawData = Files.readAllBytes(Paths.get("files\\" + filename));
        byte[] comp = compressionType.compress(rawData, progress -> System.out.print("\rC: " + progress));
        //Files.write(Paths.get("files\\" + filename + ".comp"), comp);
        byte[] decomp = compressionType.decompress(comp, progress -> System.out.print("\rD: " + progress));
        //Files.write(Paths.get("files\\" + filename + ".decomp"), decomp);
        System.out.println("   nzip (" + filename + ") -> C: " + comp.length + " | D: " + decomp.length + " | R: " + rawData.length + " | Ratio: " + ((float) rawData.length/comp.length) + " | Verify: " + Arrays.equals(rawData, decomp));
    }

    public static void testCompress(CompressionType compressionType) throws IOException {
        compress("File1.html", compressionType);
        compress("File2.html", compressionType);
        compress("File3.html", compressionType);
        compress("File4.html", compressionType);
        compress("test.txt", compressionType);
        compress("shrek.txt", compressionType);
        compress("test_256.bin", compressionType);
        compress("small.txt", compressionType);
        compress("1byte.txt", compressionType);
        compress("small_test.txt", compressionType);
        compress("screenshot.png", compressionType);
        compress("1234.txt", compressionType);
        compress("blank.bin", compressionType);
        compress("monkey.bmp", compressionType);
        compress("empty.txt", compressionType);

        /*
        (File1.html) -> C: 24180 | D: 80479 | R: 80479 | Ratio: 3.328329 | Verify: true
        (File2.html) -> C: 76176 | D: 344523 | R: 344523 | Ratio: 4.5227237 | Verify: true
        (File3.html) -> C: 22013 | D: 83069 | R: 83069 | Ratio: 3.7736337 | Verify: true
        (File4.html) -> C: 41758 | D: 206694 | R: 206694 | Ratio: 4.949806 | Verify: true
        (test.txt) -> C: 34655 | D: 184207 | R: 184207 | Ratio: 5.315452 | Verify: true
        (shrek.txt) -> C: 33142 | D: 70658 | R: 70658 | Ratio: 2.1319776 | Verify: true
        (test_256.bin) -> C: 274 | D: 256 | R: 256 | Ratio: 0.93430656 | Verify: true
        (small.txt) -> C: 54 | D: 80 | R: 80 | Ratio: 1.4814814 | Verify: true
        (1byte.txt) -> C: 3 | D: 1 | R: 1 | Ratio: 0.33333334 | Verify: true
        (small_test.txt) -> C: 38 | D: 64 | R: 64 | Ratio: 1.6842105 | Verify: true
        (screenshot.png) -> C: 645098 | D: 645096 | R: 645096 | Ratio: 0.9999969 | Verify: true
        (1234.txt) -> C: 566 | D: 200448 | R: 200448 | Ratio: 354.1484 | Verify: true
        (blank.bin) -> C: 274 | D: 102400 | R: 102400 | Ratio: 373.72263 | Verify: true
        (monkey.bmp) -> C: 2513345 | D: 3686550 | R: 3686550 | Ratio: 1.4667903 | Verify: true
        (empty.txt) -> C: 0 | D: 0 | R: 0 | Ratio: NaN | Verify: true
         */
    }
}
