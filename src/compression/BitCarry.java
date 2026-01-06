package compression;

import java.util.ArrayList;
import java.util.List;

public class BitCarry {
    private static final int MAX_SIZE = 64;
    private final ArrayList<Byte> buffer = new ArrayList<>(); //With list, it will be faster to add data, but worse for memory usage
    private byte[] data = new byte[0]; //Data used when decoding
    private byte carry = 0; //Carrying byte, for example, 0b1101000
    private long carry_long = 0; //Carrying long, for example, 0b1101000(56)
    private int carry_k = 0; //How many bits we are carrying right now
    private byte de_carry = 0; //Carrying byte for decoding
    private int de_carry_k = 0; //How many bits we are carrying right now
    private int pos = -1; //Position used in decoding data

    public BitCarry() {}
    public BitCarry(byte[] data) { this.data = data; }

    public void pushBits(long data, int size) {
        if ((size < 1) || (size > MAX_SIZE)) { throw new RuntimeException(String.format("size must be in range [1; %d]", MAX_SIZE)); }
        data = data << (MAX_SIZE - size); //Convert data to comfortable format : (56)00000101 => 10100000(56)

        while (size > 0) {
            if (carry_k == 8) { buffer.add(carry); carry_k = carry = 0; } //If carry is full, empty it
            int move_carry_k = Math.min(size, (8 - carry_k)); //How many we can move

            //Add data to carry depending on free space of carry
            //0b11111111 >> 1 => 0b11111111 (STUPID JAVA) => use (data & 0xff)
            carry |= (byte) (data >>> (carry_k + (MAX_SIZE - 8))); //64 long - 8 byte => 56 bits + carry_k

            carry_k += move_carry_k; //Increase size of carry
            size -= move_carry_k; //Decrease size of data
            data = (data << move_carry_k); //Shift data by move_carry_k (1): 10100000(56) => 01000000(56)
        }
    }

    public void pushByte(byte data) {
        pushBits((data & 0xff), 8);
    }

    public void pushBytes(byte ...data) {
        for (byte b : data) { pushByte(b); }
    }

    public long getBits(int size, boolean reset, boolean shift) {
        if ((size < 1) || (size > MAX_SIZE)) { throw new RuntimeException(String.format("size must be in range [1; %d]", MAX_SIZE)); }
        if (reset) { carry_long = carry_k = 0; }

        if (de_carry_k == 0) {
            pos += 1;
            de_carry = data[pos];
            de_carry_k = 8;
        }

        int move_carry_k = Math.min((size - carry_k), de_carry_k); //How many we can move
        //System.out.println("{ move_carry_k: " + move_carry_k + " carry: " + formatByte(carry) + " de_carry: " + formatByte(de_carry) + " carry_k: " + carry_k + " }");

        long de_carry_long = (long) (de_carry & 0xff) << ((MAX_SIZE - 8) - carry_k); //Convert data to comfort format: 11000000 -> 11000000(56)
        carry_long |= (de_carry_long); //Move bits from de_carry to carry
        if (shift) { de_carry = (byte) ((de_carry & 0xff) << move_carry_k); } //Update left bits in de_carry, move them to right

        carry_k += move_carry_k; //Update how many we are carrying in carry
        if (shift) { de_carry_k -= move_carry_k; } //Update how many we are carrying in de_carry

        //Maybe there was not enough bits in de_carry
        if ((carry_k != size)) { return getBits(size, false, shift); }
        return (carry_long >>> (MAX_SIZE - size)); //So this is for what >>> is used
    }

    public long getBits(int size) {
        return getBits(size, true, true);
    }

    public byte getByte() {
        return (byte) getBits(8);
    }

    public byte[] getBytes(int size) {
        byte[] buffer = new byte[size];
        for (int i = 0; i < size; i++) { buffer[i] = getByte(); }
        return buffer;
    }

    public void clear() {
        this.buffer.clear();
        carry_k = carry = 0;
        de_carry_k = de_carry = 0;
    }

    private void flushCarry() {
        if (carry_k == 0) { return; }
        buffer.add(carry);
        carry_k = carry = 0;
    }

    public byte[] getBytes(boolean flush) {
        if (flush) { this.flushCarry(); }
        return copyBytes(buffer);
    }

    public long getSize(boolean bits) {
        return buffer.size() * (bits ? 8L : 1L) + (bits ? carry_k : 0L);
    }

    public long availableSize(boolean bits) {
        return (data.length - (pos+1)) * (bits ? 8L : 1L) + (bits ? de_carry_k : 0L);
    }

    public static String formatLong(long value) {
        return String.format("%64s", Long.toBinaryString(value)).replace(' ', '0');
    }

    public static String formatByte(byte value) {
        return String.format("%8s", Integer.toBinaryString(value & 0xff)).replace(' ', '0');
    }

    public static byte[] copyBytes(List<Byte> list) {
        byte[] byteArray = new byte[list.size()];

        for (int i = 0; i < list.size(); i++) {
            byteArray[i] = list.get(i);
        }

        return byteArray;
    }

    public static List<Byte> copyBytes(byte[] array) {
        ArrayList<Byte> byteList = new ArrayList<>();
        for (byte b : array) { byteList.add(b); }
        return byteList;
    }
}