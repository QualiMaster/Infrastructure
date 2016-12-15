package eu.qualimaster.dataManagement.common.replay;

import com.google.common.primitives.Bytes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;

/**
 * Utility methods for the replay mechanism
 *
 * @author tuan
 * @since 03/06/16
 */
public class ReplayUtils {

    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yyyy,HH:mm:ss");

    /** Converts the timestamp representation to UNIX epoch.
     * This method should be equipped with a list of type-specific
     * converters */
    public static long getTimestamp(Field field, Object timeValue) {
        assert (field.isTimesamp());

        // TODO: access the respective converter from its class from a factory
        Class<?> type = field.getType();
        if (type == String.class) {
            return formatter.parseMillis((String)timeValue);
        }
        return 0L;
    }

    // Convert the timestamp.
    // Put a separate method here in case the input type gets changed later by TSI
    public static long getTimestamp(Date d) {
        return d.getTime();
    }

    /* Variants of the timestamp converters with primitive inputs to avoid (un-)boxing */
    public static long getTimestamp(Field field, long timeValue) {
        return timeValue;
    }

    public static long getTimestamp(Field field, int timeValue) {
        return timeValue;
    }

    public static long getTimestamp(Field field, short timeValue) {
        return timeValue;
    }

    public static long getTimestampFromResult(String[] hbaseKey) {
        return Long.parseLong(hbaseKey[hbaseKey.length-1]);
    }

    public static long getTimestamp(String timeValue) {
        return formatter.parseMillis((String)timeValue);
    }

    /***************************************************************************
     * Primitive arrays conversion utilities.
     **************************************************************************/

    /* assuming the characters are encoded in UTF-8 */
    private static final Charset CHARSET = Charset.forName("UTF-8");

    public static char[] toChars(byte[] bytes) {
        ByteBuffer bf = ByteBuffer.wrap(bytes);
        CharBuffer cf = CHARSET.decode(bf);
        return cf.array();
    }

    public static byte[] toBytes(char[] chars) {
        CharBuffer cf = CharBuffer.wrap(chars);
        ByteBuffer bf = CHARSET.encode(cf);
        return bf.array();
    }

    public static byte[] toBytes(String[] strings) {
        int n = strings.length;

        // avoid creating intermediate objects by incremental concantenation
        byte[][] tmp = new byte[n][];
        int size = SIZEOF_INT * (n + 1);
        for(int i = 0; i < n; i++) {
            tmp[i] = strings[i].getBytes(CHARSET);
            size += tmp[i].length;
        }
        byte[] bytes = new byte[size];
        addToByteArray(n, bytes, 0);
        int offset = SIZEOF_INT;
        for(int i = 0; i < n; i++) {
            addToByteArray(strings[i].length(), bytes, offset);
            offset += SIZEOF_INT;
            System.arraycopy(tmp[i], 0, bytes, offset, tmp[i].length);
            offset += tmp[i].length;
        }
        return bytes;
    }

    public static String[] toStrings(byte[] bytes) {
        int len = toInt(bytes, 0);
        String[] strings = new String[len];
        int offset = SIZEOF_INT;
        for(int i = 0; i < len; i++) {
            int n = toInt(bytes, offset);
            offset += SIZEOF_INT;
            strings[i] = new String(Arrays.copyOfRange(bytes, offset, n));
            offset += n;
        }
        return strings;
    }

    public static long[] toLongs(byte[] bytes) {
        int size = toInt(bytes, 0);
        long[] result = new long[size];
        int offset = SIZEOF_INT;
        for(int i = 0; i < size; i++) {
            result[i] = toLong(bytes, offset);
            offset += SIZEOF_LONG;
        }
        return result;
    }

    public static byte[] toBytes(long[] longs) {
        int size = SIZEOF_INT + longs.length * SIZEOF_LONG;
        byte[] bytes = new byte[size];
        addToByteArray(longs.length, bytes, 0);
        int offset = SIZEOF_INT;
        for(int i = 0; i < longs.length; i++) {
            addToByteArray(longs[i], bytes, offset);
            offset += SIZEOF_LONG;
        }
        return bytes;
    }

    public static int[] toInts(byte[] bytes) {
        int size = toInt(bytes, 0);
        int[] result = new int[size];
        int offset = SIZEOF_INT;
        for(int i = 0; i < size; i++) {
            result[i] = toInt(bytes, offset);
            offset += SIZEOF_INT;
        }
        return result;
    }

    public static byte[] toBytes(int[] ints) {
        int size = SIZEOF_INT + ints.length * SIZEOF_INT;
        byte[] bytes = new byte[size];
        addToByteArray(size, bytes, 0);
        int offset = SIZEOF_INT;
        for(int i = 0; i < ints.length; i++) {
            addToByteArray(ints[i], bytes, offset);
            offset += SIZEOF_INT;
        }
        return bytes;
    }

    public static short[] toShorts(byte[] bytes) {
        int size = toInt(bytes, 0);
        short[] result = new short[size];
        int offset = SIZEOF_INT;
        for(int i = 0; i < size; i++) {
            result[i] = toShort(bytes, offset);
            offset += SIZEOF_SHORT;
        }
        return result;
    }

    public static byte[] toBytes(short[] shorts) {
        int size = shorts.length * SIZEOF_SHORT + SIZEOF_INT;
        byte[] bytes = new byte[size];
        addToByteArray(size, bytes, 0);
        int offset = SIZEOF_INT;
        for(int i = 0; i < shorts.length; i++) {
            addToByteArray(shorts[i], bytes, offset);
            offset += SIZEOF_SHORT;
        }
        return bytes;
    }

    public static boolean[] toBooleans(byte[] bytes) {
        int size = toInt(bytes, 0);
        boolean[] result = new boolean[size];
        for(int i = 0; i < size; i++) {
            result[i] = toBoolean(bytes, i + SIZEOF_INT);
        }
        return result;
    }

    public static byte[] toBytes(boolean[] bools) {
        int size = SIZEOF_INT + bools.length;
        byte[] bytes = new byte[size];
        addToByteArray(size, bytes, 0);
        for(int i = 0; i < bools.length; i++) {
            bytes[i + SIZEOF_INT] = bools[i] ? (byte) 1 : (byte) 0;
        }
        return bytes;
    }

    public static float[] toFloats(byte[] bytes) {
        int size = toInt(bytes, 0);
        float[] result = new float[size];
        int offset = SIZEOF_INT;
        for(int i = 0; i < size; i++) {
            result[i] = toFloat(bytes, offset);
            offset += SIZEOF_INT;
        }
        return result;
    }

    public static byte[] toBytes(float[] floats) {
        int size = SIZEOF_INT * (floats.length + 1);
        byte[] bytes = new byte[size];
        addToByteArray(size, bytes, 0);
        int offset = SIZEOF_INT;
        for(int i = 0; i < floats.length; i++) {
            addToByteArray(floats[i], bytes, offset);
            offset += SIZEOF_INT;
        }
        return bytes;
    }

    public static double[] toDoubles(byte[] bytes) {
        int size = toInt(bytes, 0);
        double[] result = new double[size];
        int offset = SIZEOF_INT;
        for(int i = 0; i < size; i++) {
            result[i] = toDouble(bytes, offset);
            offset += SIZEOF_LONG;
        }
        return result;
    }

    public static byte[] toBytes(double[] doubles) {
        int size = SIZEOF_INT + doubles.length * SIZEOF_LONG;
        byte[] bytes = new byte[size];
        addToByteArray(size, bytes, 0);
        int offset = SIZEOF_INT;
        for(int i = 0; i < doubles.length; i++) {
            addToByteArray(doubles[i], bytes, offset);
            offset += SIZEOF_LONG;
        }
        return bytes;
    }

    /** Tuan - I constructed the whole byte array to avoid unnecessary object creation */
    private static void addToByteArray(long val, byte[] bytes, int offset) {
        int sentinel = SIZEOF_LONG + offset - 1;
        for(int i = sentinel; i > offset; i--) {
            bytes[i] = (byte) val;
            val >>>= 8;
        }
        bytes[offset] = (byte) val;
    }

    private static void addToByteArray(int val, byte[] bytes, int offset) {
        int sentinel = SIZEOF_INT + offset - 1;
        for(int i = sentinel; i > offset; i--) {
            bytes[i] = (byte) val;
            val >>>= 8;
        }
        bytes[offset] = (byte) val;
    }

    private static void addToByteArray(short val, byte[] bytes, int offset) {
        bytes[offset + 1] = (byte) val;
        val >>= 8;
        bytes[offset] = (byte) val;
    }

    private static void addToByteArray(float val, byte[] bytes, int offset) {
        int intBits = Float.floatToRawIntBits(val);
        addToByteArray(intBits, bytes, offset);
    }

    private static void addToByteArray(double val, byte[] bytes, int offset) {
        long longBits = Double.doubleToRawLongBits(val);
        addToByteArray(longBits, bytes, offset);
    }

    /** I re-ported the (slightly modified) HBase's Bytes code here
     * to avoid dependencies on HBase in the common package of DML.
     * The copyright is included.
     *
     *
     * Copyright 2010 The Apache Software Foundation
     *
     * Licensed to the Apache Software Foundation (ASF) under one
     * or more contributor license agreements.  See the NOTICE file
     * distributed with this work for additional information
     * regarding copyright ownership.  The ASF licenses this file
     * to you under the Apache License, Version 2.0 (the
     * "License"); you may not use this file except in compliance
     * with the License.  You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

    public static final int SIZEOF_INT = Integer.SIZE / Byte.SIZE;
    public static final int SIZEOF_LONG = Long.SIZE / Byte.SIZE;
    public static final int SIZEOF_SHORT = Short.SIZE / Byte.SIZE;

    private static long toLong(byte[] bytes, int offset) {
        if (offset + SIZEOF_LONG > bytes.length) {
            throw new RuntimeException("Cannot decode long from the buffer");
        }
        long l = 0;
        for(int i = offset; i < offset + SIZEOF_LONG; i++) {
            l <<= 8;
            l ^= bytes[i] & 0xFF;
        }
        return l;
    }

    private static int toInt(byte[] bytes, int offset) {
        if (offset + SIZEOF_INT > bytes.length) {
            throw new RuntimeException("Cannot decode int from the buffer");
        }
        int n = 0;
        for(int i = offset; i < (offset + SIZEOF_INT); i++) {
            n <<= 8;
            n ^= bytes[i] & 0xFF;
        }
        return n;
    }

    private static short toShort(byte[] bytes, int offset) {
        if (offset + SIZEOF_SHORT > bytes.length) {
            throw new RuntimeException("Cannot decode short from the buffer");
        }
        short n = 0;
        n ^= bytes[offset] & 0xFF;
        n <<= 8;
        n ^= bytes[offset+1] & 0xFF;
        return n;
    }

    private static float toFloat(byte[] bytes, int offset) {
        return Float.intBitsToFloat(toInt(bytes, offset));
    }

    private static double toDouble(byte[] bytes, int offset) {
        return Double.longBitsToDouble(toLong(bytes, offset));
    }

    private static boolean toBoolean(byte[] bytes, int offset) {
        if (offset  >= bytes.length) {
            throw new RuntimeException("Cannot decode boolean from the buffer");
        }
        return bytes[offset] != (byte) 0;
    }

    public static void main(String[] args) {
        System.out.println(getTimestamp("11/15/2016,10:50:34"));
    }
}
