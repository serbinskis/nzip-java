package compression.deflate;


import compression.huffman.HuffmanEncoder;
import compression.lz77.LZ77Encoder;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class Deflate {
    @SuppressWarnings("unchecked")
    public static byte[] compress(byte[] data, Consumer<Float> ...callbacks) {
        data = LZ77Encoder.compress(data, progress -> Stream.of(callbacks).forEach(e -> e.accept(progress/2f)));
        data = HuffmanEncoder.compress(data, progress -> Stream.of(callbacks).forEach(e -> e.accept(49.9f + progress/2f)));
        for (Consumer<Float> callback : callbacks) { callback.accept(100f); }
        return data;
    }

    @SuppressWarnings("unchecked")
    public static byte[] decompress(byte[] data, Consumer<Float> ...callbacks) {
        data = HuffmanEncoder.decompress(data, progress -> Stream.of(callbacks).forEach(e -> e.accept(progress/2f)));
        data = LZ77Encoder.decompress(data, progress -> Stream.of(callbacks).forEach(e -> e.accept(49.9f + progress/2f)));
        for (Consumer<Float> callback : callbacks) { callback.accept(100f); }
        return data;
    }
}
