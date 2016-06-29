/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.dataManagement.serialization;

import java.io.IOException;

/**
 * An abstract input data implementation providing common methods for type conversion.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractDataInput implements IDataInput {

    /**
     * Returns the next primitive data chunk.
     * 
     * @return the next data chunk
     * @throws IOException in case that there is no next data element
     * @see #isEOD()
     */
    protected abstract String next() throws IOException;
    
    @Override
    public int nextInt() throws IOException {
        try {
            return Integer.parseInt(next());
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
    }

    @Override
    public long nextLong() throws IOException {
        try {
            return Long.parseLong(next());
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean nextBoolean() throws IOException {
        boolean result;
        String tmp = next().toLowerCase();
        if (Boolean.toString(true).equals(tmp)) {
            result = true;
        } else if (Boolean.toString(false).equals(tmp)) {
            result = false;
        } else {
            throw new IOException("'" + tmp + "' cannot be converted to a boolean");
        }
        return result;
    }

    @Override
    public double nextDouble() throws IOException {
        try {
            return Double.parseDouble(next());
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
    }

    @Override
    public short nextShort() throws IOException {
        try {
            return Short.parseShort(next());
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
    }

    @Override
    public byte nextByte() throws IOException {
        try {
            return Byte.parseByte(next());
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String nextString() throws IOException {
        return next();
    }

    @Override
    public char nextChar() throws IOException {
        char result;
        String tmp = next();
        if (1 == tmp.length()) {
            result = tmp.charAt(0);
        } else {
            throw new IOException("'" + tmp + "' cannot be translated to a char");
        }
        return result;
    }

    @Override
    public float nextFloat() throws IOException {
        try {
            return Float.parseFloat(next());
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
    }
    
    /**
     * Returns an array size.
     * 
     * @return the size of the array
     * @throws IOException if the size is negative or does not exist
     */
    private int nextArraySize() throws IOException {
        int size = nextInt();
        if (size < 0) {
            throw new IOException("negative array size " + size);
        }
        return size;
    }

    @Override
    public int[] nextIntArray() throws IOException {
        int[] result = new int[nextArraySize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = nextInt();
        }
        return result;
    }

    @Override
    public long[] nextLongArray() throws IOException {
        long[] result = new long[nextArraySize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = nextLong();
        }
        return result;
    }

    @Override
    public boolean[] nextBooleanArray() throws IOException {
        boolean[] result = new boolean[nextArraySize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = nextBoolean();
        }
        return result;
    }

    @Override
    public double[] nextDoubleArray() throws IOException {
        double[] result = new double[nextArraySize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = nextDouble();
        }
        return result;
    }

    @Override
    public String[] nextStringArray() throws IOException {
        String[] result = new String[nextArraySize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = nextString();
        }
        return result;
    }

    @Override
    public char[] nextCharArray() throws IOException {
        char[] result = new char[nextArraySize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = nextChar();
        }
        return result;
    }

    @Override
    public float[] nextFloatArray() throws IOException {
        float[] result = new float[nextArraySize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = nextFloat();
        }
        return result;
    }

    @Override
    public short[] nextShortArray() throws IOException {
        short[] result = new short[nextArraySize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = nextShort();
        }
        return result;
    }

    @Override
    public byte[] nextByteArray() throws IOException {
        byte[] result = new byte[nextArraySize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = nextByte();
        }
        return result;
    }

}
