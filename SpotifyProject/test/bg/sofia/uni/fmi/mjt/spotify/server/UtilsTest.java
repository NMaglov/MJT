package bg.sofia.uni.fmi.mjt.spotify.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UtilsTest {
    private static SocketChannel socketChannel;
    private static ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() throws IOException {
        outputStream = new ByteArrayOutputStream();
        socketChannel = Mockito.mock(SocketChannel.class);
        Mockito.when(socketChannel.write((ByteBuffer) Mockito.any())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocation) throws IOException {
                        ByteBuffer buf = (ByteBuffer) invocation.getArguments()[0];
                        byte[] bytes = new byte[buf.remaining()];
                        buf.get(bytes);
                        outputStream.write(bytes);
                        return 0;
                    }
                });

    }

    @AfterEach
    void tearDown() throws IOException {
        outputStream.close();
    }

    @Test
    void splitTest() {
        String[] parts = Utils.split("a \"bc\" d");
        assertEquals(3, parts.length, "\"a \\\"bc\\\" d\" has 3 parts");
        assertEquals("a", parts[0], "first part of \"a \\\"bc\\\" d\" is a");
        assertEquals("bc", parts[1], "second part of \"a \\\"bc\\\" d\" is bc");
        assertEquals("d", parts[2], "third part of \"a \\\"bc\\\" d\" is d");
    }

    @Test
    void writeIntTest() throws IOException {
        Utils.writeInt(socketChannel, 10);
        try (var inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            assertEquals(10, inputStream.readInt(), "written and later read data should be same");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void splitOnlyOneTest() {
        String[] parts = Utils.split("disconnect");
        assertEquals(1, parts.length, "\"disconnect\" has 1 part");
        assertEquals("disconnect", parts[0], "only part in \"disconnect\" is disconnect");

        parts = Utils.split("\"disconnect\"");
        assertEquals(1, parts.length, "\"\\\"disconnect\\\"\" has 1 part");
        assertEquals("disconnect", parts[0], "only part in \"\\\"disconnect\\\"\" is disconnect");

    }


    @Test
    void readCommandTest() throws IOException {
        String expected = "play Metalica Nothing Else Matters";
        Mockito.when(socketChannel.read((ByteBuffer) Mockito.any())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocation) {
                        ByteBuffer buf = (ByteBuffer) invocation.getArguments()[0];
                        byte[] bytes = expected.getBytes(StandardCharsets.UTF_8);
                        buf.put(bytes);
                        return 0;
                    }
                });

        String actual = Utils.readCommand(socketChannel);
        assertEquals(expected, actual, "sent and received commands should be same");
    }

    @Test
    void sendAudioFormatTest() throws IOException {
        AudioFormat expected = new AudioFormat(new AudioFormat.Encoding("PCM_SIGNED"), 44100.0f, 16,
                2, 4, 44100.0f, false);
        Utils.sendAudioFormat(expected, socketChannel);
        try (var inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            int encodingSize = inputStream.readInt();
            String encoding = new String(inputStream.readNBytes(encodingSize));
            AudioFormat actual = new AudioFormat(new AudioFormat.Encoding(encoding), inputStream.readFloat(), inputStream.readInt(),
                    inputStream.readInt(), inputStream.readInt(), inputStream.readFloat(), inputStream.readBoolean());
            assertEquals(expected.getEncoding(), actual.getEncoding(), "sent and received encodings should be same");
            assertEquals(expected.getChannels(), actual.getChannels(), "sent and received channels should be same");
            assertEquals(expected.getFrameRate(), actual.getFrameRate(), "sent and received frame rates should be same");
            assertEquals(expected.getFrameSize(), actual.getFrameSize(), "sent and received frame sizes should be same");
            assertEquals(expected.getSampleRate(), actual.getSampleRate(), "sent and received sample rates should be same");
            assertEquals(expected.getSampleSizeInBits(), actual.getSampleSizeInBits(), "sent and received sample sizes should be same");
            assertEquals(expected.isBigEndian(), actual.isBigEndian(), "sent and received endians should be same");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void sendNextDataChunkTest() throws IOException {
        byte[] expected = "hello".getBytes(StandardCharsets.UTF_8);
        try (var inputStream = new ByteArrayInputStream(expected)) {
            Attachment attachment = new Attachment("", "", inputStream);
            Utils.sendNextDataChunk(socketChannel, attachment);
            DataInputStream inputStream1 = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
            byte[] actual = inputStream1.readNBytes(expected.length);
            assertArrayEquals(expected, actual, "sent and received data should be same");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void sendNextDataChunkNothingLeftTest() throws IOException {
        byte[] expected = "laksjdf".getBytes(StandardCharsets.UTF_8);
        Attachment attachment = new Attachment("", "", new ByteArrayInputStream(expected));
        Utils.sendNextDataChunk(socketChannel, attachment);
        assertFalse(Utils.sendNextDataChunk(socketChannel, attachment));
//        assertNull(attachment.getInputStream(), "if all data is sent inputstream() should be set to null");
    }

    @Test
    void getTopNTest() {
        NavigableSet<Pair<String, Integer>> data = new TreeSet<>((p1, p2) -> {
            if (p1.getValue().compareTo(p2.getValue()) != 0) {
                return p1.getValue().compareTo(p2.getValue());
            }
            return p1.getKey().compareTo(p2.getKey());
        });

        for (int i = 0; i < 10; i++) {
            data.add(new Pair<>(String.valueOf((char) (i + 'a')), 10 - i));
        }
        List<String> top = Utils.getTopN(data, 3);
        assertEquals(3, top.size(), "if asked for top 3,then should return list with 3 elements");
        assertEquals("a", top.get(0), "best has key a");
        assertEquals("b", top.get(1), "second has key b");
        assertEquals("c", top.get(2), "third has key c");
    }


    @Test
    void getTopNNegativeNTest() {
        NavigableSet<Pair<String, Integer>> data = new TreeSet<>((p1, p2) -> {
            if (p1.getValue().compareTo(p2.getValue()) != 0) {
                return p1.getValue().compareTo(p2.getValue());
            }
            return p1.getKey().compareTo(p2.getKey());
        });

        for (int i = 0; i < 10; i++) {
            data.add(new Pair<>(String.valueOf((char) (i + 'a')), 10 - i));
        }
        List<String> top = Utils.getTopN(data, -1);
        assertEquals(0, top.size(), "if querying negative number, then should return empty list");
    }

    @Test
    void getTopNNBiggerThanNumberOfItemsTest() {
        NavigableSet<Pair<String, Integer>> data = new TreeSet<>((p1, p2) -> {
            if (p1.getValue().compareTo(p2.getValue()) != 0) {
                return p1.getValue().compareTo(p2.getValue());
            }
            return p1.getKey().compareTo(p2.getKey());
        });

        for (int i = 0; i < 2; i++) {
            data.add(new Pair<>(String.valueOf((char) (i + 'a')), 10 - i));
        }
        List<String> top = Utils.getTopN(data, 10);
        assertEquals(2, top.size(), "if querying top n, where n>size of set, then all elements should be present in returned list");
        assertEquals("a", top.get(0), "best has key a");
        assertEquals("b", top.get(1), "second has key b");
    }

    @Test
    void writeStringsToSocketChannelTest() throws IOException {
        List<String> expected = new ArrayList<>();
        expected.add("hello");
        expected.add("world");
        expected.add("!");
        Utils.writeStringsToSocketChannel(expected, socketChannel);
        try (var inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            List<String> actual = new ArrayList<>();
            int actualTotalSize = inputStream.readInt();
            int expectedTotalSize = 0;
            for (String str : expected) {
                expectedTotalSize += str.getBytes(StandardCharsets.UTF_8).length;
                expectedTotalSize += 4;
            }
            assertEquals(expectedTotalSize, actualTotalSize, "total sizes of sent data and received data should be same");
            for (int i = 0; i < expected.size(); i++) {
                int size = inputStream.readInt();
                byte[] bytes = new byte[size];
                inputStream.read(bytes);
                actual.add(new String(bytes));
            }
            assertEquals(expected, actual, "sent and received data should be same");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void writeStringsToSocketChannelNullTest() throws IOException {
        List<String> strings = null;
        Utils.writeStringsToSocketChannel(strings, socketChannel);
        try (var inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            assertEquals(0, inputStream.readInt(), "if no strings written, then there is no strings for reading");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void writeStringsToSocketChannelZeroStringsTest() throws IOException {
        List<String> strings = new ArrayList<>();
        Utils.writeStringsToSocketChannel(strings, socketChannel);
        try (var inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            assertEquals(0, inputStream.readInt(), "if no string written, then there is no strings for reading");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}