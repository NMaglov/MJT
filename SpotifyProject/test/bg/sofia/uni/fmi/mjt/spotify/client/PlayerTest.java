package bg.sofia.uni.fmi.mjt.spotify.client;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerTest {
    private static final SocketChannel socketChannel = Mockito.mock(SocketChannel.class);

    @BeforeAll
    static void setUp() throws IOException {
        Mockito.when(socketChannel.read((ByteBuffer) Mockito.any())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocation) throws IOException {
                        return 1;
                    }
                });
    }

    @Test
    void runTest() throws InterruptedException {
        Player player = new Player();
        player.setSocketChannel(socketChannel);
        assertEquals(Thread.State.NEW, player.getState(), "when created but not started player should be in state NEW");

        player.start();
        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, player.getState(), "after starting player should be in state WAITING (waiting to be set for streaming)");

        player.setStreaming(true);
        Thread.sleep(100);
        assertEquals(Thread.State.RUNNABLE, player.getState(), "after set for streaming player should be in state RUNNABLE");

        player.setStreaming(false);
        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, player.getState(), "if player stop streaming it should be in state WAITING");

        player.setActive(false);
        player.setStreaming(false);
        Thread.sleep(100);
        assertEquals(Thread.State.TERMINATED, player.getState());
    }
}