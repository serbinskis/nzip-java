package compression.huffman;

import compression.BitCarry;

import java.util.*;
import java.util.function.Consumer;

public class HuffmanEncoder {
    private static final int MAX_FREQUENCY_BITS_LENGTH = 6; //Frequency is integer, integer max binary length is 32, 32 max binary length is 6
    private static final int MAX_POSITIVE_INTEGER_LENGTH = 31; //This is max size of positive integer in bits

    @SafeVarargs
    public static byte[] compress(byte[] data, Consumer<Float> ...callbacks) {
        if (data.length == 0) { return data; }
        return encodedTree(data, new HuffmanTree(data), callbacks);
    }

    @SafeVarargs
    public static byte[] decompress(byte[] data, Consumer<Float> ...callbacks) {
        if (data.length == 0) { return data; }
        return decodedTree(data, callbacks);
    }

    @SafeVarargs
    private static byte[] encodedTree(byte[] data, HuffmanTree huffmanTree, Consumer<Float> ...callbacks) {
        BitCarry bitCarry = new BitCarry();
        bitCarry.pushBits(1, 1); // Determine if data is compressed or no (BY DEFAULT YES)
        bitCarry.pushBits(data.length, MAX_POSITIVE_INTEGER_LENGTH); //We need to know data size so that we don't read final bits, which are not used
        encodeHeader(bitCarry, huffmanTree); //Encode header, frequency map

        //Encode data from lookup table
        for (int i = 0; i < data.length; i++) {
            HuffmanTree.Node node = huffmanTree.getLookupTable().get(data[i] & 0xff);
            bitCarry.pushBits(node.getBinary(), node.getBinaryLength());
            for (Consumer<Float> callback : callbacks) { callback.accept((float) i/data.length*100); }
        }

        //In case if compressed data is bigger than original, there is no point in storing it
        if (bitCarry.getSize(false) > data.length) {
            bitCarry.clear();
            bitCarry.pushBits(0, 1);
            bitCarry.pushBytes(data);
        }

        return bitCarry.getBytes(true);
    }

    @SafeVarargs
    private static byte[] decodedTree(byte[] data, Consumer<Float> ...callbacks) {
        BitCarry bitCarry = new BitCarry(data);
        boolean compressed = bitCarry.getBits(1) == 1;
        ArrayList<Byte> output = new ArrayList<>();

        if (compressed) {
            int size = (int) bitCarry.getBits(MAX_POSITIVE_INTEGER_LENGTH); //Get size of decoded file
            HuffmanTree huffman = decodeHeader(bitCarry);
            HuffmanTree.Node node = huffman.getRoot();

            //Get bit by bit and navigate trough nodes to get back data until we reach file size
            while (output.size() < size) {
                byte binary = (byte) bitCarry.getBits(1); //Yes, this is not very efficient way, but I kinda don't care
                node = (binary == 0) ? node.getLeftNode() : node.getRightNode(); //Depending on bit we go left or right
                if (!node.isLeaf()) { continue; } //If node is a leaf, then we reached the end and should add data from it to buffer
                output.add((byte) node.getCharacter()); //Add node's character to buffer
                for (Consumer<Float> callback : callbacks) { callback.accept((float) output.size()/size*100); } //Just a simple progress callback
                node = huffman.getRoot(); //After adding character we need to go back to root and start over
            }
        } else {
            while (bitCarry.availableSize(false) > 0) {
                output.add((byte) bitCarry.getBits(8, true, true));
                long done = data.length - bitCarry.availableSize(false); //Calculate how many bytes we processed
                for (Consumer<Float> callback : callbacks) { callback.accept((float) done/data.length*100); }
            }
        }

        return BitCarry.copyBytes(output);
    }

    public static HuffmanTree encodeHeader(BitCarry bitCarry, HuffmanTree huffman) {
        boolean empty = huffman.getFrequencies().isEmpty();
        bitCarry.pushBits(empty ? 0b1 : 0b0, 1);
        if (empty) { return huffman; }

        int max_frequency = Collections.max(huffman.getFrequencies().values()); //Get max frequency, we will use it to know how many bits to use for it
        int max_frequency_bits = Integer.toBinaryString(max_frequency).length(); //Get length of max frequency
        int max_value = Collections.max(huffman.getFrequencies().keySet()); //Get max value, we will use it to know how many bits to use for it
        int max_value_bits = Integer.toBinaryString(max_value).length(); //Get length in bits of max value
        int size = huffman.getFrequencies().size() - 1; //This cannot be more than 256, and since byte is [0, 255] and size cannot be 0, therefore -1
        int size_bits = Integer.toBinaryString(size).length(); //Get size in bits for amount of elements count

        //System.out.println("[encodeHeader]: Size: " + size + ", Max Frequency Bits: " + max_frequency_bits + ", Max Value Bits: " + max_value_bits + ", Size Bits: " + size_bits);

        bitCarry.pushBits(max_frequency_bits, MAX_FREQUENCY_BITS_LENGTH); //Save max frequency size in bits, this will help to reduce space
        bitCarry.pushBits(max_value_bits, MAX_FREQUENCY_BITS_LENGTH); //Save max amount size in bits, this will help make it universal
        bitCarry.pushBits(size_bits, MAX_FREQUENCY_BITS_LENGTH); //Save max amount size in bits, this will help make it universal
        bitCarry.pushBits(size, size_bits); //Save frequency element count, so we know how much to read when decoding

        //Save frequencies to bit carry
        for (Map.Entry<Integer, Integer> entry : huffman.getFrequencies().entrySet()) {
            bitCarry.pushBits(entry.getKey(), max_value_bits);
            bitCarry.pushBits(entry.getValue(), max_frequency_bits);
        }

        return huffman;
    }

    public static HuffmanTree decodeHeader(BitCarry bitCarry) {
        boolean empty = bitCarry.getBits(1) == 1;
        if (empty) { return new HuffmanTree(new HashMap<>()); }

        int max_frequency_bits = (int) bitCarry.getBits(MAX_FREQUENCY_BITS_LENGTH); //Get info, how much space does frequency take
        int max_value_bits = (int) bitCarry.getBits(MAX_FREQUENCY_BITS_LENGTH); //Get info, how much space does size take
        int size_bits = (int) bitCarry.getBits(MAX_FREQUENCY_BITS_LENGTH); //Get info, how much space does size take
        int size = (int) (bitCarry.getBits(size_bits) + 1); //Then get info, how many frequencies we have
        HashMap<Integer, Integer> frequencies = new HashMap<>(); //Make hash map to load frequencies

        //System.out.println("[decodeHeader]: Size: " + size + ", Max Frequency Bits: " + max_frequency_bits + ", Max Value Bits: " + max_value_bits + ", Size Bits: " + size_bits);

        //Load frequencies into hashmap
        for (int i = 0; i < size; i++) {
            int character = (int) bitCarry.getBits(max_value_bits);
            int frequency = (int) bitCarry.getBits(max_frequency_bits);
            frequencies.put(character, frequency);
        }

        //Build huffman tree from frequencies
        return new HuffmanTree(frequencies);
    }
}
