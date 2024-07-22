package bg.sofia.uni.fmi.mjt.spotify.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ServerTest {
    private static Server server;
    private static SocketChannel socketChannel;
    private static ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() throws IOException {
        List<String> data = new ArrayList<>();
        data.add("Eminem Not Afraid");
        data.add("Eminem Without Me");
        data.add("Eminem Lose Yourself");
        data.add("Metallica Nothing Else Matters");
        data.add("Metallica The Unforgiven");
        data.add("Rammstein Deutschland");
        server = new Server(null, data);
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
    void play() throws IOException {
        Attachment attachment = new Attachment(null, null, null);
        server.play(socketChannel, attachment, null, null, "Rammstein Deutschland");
        assertEquals("Rammstein Deutschland", attachment.getContent(), "name of content which is currently playing, should be in the attachment");
        server.play(socketChannel, attachment, null, null, "Eminem Lose Yourself");
        assertEquals("Eminem Lose Yourself", attachment.getContent(), "name of content which is currently playing, should be in the attachment");
    }

    @Test
    void stop() {
        byte[] bytes = new byte[1];
        try (var inputStream = new ByteArrayInputStream(bytes)) {
            Attachment attachment = new Attachment(null, null, null);
            server.play(socketChannel, attachment, null, inputStream, "Rammstein Deutschland");
            assertNotNull(attachment.getInputStream(), "after starting streaming source of data should be set");
            server.stopStreaming(attachment);
            assertNull(attachment.getInputStream(), "when streaming stops source of data should be null");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void checkIfExistTest() {
        try (StringWriter stringWriter = new StringWriter();
             PrintWriter printWriter = new PrintWriter(stringWriter)) {
            printWriter.println("abcd ef");
            printWriter.println("a  e");
            printWriter.println("qwr");
            printWriter.println("12iop");
            assertFalse(server.checkIfExist(new BufferedReader(new StringReader(stringWriter.toString())), null), "cant match with null pattern");
            assertFalse(server.checkIfExist(new BufferedReader(new StringReader(stringWriter.toString())), ""), "there is no empty string");
            assertFalse(server.checkIfExist(new BufferedReader(new StringReader(stringWriter.toString())), "abcd eg"), "\"abcd eg\" does not matches with any string in the reader");
            assertTrue(server.checkIfExist(new BufferedReader(new StringReader(stringWriter.toString())), "a  e"), "\"a  e\" matches");
            assertTrue(server.checkIfExist(new BufferedReader(new StringReader(stringWriter.toString())), "12iop"), "\"12iop\" matches");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void checkIfUserWithSuchEmailExistsTest() {
        try (StringWriter stringWriter = new StringWriter();
             PrintWriter printWriter = new PrintWriter(stringWriter)) {
            printWriter.println("ivan a");
            printWriter.println("georgi b");
            printWriter.println("ceco c");
            printWriter.println("lora d");
            assertFalse(server.checkIfUserWithSuchEmailExists(new BufferedReader(new StringReader(stringWriter.toString())), null), "null cant match with any user");
            assertFalse(server.checkIfUserWithSuchEmailExists(new BufferedReader(new StringReader(stringWriter.toString())), ""), "there is no user \"\"");
            assertFalse(server.checkIfUserWithSuchEmailExists(new BufferedReader(new StringReader(stringWriter.toString())), "gosho"), "there is no user gosho");
            assertTrue(server.checkIfUserWithSuchEmailExists(new BufferedReader(new StringReader(stringWriter.toString())), "georgi"), "there is user georgi");
            assertTrue(server.checkIfUserWithSuchEmailExists(new BufferedReader(new StringReader(stringWriter.toString())), "lora"), "there is user lora");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void registerTest() {
        try (
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter)) {
            server.register(printWriter, "email@gmail.com", "password");
            server.register(printWriter, "secondemail@abv.bg", "password123");
            BufferedReader bufferedReader = new BufferedReader(new StringReader(stringWriter.toString()));
            assertEquals("email@gmail.com password", bufferedReader.readLine(), "saved and actual email and password should be same");
            assertEquals("secondemail@abv.bg password123", bufferedReader.readLine(), "saved and actual email and password should be same");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void loginTest() {
        try (StringWriter stringWriter = new StringWriter();
             PrintWriter printWriter = new PrintWriter(stringWriter)) {
            server.register(printWriter, "email@gmail.com", "password");
            server.register(printWriter, "secondemail@abv.bg", "password123");
            SelectionKey selectionKey = Mockito.mock(SelectionKey.class);
            assertFalse(server.login(new BufferedReader(new StringReader(stringWriter.toString())), selectionKey, "abc@yahoo.com", "pass"), "there is no user with this email");
            assertFalse(server.login(new BufferedReader(new StringReader(stringWriter.toString())), selectionKey, "secondemail@abv.bg", "pass"), "there is user with this email but passwords dont match");
            assertTrue(server.login(new BufferedReader(new StringReader(stringWriter.toString())), selectionKey, "email@gmail.com", "password"), "there is user with this email and password");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void searchManyResultsTest() throws IOException {
        server.search(socketChannel, new String[]{"Eminem"});
        List<String> strings = new ArrayList<>();
        try (var inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            inputStream.readInt();

            while (inputStream.available() > 0) {
                int size = inputStream.readInt();
                byte[] bytes = new byte[size];
                inputStream.read(bytes);
                strings.add(new String(bytes));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(3, strings.size(), "keyword Eminem is present in 3 songs-Eminem Not Afraid,Eminem Without Me,Eminem Lose Yourself");
        assertTrue(strings.contains("Eminem Not Afraid"), "keyword Eminem is present in Eminem Not Afraid");
        assertTrue(strings.contains("Eminem Without Me"), "keyword Eminem is present in Eminem Without Me");
        assertTrue(strings.contains("Eminem Lose Yourself"), "keyword Eminem is present in Eminem Lose Yourself");
    }

    @Test
    void searchOneResultTest() throws IOException {
        server.search(socketChannel, new String[]{"Deutschland"});
        List<String> strings = new ArrayList<>();
        try (var inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            inputStream.readInt();

            while (inputStream.available() > 0) {
                int size = inputStream.readInt();
                byte[] bytes = new byte[size];
                inputStream.read(bytes);
                strings.add(new String(bytes));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(1, strings.size(), "keyword Deautschland is present in 1 song-Rammstein Deutschland");
        assertTrue(strings.contains("Rammstein Deutschland"), "keyword Deautschland is present in Rammstein Deutschland");

    }

    @Test
    void searchNoResultTest() throws IOException {
        server.search(socketChannel, new String[]{"50 Cent"});
        List<String> strings = new ArrayList<>();
        try (var inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            inputStream.readInt();

            while (inputStream.available() > 0) {
                int size = inputStream.readInt();
                byte[] bytes = new byte[size];
                inputStream.read(bytes);
                strings.add(new String(bytes));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(0, strings.size(), "no song has keyword 50 Cent");
    }

    @Test
    void topTest() throws IOException {
        Attachment attachment = new Attachment(null, null, null);
        server.play(socketChannel, attachment, null, null, "Rammstein Deutschland");
        server.play(socketChannel, attachment, null, null, "Eminem Lose Yourself");
        server.play(socketChannel, attachment, null, null, "Rammstein Deutschland");
        server.play(socketChannel, attachment, null, null, "Rammstein Deutschland");
        server.play(socketChannel, attachment, null, null, "Eminem Not Afraid");
        server.play(socketChannel, attachment, null, null, "Eminem Not Afraid");
        server.top(socketChannel, 2);
        List<String> strings = new ArrayList<>();
        try (var inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            inputStream.readInt();

            while (inputStream.available() > 0) {
                int size = inputStream.readInt();
                byte[] bytes = new byte[size];
                inputStream.read(bytes);
                strings.add(new String(bytes));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(2, strings.size());
        assertTrue(strings.contains("Rammstein Deutschland"), "Rammstein Deutschland has 3 views,Eminem Not Afraid 2 and Eminem Lose Yourself 1");
        assertTrue(strings.contains("Eminem Not Afraid"), "Rammstein Deutschland has 3 views,Eminem Not Afraid 2 and Eminem Lose Yourself 1");
    }

    @Test
    void topPlayThenStopTest() {
        Attachment attachment = new Attachment(null, null, null);
        List<String> strings = new ArrayList<>();
        byte[] b = new byte[1];
        try (var inStream = new ByteArrayInputStream(b)) {
            server.play(socketChannel, attachment, null, inStream, "Rammstein Deutschland");
            server.play(socketChannel, attachment, null, inStream, "Eminem Lose Yourself");
            server.stopStreaming(attachment);
            server.top(socketChannel, 20);
            DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
            inputStream.readInt();

            while (inputStream.available() > 0) {
                int size = inputStream.readInt();
                byte[] bytes = new byte[size];
                inputStream.read(bytes);
                strings.add(new String(bytes));
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(1, strings.size(), "only 1 song is playing");
        assertTrue(strings.contains("Rammstein Deutschland"), "only Rammstein Deutshland is playing");

    }

    @Test
    void createPlaylistTest() throws IOException {
        server.createPlaylist("tmp", "email@abv.bg", "rock");
        assertTrue(Files.exists(Path.of("tmp", "email@abv.bg", "rock")), "creating playlist means creating file with name same as playlist");
        Files.walk(Path.of("tmp"))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void addSongToPlaylistTest() throws IOException {
        try (StringWriter stringWriter = new StringWriter();
             PrintWriter printWriter = new PrintWriter(stringWriter)) {
            server.addSongToPlaylist(printWriter, "Metallica Nothing Else Matters");
            BufferedReader bufferedReader = new BufferedReader(new StringReader(stringWriter.toString()));
            assertEquals("Metallica Nothing Else Matters", bufferedReader.readLine(), "if song is added to playlist its name should be written in it");
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void showPlaylistTest() throws IOException {
        server.createPlaylist("tmp", "email@abv.bg", "Rap");
        Path path = Path.of("tmp", "email@abv.bg", "Rap");
        PrintWriter printWriter = new PrintWriter(new FileWriter(path.toString()));
        server.addSongToPlaylist(printWriter, "Eminem Not Afraid");
        server.addSongToPlaylist(printWriter, "Eminem Lose Yourself");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        writer.println("Eminem Not Afraid");
        writer.println("Eminem Lose Yourself");
        BufferedReader bufferedReader = new BufferedReader(new StringReader(stringWriter.toString()));
        server.showPlaylist(socketChannel, bufferedReader);
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
        inputStream.readInt();
        List<String> strings = new ArrayList<>();
        while (inputStream.available() > 0) {
            int size = inputStream.readInt();
            byte[] bytes = new byte[size];
            inputStream.read(bytes);
            strings.add(new String(bytes));
        }
        printWriter.close();
        stringWriter.close();
        writer.close();
        bufferedReader.close();
        inputStream.close();
        assertTrue(strings.contains("Eminem Not Afraid"), "in playlist Rap there are 2 songs-Eminem Not Afraid and Eminem Lose Yourself");
        assertTrue(strings.contains("Eminem Lose Yourself"), "in playlist Rap there are 2 songs-Eminem Not Afraid and Eminem Lose Yourself");
        Files.walk(Path.of("tmp"))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void sendDataTest() {

    }

    @Test
    void workTest() throws InterruptedException {
        Thread t = new Thread(() -> server.work());
        t.start();
        Thread.sleep(100);
        final int SERVER_PORT = 6666;
        try (SocketChannel socketChannel = SocketChannel.open();
             DataInputStream inputStream = new DataInputStream(Channels.newInputStream(socketChannel));
             PrintWriter writer = new PrintWriter(Channels.newWriter(socketChannel, "UTF-8"), true);
        ) {
            socketChannel.connect(new InetSocketAddress("localhost", SERVER_PORT));
            assertTrue(socketChannel.isConnected(), "connection with server should be established");

            writer.println("play \"fake song faishfdoa\"");
            assertEquals(0, inputStream.readInt(), "cant play songs before logging in");
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.stop();
    }
}