package folk.sisby.surveyor.util.uints;

import folk.sisby.surveyor.util.ArrayUtil;

import java.util.BitSet;
import java.util.function.Function;

public interface ArrayUInts extends UInts {
    @Override
    default int[] getUnmasked(BitSet mask) {
        int[] unmasked = new int[mask.size()];
        int maskedIndex = 0;
        for (int i = 0; i < unmasked.length; i++) {
            if (mask.get(i)) {
                unmasked[i] = get(maskedIndex);
                maskedIndex++;
            }
        }
        return unmasked;
    }

    @Override
    default UInts remap(Function<Integer, Integer> remapping, int defaultValue, int cardinality) {
        int[] newArray = ArrayUtil.ofSingle(cardinality, defaultValue);
        for (int i = 0; i < newArray.length; i++) {
            newArray[i] = remapping.apply(get(i));
        }
        return UInts.fromUInts(newArray, defaultValue);
    }
}