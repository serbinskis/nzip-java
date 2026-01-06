package compression.lz77;

import compression.lz77.versions.LZ77EncoderV1;
import compression.lz77.versions.LZ77EncoderV2;

import java.util.function.Consumer;

public class LZ77Encoder {
    @SafeVarargs
    public static byte[] compress(byte[] data, Consumer<Float>... callbacks) {
        return LZ77EncoderV2.compress(data, callbacks);
    }

    @SafeVarargs
    public static byte[] decompress(byte[] data, Consumer<Float>... callbacks) {
        return LZ77EncoderV2.decompress(data, callbacks);
    }
}
