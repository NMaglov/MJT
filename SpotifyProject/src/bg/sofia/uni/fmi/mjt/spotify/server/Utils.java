package bg.sofia.uni.fmi.mjt.spotify.server;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;

public class Utils {
    private static final int BUFFER_SIZE = 4096;

    /**
     * Splits string with delimiter whitespace, except when text is surrounded with quotes,
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
     * Write single int to socket channel.
     *
     * @param socketChannel socket channel where the integer is written
     * @param n             the integer to be written to the socket channel
     * @throws IOException if error occurs when writing to the socket channel
     */
    public static void writeInt(SocketChannel socketChannel, int n) throws IOException {
        final int intSize = 4;
        ByteBuffer byteBuffer = ByteBuffer.allocate(intSize);
        byteBuffer.putInt(n);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
    }

    /**
     * Reads command from socket channel.
     *
     * @param socketChannel socket channel from which command is read
     * @return String representing the command
     * @throws IOException if error occurs when reading from the socket channel
     */
    public static String readCommand(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        byteBuffer.clear();
        socketChannel.read(byteBuffer);
        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Sends audio format information to the socket channel
     *
     * @param audioFormat   audio format to be sent
     * @param socketChannel socket channel where information is being written
     * @throws IOException if error occurs when writing to the socket channel
     */
    public static void sendAudioFormat(AudioFormat audioFormat, SocketChannel socketChannel)
            throws IOException {
        if (audioFormat == null || socketChannel == null) {
            return;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        byte[] encoding = audioFormat.getEncoding().toString().getBytes(StandardCharsets.UTF_8);
        byteBuffer.putInt(encoding.length);
        byteBuffer.put(encoding);
        byteBuffer.putFloat(audioFormat.getSampleRate());
        byteBuffer.putInt(audioFormat.getSampleSizeInBits());
        byteBuffer.putInt(audioFormat.getChannels());
        byteBuffer.putInt(audioFormat.getFrameSize());
        byteBuffer.putFloat(audioFormat.getFrameRate());
        byteBuffer.putInt(audioFormat.isBigEndian() ? 1 : 0);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
    }

    /**
     * Sends data chunk to the socket channel. If no data left sets attachment's input stream to null.
     *
     * @param socketChannel socket channel where data is written
     * @param attachment    the attachment which input stream is used as source of data
     * @throws IOException if error occurs when reading to the socket channel
     */
    public static boolean sendNextDataChunk(SocketChannel socketChannel, Attachment attachment) throws IOException {
        InputStream inputStream = attachment.getInputStream();
        byte[] bytes = new byte[BUFFER_SIZE];
        if (inputStream.read(bytes) < 0) {
            return false;
        } else {
            socketChannel.write(ByteBuffer.wrap(bytes));
            return true;
        }
    }

    /**
     * Finds the n most watched songs at the moment.
     *
     * @param data set of songs with their views
     * @param n    the number of songs to return
     * @return the n most watched songs
     */
    public static List<String> getTopN(NavigableSet<Pair<String, Integer>> data, int n) {
        List<String> top = new ArrayList<>();
        if (data == null || data.size() == 0) {
            return top;
        }
        var cur = data.last();
        for (int i = 0; i < n; i++) {
            if (cur == null) {
                break;
            }
            var next = data.lower(cur);
            top.add(cur.getKey());
            cur = next;
        }
        return top;
    }

    /**
     * Write strings to socket channel
     *
     * @param strings       strings to be written to socket channel
     * @param socketChannel socket channel where the strings are written
     * @throws IOException if error occurs when writing to the socket channel
     */
    public static void writeStringsToSocketChannel(Collection<String> strings,
                                                   SocketChannel socketChannel) throws IOException {
        final int intSize = 4;
        if (strings == null || strings.isEmpty()) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(intSize);
            byteBuffer.putInt(0);
            byteBuffer.flip();
            socketChannel.write(byteBuffer);
            return;
        }

        int size = 0;
        for (String str : strings) {
            size += str.length();
            size += intSize;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(size + intSize);
        byteBuffer.putInt(size);
        for (String str : strings) {
            byteBuffer.putInt(str.length());
            byteBuffer.put(str.getBytes(StandardCharsets.UTF_8));
        }
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
    }
}
