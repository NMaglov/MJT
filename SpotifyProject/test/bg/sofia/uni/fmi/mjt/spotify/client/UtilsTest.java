package bg.sofia.uni.fmi.mjt.spotify.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

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
    void readIntTest() throws IOException {
        Mockito.when(socketChannel.read((ByteBuffer) Mockito.any())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocation) {
                        ByteBuffer buf = (ByteBuffer) invocation.getArguments()[0];
                        buf.putInt(10);
                        return 0;
                    }
                });

        assertEquals(10, Utils.readInt(socketChannel));
    }

    @Test
    void splitPlayTest() {
        String[] parts = Utils.split("play \"Eminem Not Afraid\"");
        assertEquals("play", parts[0], "first part of \"play \\\"Eminem Not Afraid\\\" is play");
        assertEquals("Eminem Not Afraid", parts[1], "second part of \"play \\\"Eminem Not Afraid\\\" is Eminem Not Afraid");
    }

    @Test
    void splitPlaylistTest() {
        String[] parts = Utils.split("add_to_playlist \"Rap\" \"Eminem Not Afraid\"");
        assertEquals(3, parts.length, "\"add_to_playlist \\\"Rap\\\" \\\"Eminem Not Afraid\\\"\" has 3 parts");
        assertEquals("add_to_playlist", parts[0], "first part of \"add_to_playlist \\\"Rap\\\" \\\"Eminem Not Afraid\\\"\" is add_to_playlist");
        assertEquals("Rap", parts[1], "second part of \"add_to_playlist \\\"Rap\\\" \\\"Eminem Not Afraid\\\"\" is Rap");
        assertEquals("Eminem Not Afraid", parts[2], "third part of \"add_to_playlist \\\"Rap\\\" \\\"Eminem Not Afraid\\\"\" is Eminem Not Afraid");

    }

    @Test
    void splitNoQuotesTest() {
        String[] parts = Utils.split("login ivan a");
        assertEquals(3, parts.length, "\"login ivan a\" has 3 parts");
        assertEquals("login", parts[0], "first part of \"login ivan a\" is login");
        assertEquals("ivan", parts[1], "second part of \"login ivan a\" is ivan");
        assertEquals("a", parts[2], "third part of \"login ivan a\" is a");

    }

    @Test
    void getAudioFormatTest() throws IOException {
        AudioFormat expected = new AudioFormat(new AudioFormat.Encoding("PCM_SIGNED"), 44100.0f, 16,
                2, 4, 44100.0f, false);
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.putInt(expected.getEncoding().toString().length());
        buf.put(expected.getEncoding().toString().getBytes(StandardCharsets.UTF_8));
        buf.putFloat(expected.getSampleRate());
        buf.putInt(expected.getSampleSizeInBits());
        buf.putInt(expected.getChannels());
        buf.putInt(expected.getFrameSize());
        buf.putFloat(expected.getFrameRate());
        buf.putInt(expected.isBigEndian() ? 1 : 0);
        buf.flip();
        Mockito.when(socketChannel.read((ByteBuffer) Mockito.any())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocation) throws IOException {
                        ByteBuffer dst = (ByteBuffer) invocation.getArguments()[0];
                        while (buf.remaining() > 0) {
                            dst.put(buf.get());
                        }
                        return 1;
                    }
                });

        AudioFormat actual = Utils.getAudioFormat(socketChannel);
        assertEquals(expected.getEncoding(), actual.getEncoding(), "original and received encoding should be same");
        assertEquals(expected.getChannels(), actual.getChannels(), "original and received channels should be same");
        assertEquals(expected.getFrameRate(), actual.getFrameRate(), "original and received frame rates should be same");
        assertEquals(expected.getFrameSize(), actual.getFrameSize(), "original and received frame sizes should be same");
        assertEquals(expected.getSampleRate(), actual.getSampleRate(), "original and received sample rates should be same");
        assertEquals(expected.getSampleSizeInBits(), actual.getSampleSizeInBits(), "original and received sample sizes should be same");
        assertEquals(expected.isBigEndian(), actual.isBigEndian(), "original and received endians should be same");
    }

    @Test
    void readAllBytes() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("hello");
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        try (var inputStream = new ByteArrayInputStream(bytes)) {
            Mockito.when(socketChannel.read((ByteBuffer) Mockito.any())).thenAnswer(
                    new Answer() {
                        public Object answer(InvocationOnMock invocation) throws IOException {
                            ByteBuffer dst = (ByteBuffer) invocation.getArguments()[0];
                            int size = 0;
                            byte[] b = new byte[1];
                            while (inputStream.available() > 0 && dst.remaining() > 0) {
                                inputStream.read(b);
                                dst.put(b);
                                size++;
                            }
                            return size;
                        }
                    });
            Utils.readAllBytes(socketChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        assertEquals(0, socketChannel.read(buffer), "after reading all bytes nothing should be left for reading");
    }

    @Test
    void readStringsFromSocketChannel() throws IOException {
        List<String> expected = new ArrayList<>();
        expected.add("my");
        expected.add("name");
        expected.add("is");
        expected.add("Nikolay");
        int size = 0;
        for (String str : expected) {
            size += str.getBytes(StandardCharsets.UTF_8).length;
            size += 4;
        }
        ByteBuffer buf = ByteBuffer.allocate(size + 4);
        buf.putInt(size);
        for (String str : expected) {
            buf.putInt(str.getBytes(StandardCharsets.UTF_8).length);
            buf.put(str.getBytes(StandardCharsets.UTF_8));
        }
        try (var inputStream = new ByteArrayInputStream(buf.array())) {
            Mockito.when(socketChannel.read((ByteBuffer) Mockito.any())).thenAnswer(
                    new Answer() {
                        public Object answer(InvocationOnMock invocation) throws IOException {
                            ByteBuffer dst = (ByteBuffer) invocation.getArguments()[0];
                            byte[] b = new byte[1];
                            while (dst.remaining() > 0 && inputStream.available() > 0) {
                                inputStream.read(b);
                                dst.put(b);
                            }
                            return b.length;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> actual = Utils.readStringsFromSocketChannel(socketChannel);
        assertIterableEquals(expected, actual, "sent and received strings should be same");
    }
}