package bg.sofia.uni.fmi.mjt.spotify.client;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final int SERVER_PORT = 6666;
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 4096;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private Player player;
    private boolean loggedIn = false;
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    public Client() {
        try {
            Handler fileHandler = new FileHandler("logs");
            LOGGER.addHandler(fileHandler);
        } catch (Exception e) {
            Writer buffer = new StringWriter();
            PrintWriter pw = new PrintWriter(buffer);
            e.printStackTrace(pw);
            LOGGER.log(Level.SEVERE, buffer.toString(), e);
        }
    }

    /**
     * Logins the client.
     */
    public void login() {
        loggedIn = true;
    }


    /**
     * Returns command corresponding to the given arguments
     *
     * @param args         arguments from which the command is derived
     * @param outputStream stream where messages for invalid commands are printed.
     */
    public Commands getCommand(String[] args, OutputStream outputStream) {
        PrintWriter writer = new PrintWriter(outputStream);
        final int three = 3;
        Commands command;
        String message;
        try {
            command = Commands.valueOf(args[0].toUpperCase());
        } catch (Exception e) {
            message = "Invalid command";
            writer.println(message);
            writer.flush();
            return null;
        }
        if (command == Commands.DISCONNECT && args.length != 1) {
            message = "disconnect has no arguments";
            writer.println(message);
        } else if (command == Commands.REGISTER && args.length != three) {
            message = "register has 2 arguments - email and password";
            writer.println(message);
        } else if (command == Commands.LOGIN && args.length != three) {
            message = "login has 2 arguments - email and password";
            writer.println(message);
        } else if (!loggedIn && command != Commands.LOGIN && command != Commands.REGISTER) {
            message = "You are not logged in";
            writer.println(message);
        } else if (command == Commands.SEARCH && args.length < 2) {
            message = "provide at least one keyword";
            writer.println(message);
        } else if (command == Commands.TOP && args.length != 2) {
            message = "top has 1 argument";
            writer.println(message);
        } else if (command == Commands.TOP) {
            try {
                Integer.parseInt(args[1]);
                return command;
            } catch (Exception e) {
                message = "top's argument should be integer";
                writer.println(message);
                writer.flush();
                return null;
            }
        } else if (command == Commands.CREATE_PLAYLIST && args.length != 2) {
            message = "create_playlist has 1 argument - the playlist name";
            writer.println(message);
        } else if (command == Commands.ADD_SONG_TO && args.length != three) {
            message = "add_song_to has 2 argument - playlist name and song name";
            writer.println(message);
        } else if (command == Commands.SHOW_PLAYLIST && args.length != 2) {
            message = "show_playlist has 1 argument - the playlist name";
            writer.println(message);
        } else if (command == Commands.PLAY && args.length != 2) {
            message = "play has 1 argument - the name of song";
            writer.println(message);
        } else if (command == Commands.STOP && args.length > 1) {
            message = "stop has no argument";
            writer.println(message);
        } else {
            return command;
        }
        writer.flush();
        return null;
    }


    /**
     * Stops client's player streaming
     *
     * @param socketChannel socket channel where stop command is sent to the server.
     * @throws IOException if cant't read all bytes from socketChannel
     */
    public void stopStreaming(SocketChannel socketChannel) throws IOException {
        if (player.getStreaming()) {
            buffer.clear();
            buffer.put("stop".getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            socketChannel.write(buffer);
            player.setStreaming(false);
            Utils.readAllBytes(socketChannel);
        }
    }

    /**
     * Starts playing song by receiving AudioFormat information from socket channel and setting player for streaming.
     *
     * @param socketChannel socket channel used for receiving data
     * @throws IOException              if cant't read from socket channel
     * @throws LineUnavailableException if error occurs when creating dataline for the player
     */
    public boolean play(SocketChannel socketChannel) throws IOException, LineUnavailableException {
        boolean successful = Utils.readInt(socketChannel) == 1;
        if (!successful) {
            return false;
        }
        AudioFormat audioFormat = Utils.getAudioFormat(socketChannel);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine dataLine = (SourceDataLine) AudioSystem.getLine(info);
        dataLine.open();
        dataLine.start();
        player.setDataLine(dataLine);
        player.setStreaming(true);
        return true;
    }


    /**
     * Connect to the server and manages commands written from user.
     */
    public void work() {
        try (SocketChannel socketChannel = SocketChannel.open();
             Scanner scanner = new Scanner(System.in)) {
            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));

            socketChannel.configureBlocking(false);
            player = new Player();
            player.setSocketChannel(socketChannel);
            player.start();

            while (true) {
                String input = scanner.nextLine().trim().replaceAll(" +", " ");
                stopStreaming(socketChannel);
                String[] parts = Utils.split(input);

                Commands command = getCommand(parts, System.out);
                if (command == null) {
                    continue;
                }

                buffer.clear();
                buffer.put(input.getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                socketChannel.write(buffer);

                if (command == Commands.DISCONNECT) {
                    player.setActive(false);
                    player.setStreaming(false);
                    break;
                }
                if (command == Commands.REGISTER) {
                    System.out.println(Utils.readInt(socketChannel) == 1 ? "Successful registration" :
                            "User with this email already exist");
                } else if (command == Commands.LOGIN) {
                    boolean successful = (Utils.readInt(socketChannel) == 1);
                    if (successful) {
                        System.out.println("Successful login");
                        login();
                    } else {
                        System.out.println("Wrong email or password");
                    }
                } else if (command == Commands.SEARCH || command == Commands.TOP || command == Commands.SHOW_PLAYLIST) {
                    List<String> matching = Utils.readStringsFromSocketChannel(socketChannel);
                    if (matching.size() == 0) {
                        System.out.println("Nothing to show");
                    } else {
                        for (String str : matching) {
                            System.out.println(str);
                        }
                    }
                } else if (command == Commands.CREATE_PLAYLIST) {
                    System.out.println(Utils.readInt(socketChannel) == 1 ? "Playlist created" :
                            "Such playlist already exist");
                } else if (command == Commands.ADD_SONG_TO) {
                    System.out.println(Utils.readInt(socketChannel) == 1 ? "Song added" :
                            "No such playlist");
                } else if (command == Commands.PLAY) {
                    System.out.println(play(socketChannel) ? "Playing..." : "No such song");
                }
            }

        } catch (Exception e) {
            Writer buffer = new StringWriter();
            PrintWriter pw = new PrintWriter(buffer);
            e.printStackTrace(pw);
            LOGGER.log(Level.SEVERE, buffer.toString(), e);
        }

    }
}
