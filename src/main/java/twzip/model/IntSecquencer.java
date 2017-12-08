package twzip.model;

import zip4pos.util.function.IntSupplier;

/**
 *
 * @author Kent Yeh
 */
public class IntSecquencer implements IntSupplier {

    int val = 0;

    public IntSecquencer(int val) {
        this.val = val;
    }

    @Override
    public int getAsInt() {
        return val++;
    }

}
