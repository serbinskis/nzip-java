package compression;

import compression.deflate.Deflate;
import compression.huffman.HuffmanEncoder;
import compression.lz77.LZ77Encoder;

import java.util.function.Consumer;
import java.util.stream.Stream;

public enum CompressionType {
    DEFLATE("Deflate", ".nzip"),
    HUFFMAN("Huffman", ".huff"),
    LZSS("LZSS", ".lzss");

    public static final CompressionType[] COMPRESSION_TYPES = new CompressionType[] { DEFLATE, HUFFMAN, LZSS };
    private final String name;
    private final String extension;

    CompressionType(String name, String extension) {
        this.name = name;
        this.extension = extension;
    }

    // Getter methods
    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static CompressionType getCompressed(String filename) {
        String extension = filename.substring(Math.max(filename.lastIndexOf('.'), 0));
        return Stream.of(COMPRESSION_TYPES).filter(e -> e.getExtension().equalsIgnoreCase(extension)).findFirst().orElse(null);
    }

    @SafeVarargs
    public final byte[] compress(byte[] data, Consumer<Float>... callbacks) {
        return switch (this) {
            case DEFLATE -> Deflate.compress(data, callbacks);
            case HUFFMAN -> HuffmanEncoder.compress(data, callbacks);
            case LZSS -> LZ77Encoder.compress(data, callbacks);
        };
    }

    @SafeVarargs
    public final byte[] decompress(byte[] data, Consumer<Float>... callbacks) {
        return switch (this) {
            case DEFLATE -> Deflate.decompress(data, callbacks);
            case HUFFMAN -> HuffmanEncoder.decompress(data, callbacks);
            case LZSS -> LZ77Encoder.decompress(data, callbacks);
        };
    }
}
