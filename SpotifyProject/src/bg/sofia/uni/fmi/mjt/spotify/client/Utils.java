package bg.sofia.uni.fmi.mjt.spotify.client;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    private static final int BUFFER_SIZE = 4096;


    /**
     * Splits string with delimeter whitespace, except when text is surrounded with quotes,
     * then it is present as single word.
     * For example splitting "ab "cd e" fgh" returns ["ab", "cd e", "fgh"]
     *
     * @param str string to be splitted
     * @return array of parts of str
     */
    public static String[] split(String str) {
        boolean f = false;
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '\"') {
                if (cur.length() > 0) {
                    parts.add(cur.toString());
                }
                cur.setLength(0);
                f = !f;
            } else if (str.charAt(i) == ' ') {
                if (!f) {
                    if (cur.length() > 0) {
                        parts.add(cur.toString());
                    }
                    cur.setLength(0);
                } else {
                    cur.append(str.charAt(i));
                }
            } else {
                cur.append(str.charAt(i));
            }
        }
        if (cur.length() > 0) {
            parts.add(cur.toString());
        }
        String[] arr = new String[parts.size()];
        parts.toArray(arr);
        return arr;
    }

    /**
     * Read single int from socket channel
     *
     * @param socketChannel socket channel from which the integer is read
     * @return integer which was read from the socket channel
     * @throws IOException if cant't read from the socket channel
     */
    public static int readInt(SocketChannel socketChannel) throws IOException {
        final int intSize = 4;
        ByteBuffer byteBuffer = ByteBuffer.allocate(intSize);
        while (byteBuffer.position() < intSize) {
            socketChannel.read(byteBuffer);
        }
        byteBuffer.flip();
        return byteBuffer.getInt();
    }


    /**
     * Reads audio format information from socket channel
     *
     * @param socketChannel socket channel from which data is received
     * @return the audio format received from the socket channel
     * @throws IOException if cant't read from the socket channel
     */
    public static AudioFormat getAudioFormat(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        while (true) {
            int size = socketChannel.read(byteBuffer);
            if (size > 0) {
                break;
            }
        }
        byteBuffer.flip();
        int size = byteBuffer.getInt();
        byte[] encodingBytes = new byte[size];
        byteBuffer.get(encodingBytes);
        return new AudioFormat(new AudioFormat.Encoding(new String(encodingBytes)), byteBuffer.getFloat(),
                byteBuffer.getInt(), byteBuffer.getInt(), byteBuffer.getInt(),
                byteBuffer.getFloat(), byteBuffer.getInt() == 1);
    }


    /**
     * Reads all bytes in the socket channel
     *
     * @param socketChannel the socket channel from which bytes are read
     * @throws IOException if error occur when reading from the socket channel
     */
    public static void readAllBytes(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        while (socketChannel.read(byteBuffer) == BUFFER_SIZE) {
            byteBuffer.rewind();
        }
    }


    /**
     * Reads strings data from socket channel
     *
     * @param socketChannel the socket channel from which bytes are read
     * @return list of strings read from the socket channel
     * @throws IOException if error occur when reading from the socket channel
     */
    public static List<String> readStringsFromSocketChannel(SocketChannel socketChannel) throws IOException {
        List<String> strings = new ArrayList<>();
        final int intSize = 4;
        ByteBuffer tmp = ByteBuffer.allocate(intSize);
        while (tmp.position() < intSize) {
            socketChannel.read(tmp);
        }
        tmp.flip();
        int size = tmp.getInt();
        if (size == 0) {
            return strings;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        while (byteBuffer.position() < size) {
            socketChannel.read(byteBuffer);
        }
        byteBuffer.flip();
        while (byteBuffer.remaining() > 0) {
            int len = byteBuffer.getInt();
            byte[] cur = new byte[len];
            byteBuffer.get(cur);
            strings.add(new String(cur, StandardCharsets.UTF_8));
        }

        return strings;
    }
}
