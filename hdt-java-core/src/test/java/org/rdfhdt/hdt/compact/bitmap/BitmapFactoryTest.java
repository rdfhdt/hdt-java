package org.rdfhdt.hdt.compact.bitmap;

import org.junit.Assert;
import org.junit.Test;

public class BitmapFactoryTest {

    @Test
    public void emptyBitmapTest() {
        final long size = 123456;
        Bitmap bitmap = BitmapFactory.createEmptyBitmap(size);
        Assert.assertEquals("bitmap size", size, bitmap.getNumBits());
        Assert.assertEquals("countOnes", bitmap.countOnes(), 0);
        Assert.assertEquals("countZeros", bitmap.countZeros(), size);
    }
}
