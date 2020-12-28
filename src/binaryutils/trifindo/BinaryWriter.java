
package binaryutils.trifindo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * @author Trifindo, AdAstra/LD3005
 */
public class BinaryWriter implements AutoCloseable {

    private FileOutputStream fos;
    private int bytesWritten = 0;

    public BinaryWriter(String path) throws FileNotFoundException {
        fos = new FileOutputStream(new File(path));
    }

    public BinaryWriter(File file) throws FileNotFoundException {
        fos = new FileOutputStream(file);
    }

    public int getBytesWritten() {
        return bytesWritten;
    }

    public void close() throws IOException {
        fos.close();
    }

    public void writeString(String string, int size, byte fill) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        fos.write(bytes);
        for (int i = bytes.length; i < size; i++) {
            fos.write(fill);
        }
    }

    public void writeString(String string) throws IOException {
        fos.write(string.getBytes(StandardCharsets.UTF_8));
    }

    public int writeUInt8(int value) throws IOException {
        fos.write((byte) value);
        bytesWritten += 1;
        return 1;
    }

    public int writeUInt16(int value) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putShort((short) (value & 0xffffL));
        byte[] array = byteBuffer.array();
        fos.write(array);
        bytesWritten += 2;
        return 2;
    }

    public int writeUInt32(int value) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt((int) (value & 0xffffffffL));
        byte[] array = byteBuffer.array();
        fos.write(array);
        bytesWritten += 4;
        return 4;
    }

    public int writeUInt32(long value) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt((int) (value & 0xffffffffL));
        byte[] array = byteBuffer.array();
        fos.write(array);
        bytesWritten += 4;
        return 4;
    }

    public int writeBytes(byte[] bytes) throws IOException {
        fos.write(bytes);
        bytesWritten += bytes.length;
        return bytes.length;
    }

    public static int writeUInt8(byte[] allData, int offset, int value) throws Exception {
        allData[offset] = (byte) value;
        return 1;
    }

    public static int writeUInt16(byte[] allData, int offset, int value) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putShort((short) (value & 0xffffL));
        byte[] array = byteBuffer.array();
        System.arraycopy(array, 0, allData, offset, 2);
        return 2;
    }

    public static int writeUInt32(byte[] allData, int offset, long value) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt((int) (value & 0xffffffffL));
        byte[] array = byteBuffer.array();
        System.arraycopy(array, 0, allData, offset, 4);
        return 4;
    }

    public static int writeFI32(byte[] fullData, int offset, float value) throws Exception {
        float intValue = (float) Math.floor(value);
        float decValue = value - intValue;
        writeUInt16(fullData, offset, (int) (decValue * 65535f));
        writeUInt16(fullData, offset + 2, (int) intValue);
        return 4;
    }

}
