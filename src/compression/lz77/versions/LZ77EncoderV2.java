package compression.lz77.versions;

import compression.BitCarry;
import compression.huffman.HuffmanEncoder;
import compression.huffman.HuffmanTree;
import compression.lz77.SuffixArray;

import java.util.*;
import java.util.function.Consumer;

public class LZ77EncoderV2 {
    private static final int REFERENCE_LENGTH_SIZE = 8; //Size in bits to encode length
    private static final int REFERENCE_DISTANCE_SIZE = 16; //Size in bits to encode distance
    private static final int REFERENCE_SMALL_DISTANCE_SIZE = 10; //Size in bits to encode small distance

    //This is because encoded reference uses 3 bytes + 1 bit while raw data uses 3 bytes and 3 bits
    //Yes, we can set it to 3, because 25 < 27, but then we sacrifice length, so in average file it will get worse
    private static final int MIN_DATA_LENGTH = 4;

    //255 + 4 => which is 1 byte used in encoding, remember [0, 1, 2, 3] are never used in length
    //So in the end we will have range [4; 259] - 4 => [0, 255]
    private static final int LOOK_AHEAD_BUFFER_SIZE = (1 << REFERENCE_LENGTH_SIZE) - 1 + MIN_DATA_LENGTH; //256 - 1 + 4 = 259

    //Because of cycling data new possible range is [1, 65536] in case of 111111111 -> 1<8, 1> so we add +1
    private static final int MIN_DATA_DISTANCE = 1;
    private static final int SEARCH_BUFFER_SIZE = (1 << REFERENCE_DISTANCE_SIZE) + MIN_DATA_DISTANCE; //[0; 65535] which is 2 bytes used in encoding

    //Return generated huffman tree for frequencies of length of repeating data and also list of references
    @SafeVarargs
    private static Map.Entry<HuffmanTree, List<int[]>> generateHeader(BitCarry bitCarry, byte[] data, Consumer<Float>... callbacks) {
        SuffixArray suffixArray = new SuffixArray(data, LOOK_AHEAD_BUFFER_SIZE, SEARCH_BUFFER_SIZE, MIN_DATA_LENGTH);
        List<int[]> references = new ArrayList<>(); //Store position as of position in buffer and store length as of ref_length
        HashMap<Integer, Integer> frequencies = new HashMap<>(); //Store frequencies of repeating length values
        int position = 0;

        while (position < data.length - MIN_DATA_LENGTH) {
            int[] reference = suffixArray.nextLongestMatch(position);
            int length = reference[0]; //Length of repeating data

            if (length >= MIN_DATA_LENGTH) {
                references.add(new int[] { position, length, reference[1] });
                int ref_length = length - MIN_DATA_LENGTH;
                frequencies.put(ref_length, frequencies.getOrDefault(ref_length, 0)+1);
                position += length;
            } else {
                position++;
            }

            for (Consumer<Float> callback : callbacks) { callback.accept((float) position/data.length*60f); }
        }

        HuffmanTree huffmanTree = new HuffmanTree(frequencies);
        HuffmanEncoder.encodeHeader(bitCarry, huffmanTree);
        return Map.entry(huffmanTree, references);
    }

    @SafeVarargs
    public static byte[] compress(byte[] data, Consumer<Float>... callbacks) {
        if (data.length == 0) { return data; }
        BitCarry bitCarry = new BitCarry(); //Used to easily manipulate bits
        bitCarry.pushBits(1, 1); // Determine if data is compressed or no

        Map.Entry<HuffmanTree, List<int[]>> header = generateHeader(bitCarry, data, callbacks);
        HuffmanTree huffmanTree = header.getKey();
        int position = 0; //Position for taking data from buffer

        //Write data with references
        for (int[] reference : header.getValue()) {
            while (position != reference[0]) { //Get end position where reference was taken from
                for (Consumer<Float> callback : callbacks) { callback.accept(60+((float) position/data.length*30f)); }
                boolean b1 = isLeadingOne(data[position], 8);
                if (b1) { bitCarry.pushBits(1, 1); }  //This determines if next data is raw data that starts with 1 bit
                bitCarry.pushBits(data[position], 8);
                position++;
            }

            int length = reference[1]; //Length of repeating data
            int distance = reference[2]; //Position from where to copy data
            int offset = position - distance - MIN_DATA_DISTANCE; //Offset, aka, how much to go back
            int ref_length = length - MIN_DATA_LENGTH;
            boolean arg1 = (offset > ((1 << REFERENCE_SMALL_DISTANCE_SIZE) - 1));
            bitCarry.pushBits(0b10, 2); //This determines if next data encoded reference
            HuffmanTree.Node node = huffmanTree.getLookupTable().get(ref_length);
            bitCarry.pushBits(node.getBinary(), node.getBinaryLength()); //Save length as huffman binary path
            bitCarry.pushBits(arg1 ? 1 : 0, 1);
            bitCarry.pushBits(offset, arg1 ? REFERENCE_DISTANCE_SIZE : REFERENCE_SMALL_DISTANCE_SIZE);
            position += length;
        }

        boolean isBigger = (bitCarry.getSize(false) + (data.length - position)) > data.length;

        //In case if compressed data is bigger than original, there is no point in storing it
        if (isBigger) {
            bitCarry.clear();
            bitCarry.pushBits(0, 1);
            position = 0;
        }

        //Write remaining bytes as raw data
        for (int i = position; i < data.length; i++) {
            for (Consumer<Float> callback : callbacks) { callback.accept(90+((float) i/data.length*10f)); }
            if (!isBigger && isLeadingOne(data[i], 8)) { bitCarry.pushBits(1, 1); }
            bitCarry.pushBits(data[i], 8);
        }

        for (Consumer<Float> callback : callbacks) { callback.accept(100f); }
        return bitCarry.getBytes(true);
    }

    @SafeVarargs
    public static byte[] decompress(byte[] data, Consumer<Float>... callbacks) {
        if (data.length == 0) { return data; }
        BitCarry bitCarry = new BitCarry(data);
        boolean compressed = bitCarry.getBits(1) == 1;
        HuffmanTree huffmanTree = compressed ? HuffmanEncoder.decodeHeader(bitCarry) : null;
        ArrayList<Byte> output = new ArrayList<>();
        int position = 0;

        while (!compressed && (bitCarry.availableSize(false) > 0)) {
            output.add((byte) bitCarry.getBits(8, true, true));
            long done = data.length - bitCarry.availableSize(false); //Calculate how many bytes we processed
            for (Consumer<Float> callback : callbacks) { callback.accept((float) done/data.length*100); }
        }

        while (compressed && (bitCarry.availableSize(false) > 0)) {
            //D: 01110101 -> 01110101
            if (bitCarry.getBits(1, true, false) == 0) {
                output.add((byte) bitCarry.getBits(8, true, true));
                position += 1;
                continue;
            }

            bitCarry.getBits(1, true, true); //It is definitely 1, we don't need it

            //D: 11110101 -> 1 11110101
            if ((bitCarry.getBits(1, true, false) == 1)) {
                output.add((byte) bitCarry.getBits(8, true, true));
                position += 1;
                continue;
            }

            bitCarry.getBits(1, true, true); //It is definitely 0, we don't need it

            //R: 01110001 -> 1 0 01110001
            //R: 11110001 -> 1 0 11110001
            int length = getHuffmanLength(bitCarry, huffmanTree) + MIN_DATA_LENGTH; //Length is encoded as huffman binary path + MIN_DATA_LENGTH
            boolean arg1 = bitCarry.getBits(1) == 1; //Check if we have long or short distance
            int distance = (int) bitCarry.getBits(arg1 ? REFERENCE_DISTANCE_SIZE : REFERENCE_SMALL_DISTANCE_SIZE) + MIN_DATA_DISTANCE; //Distance is encoded as 2 byte and 1 byte is 16 bits

            //Copy bytes in loop from past
            for (int i = 0; i < length; i++) {
                output.add(output.get((position - distance + i)));
            }

            position += length; //Increase position by reference length
            long done = data.length - bitCarry.availableSize(false); //Calculate how many bytes we processed
            for (Consumer<Float> callback : callbacks) { callback.accept((float) done/data.length*100); }
        }

        for (Consumer<Float> callback : callbacks) { callback.accept(100f); }
        return BitCarry.copyBytes(output);
    }

    public static boolean isLeadingOne(long data, int size) {
        return (((data >>> (size-1)) & 0x1) == 1);
    }

    private static int getHuffmanLength(BitCarry bitCarry, HuffmanTree huffmanTree) {
        HuffmanTree.Node node = huffmanTree.getRoot();

        //Get bit by bit and navigate trough nodes to get back data until we reach the end
        while (true) {
            byte binary = (byte) bitCarry.getBits(1); //Yes, this is not very efficient way, but I kinda don't care
            node = (binary == 0) ? node.getLeftNode() : node.getRightNode(); //Depending on bit we go left or right
            if (!node.isLeaf()) { continue; } //If node is a leaf, then we reached the end and should get data
            return node.getCharacter(); //Get length from huffman node
        }
    }
}
