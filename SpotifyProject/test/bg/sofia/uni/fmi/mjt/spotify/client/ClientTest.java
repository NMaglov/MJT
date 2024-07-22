package bg.sofia.uni.fmi.mjt.spotify.client;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


class ClientTest {

    @BeforeAll
    static void setUp() {

    }

    @Test
    void getCommandInvalidTest() {
        Client client = new Client();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertNull(client.getCommand(new String[]{"stop"}, outputStream), "client should be logged in to execute commands");
        client.login();
        assertNull(client.getCommand(new String[]{"adsf"}, outputStream), "adsf is invalid operation");
        assertNull(client.getCommand(new String[]{"pllay", "song"}, outputStream), "pllay is invalid operation");
        assertNull(client.getCommand(new String[]{"register", ""}, outputStream), "register should have 2 args - email and password");
        assertNull(client.getCommand(new String[]{"register", "asf@abv.bg", "", ""}, outputStream), "register should have 2 args no more");
        assertNull(client.getCommand(new String[]{"login"}, outputStream), "login should have 2 - args email and password");
        assertNull(client.getCommand(new String[]{"login", "ivan@gmail.com"}, outputStream), "login without password is not possible");
        assertNull(client.getCommand(new String[]{"search"}, outputStream), "search should have at least one keyword");
        assertNull(client.getCommand(new String[]{"top"}, outputStream), "top should have integer as argument");
        assertNull(client.getCommand(new String[]{"top", "1a0"}, outputStream), "top should have integer as argument not string");
        assertNull(client.getCommand(new String[]{"top", "1.0"}, outputStream), "top should have integer as argument not double");
        assertNull(client.getCommand(new String[]{"create_playlist"}, outputStream), "create_playlist has 1 arg-name of the playlist");
        assertNull(client.getCommand(new String[]{"create_playlist", "rap", "sth else"}, outputStream), "create_playlist has 1 arg not more");
        assertNull(client.getCommand(new String[]{"add_song_to", "rap"}, outputStream), "add_song_to has 2 args-playlist name and song name,only first is given");
        assertNull(client.getCommand(new String[]{"add_song_to", "rap", "song1", "song2"}, outputStream), "add_song_to has 2 args but 3 given");
        assertNull(client.getCommand(new String[]{"show_playlist"}, outputStream), "show_playlist has 1 arg - name of playlist,but 0 given");
        assertNull(client.getCommand(new String[]{"play"}, outputStream), "play has 1 arg-song name,but it is not given");
        assertNull(client.getCommand(new String[]{"play", "song1", "song2"}, outputStream), "play has 1 arg,but 2 given");
        assertNull(client.getCommand(new String[]{"stop", "song"}, outputStream), "stop has no args,but 1 given");
        assertNull(client.getCommand(new String[]{"disconnect", ""}, outputStream), "disconnect has no args,but 1 given");
    }

    @Test
    void getCommandValidTest() {
        Client client = new Client();
        client.login();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            assertNotNull(client.getCommand(new String[]{"register", "asf@abv.bg", "pass"}, outputStream), "registration with email and password is valid operation");
            assertNotNull(client.getCommand(new String[]{"top", "5"}, outputStream), "top with given int is valid operation");
            assertNotNull(client.getCommand(new String[]{"disconnect"}, outputStream), "disconnect without args is valid operation");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void play() {
    }

    @Test
    void stop() {
    }

    @Test
    void work() {
    }
}