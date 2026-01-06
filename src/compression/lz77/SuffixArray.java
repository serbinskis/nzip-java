package compression.lz77;

import java.util.*;
import java.util.LinkedList;

//We create combination of all possible suffixes, and their index places
//Then we will search for repeats of these combinations at these index places

//Btw this can be much better memory optimized if we would make it work with chunks of data instead
//of giving it entre buffer full of file data, but I DON'T CARE, because I only care about compression ratio,
//and memory usage or speed doesn't matter in this case. (KINDA BADLY DONE)

public class SuffixArray {
    private final HashMap<String, ArrayList<Integer>> suffixes = new HashMap<>(); //Place to store all combinations of suffixes and indexes
    private final byte[] buffer;
    private final int lookAheadBufferSize;
    private final int searchBufferSize;
    private final int suffixLength;
    private int lastPos;

    public SuffixArray(byte[] buffer, int lookAheadBufferSize, int searchBufferSize, int suffixLength) {
        this.buffer = buffer;
        this.lookAheadBufferSize = lookAheadBufferSize;
        this.searchBufferSize = searchBufferSize;
        this.suffixLength = suffixLength;
        this.lastPos = -suffixLength;
    }

    private String getSuffix(int position) {
        StringBuilder suffix = new StringBuilder(suffixLength);
        for (int i = 0; i < suffixLength; i++) { suffix.append((char) (buffer[position + i] & 0xff)); }
        return suffix.toString();
    }

    private void removeIndexesData(int position) {
        suffixes.remove(getSuffix(position));
    }

    private ArrayList<Integer> getIndexesData(int position, boolean create) {
        String suffix = getSuffix(position);

        if (!suffixes.containsKey(suffix) && create) {
            ArrayList<Integer> indexes = new ArrayList<>();
            suffixes.put(suffix, indexes);
            return indexes;
        }

        return suffixes.get(suffix);
    }

    //https://i.imgur.com/QQorJy6.png
    private void createSuffixes(int position) {
        int diff = position - lastPos;
        if (diff == 0) { return; }
        if (diff < 0) { throw new RuntimeException("going back not implemented"); }

        //Clear buffer in the past, this will have hit on performance, but least we will not run out of memory
        //+1 because (position - index >= searchBufferSize), >= is inclusive while "to" is exclusive se we add +1

        //(1 itr) to:    1000 - 100 => 900 + 1   position: 1000  | "to" is exclusive so 900 is cleared
        //(2 itr) from:  1000 - 100 => 900 + 1   lastPos: 1000   | "from" is inclusive, so we don't need to clear 900 again

        clearSuffixes((lastPos - searchBufferSize) + 1, (position - searchBufferSize) + 1);

        //position: 100 + SUFFIX_LENGTH => 103 => 0 to 103 -> [0, 102]
        //"from" is included, but "to" is not included: [from; to)
        createSuffixes((lastPos + suffixLength), (position + suffixLength));

        //Save current position as last for next time
        this.lastPos = position;
    }

    private void createSuffixes(int from, int to) {
        if (to + suffixLength - 1 > buffer.length) { to = buffer.length - suffixLength; }
        if (from < 0) { from = 0; }

        //System.out.println("[createSuffixes]: { " + from + " " + to + " }");

        //Initialize lists for all possible combinations to store their index
        for (int i = from; i < to; i++) { getIndexesData(i, true).add(i); }
    }

    //Same logic as createSuffixes(), but it just removes indexes and lists
    private void clearSuffixes(int from, int to) {
        if (to + suffixLength - 1 > buffer.length) { to = buffer.length - suffixLength; }
        if (from < 0) { from = 0; }

        //System.out.println("[clearSuffixes]: { " + from + " " + to + " }");

        for (int i = from; i < to; i++) {
            ArrayList<Integer> arrayList = getIndexesData(i, false);
            if (arrayList != null) { arrayList.remove((Object) i); } else { continue; }
            if (arrayList.isEmpty()) { removeIndexesData(i); }
        }
    }

    public int[] nextLongestMatch(int position) {
        //Load suffixes for next position we will be working with
        createSuffixes(position);

        //Get list of indexes of repeating data for current position
        ArrayList<Integer> indexes = getIndexesData(position, false);

        //Check if data (combination) for current position has indexes inside list
        int pos = Collections.binarySearch(indexes, position);

        //If it doesn't it means there is no repeating data, which means there is no even point going further
        if (pos-- == 0) { return new int[] { -1, -1 }; }

        //Make linked list to store matching indexes to later iterate over them
        LinkedList<Integer> indexesList = new LinkedList<>();

        //Collect indexes of repeating data which is inside our working buffer
        for (int i = pos; i >= 0; i--) {
            // [578 584 587 620 623 626 1797 2175 2178 2181] Indexes of data in file for specific suffix
            //         |     searchBufferSize    |           We can only go back as much as search buffer size allows us
            //                             <- position
            //linkedList: [587 620 623 626 1797 2175]        We insert after head, because we need to preserve same order, but loop goes backwards

            int index = indexes.get(i);
            if (position - index >= searchBufferSize) { break; } //Don't include indexes which go outisde search buffer size
            if (index < position) { indexesList.add(index); }; //Add those who do to linked list, because of my poor chunk implementation, there might be duplicates
        }

        //If no indexes found then just return
        if (indexesList.isEmpty()) { return new int[] { -1, -1 }; }

        //We don't need to check for previous bytes, because if suffix length would be 3,
        //That would mean that next 2 bytes from current position are the same
        int length = suffixLength;

        //Welp, I will try to explain this, but it is kinda hard to visualize:
        //We have "position": 2101 which in buffer corresponds to byte, for example, 0x4F
        //Now we have also indexes: "indexesList", which have indexes where same bytes appeared in last "searchBufferSize": 65536
        //Now we need to loop and increase length, and in each iteration compare if next byte from the indexes offset and current position are same: "match"
        //Also we need to check for boundaries, we cannot exceed buffer or go outside current position: "outside"
        //In the end we will be left with 1 index, that has the biggest length match

        //Implement cycling data: 12341234124 -> 1234<8, 4>, copy 1234 2 times in cycle
        //This means if index goes outside of current position go back at the index and
        //check if that works (DONE)

        loop: {
            while (length < lookAheadBufferSize) { //Loop 1 byte forward and compare if indexes do satisfy rules
                if (position + length >= buffer.length) { break; } //We cannot go outside our buffer of data
                Iterator<Integer> iterator = indexesList.iterator();

                while (iterator.hasNext()) {
                    int index = iterator.next(); //This value is index of repeating data
                    boolean outside = (index + length >= position); //And we cannot (NOW WE CAN) go outside current position, because we search patterns in past to compress what is in front
                    int rlength = (outside ? (length % (position - index)) : length); //If we are outside position, we need to go back and check for repeating data
                    boolean match = (buffer[index + rlength] == buffer[position + length]); //Check if data: [index offset + length] = [position + length]

                    if (match) { continue; } //If this index satisfy rules, keep it
                    if (indexesList.size() == 1) { break loop; } //In this case we are left with final index, which is the biggest match
                    iterator.remove(); //Remove index, because data didn't match
                }

                length++;
            }
        }

        int offset = indexesList.getFirst(); //Get last remaining index
        return new int[] { (offset > -1 ? length : -1), offset };
    }
}
