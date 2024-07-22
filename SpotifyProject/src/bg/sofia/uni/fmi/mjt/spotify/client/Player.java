package bg.sofia.uni.fmi.mjt.spotify.client;

import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


/**
 * Thread responsible for playing songs
 */
public class Player extends Thread {
    private static final int BUFFER_SIZE = 8192;
    private SourceDataLine dataLine = null;
    private boolean streaming = false;
    private boolean active = true;
    private SocketChannel socketChannel;

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void setDataLine(SourceDataLine dataLine) {
        this.dataLine = dataLine;
    }

    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
        synchronized (this) {
            notifyAll();
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean getStreaming() {
        return streaming;
    }

    /**
     * If Player is set for streaming received data from socket channel is streamed using dataline.
     */
    @Override
    public void run() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        while (active) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException("interrupted on waiting for next data chunk", e);
                }
            }
            while (streaming) {
                int size;
                try {
                    byteBuffer.clear();
                    size = socketChannel.read(byteBuffer);
                    byteBuffer.flip();
                    if (dataLine != null) {
                        dataLine.write(byteBuffer.array(), 0, size);
                    }
                    if (size <= 0) {
                        streaming = false;
                        break;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("error when reading song data chunk and streaming it", e);
                }

            }

            if (dataLine != null) {
                dataLine.stop();
            }
        }
    }
}
