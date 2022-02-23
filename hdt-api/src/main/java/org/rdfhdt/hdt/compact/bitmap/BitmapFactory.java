package org.rdfhdt.hdt.compact.bitmap;

import org.rdfhdt.hdt.exceptions.IllegalFormatException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Class to create and load bitmaps
 * @author Antoine Willerval
 */
public abstract class BitmapFactory {

    private static BitmapFactory instance;

    private static BitmapFactory getInstance() {
        if (instance == null) {
            try {
                // Try to instantiate pro
                Class<?> managerImplClass = Class.forName("org.rdfhdt.hdt.pro.BitmapFactory");
                instance = (BitmapFactory) managerImplClass.newInstance();
            } catch (Exception e1) {
                try {
                    // Pro not found, instantiate normal
                    Class<?> managerImplClass = Class.forName("org.rdfhdt.hdt.compact.bitmap.BitmapFactoryImpl");
                    instance = (BitmapFactory) managerImplClass.newInstance();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Class org.rdfhdt.hdt.compact.bitmap.BitmapFactoryImpl not found. Did you include the HDT implementation jar?");
                } catch (InstantiationException e) {
                    throw new RuntimeException("Cannot create implementation for BitmapFactory. Does the class org.rdfhdt.hdt.compact.bitmap.BitmapFactoryImpl inherit from BitmapFactory?");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return instance;
    }

    /**
     * Type for a plain bitmap in byte
     */
    public static final byte TYPE_BITMAP_PLAIN = 1;

    /**
     * create an empty modifiable bitmap
     * @param type The type of the bitmap, null for default value
     * @return bitmap
     */
    public static ModifiableBitmap createBitmap(String type) {
        return getInstance().doCreateModifiableBitmap(type);
    }
    /**
     * create an empty unmodifiable bitmap
     * @param size The size of the bitmap
     * @return bitmap
     */
    public static Bitmap createEmptyBitmap(long size) {
        return getInstance().doCreateEmptyBitmap(size);
    }
    /**
     * create an empty modifiable bitmap without indexing, only the
     * {@link Bitmap#access(long)}, {@link Bitmap#countOnes()}, {@link Bitmap#countZeros()}, 
     * {@link ModifiableBitmap#set(long, boolean)}, {@link Bitmap#getNumBits()}, {@link Bitmap#getSizeBytes()}, 
     * {@link ModifiableBitmap#append(boolean)} methods are usable, other method behaviors are undefined.
     * 
     * @return bitmap
     */
    public static ModifiableBitmap createRWBitmap(long size) {
        return getInstance().doCreateRWModifiableBitmap(size);
    }

    /**
     * load a bitmap from an {@link InputStream}
     * @param input The stream
     * @return bitmap
     * @throws IOException io exception while reading
     */
    public static Bitmap createBitmap(InputStream input) throws IOException {
        return getInstance().doCreateBitmap(input);
    }

    // Abstract methods for the current implementation
    protected abstract ModifiableBitmap doCreateModifiableBitmap(String type);
    protected abstract ModifiableBitmap doCreateRWModifiableBitmap(long size);
    protected abstract Bitmap doCreateEmptyBitmap(long size);
    protected abstract Bitmap doCreateBitmap(InputStream input) throws IOException;
}
