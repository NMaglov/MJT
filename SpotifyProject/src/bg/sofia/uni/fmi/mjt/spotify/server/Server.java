package bg.sofia.uni.fmi.mjt.spotify.server;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Server {
    private boolean active = true;
    private static final int SERVER_PORT = 6666;
    private static final String SERVER_HOST = "localhost";
    private static final String CONTENT_PATH = "songs";
    private static final String ACCOUNTS_PATH = "accounts";
    private static final String PLAYLISTS_PATH = "playlists";
    private PrintWriter writer = null;
    private KeywordsSearchEngine keywordsSearchEngine;
    private final Map<String, Integer> contentViews = new HashMap<>();
    private final NavigableSet<Pair<String, Integer>> sortedContentViews = new TreeSet<>((p1, p2) -> {
        if (p1.getValue().compareTo(p2.getValue()) != 0) {
            return p1.getValue().compareTo(p2.getValue());
        }
        return p1.getKey().compareTo(p2.getKey());
    });
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());


    public Server() {
        try (Stream<Path> filepath
                     = Files.walk(Path.of(CONTENT_PATH))) {
            writer = new PrintWriter(new FileWriter(ACCOUNTS_PATH, true));
            final int extensionLength = 4;
            keywordsSearchEngine = new KeywordsSearchEngine(filepath.map((path -> {
                String str = path.toString();
                if (str.equals(CONTENT_PATH)) {
                    return "";
                }
                return str.substring(CONTENT_PATH.length() + 1, str.length() - extensionLength);
            })).toList());
            Handler fileHandler = new FileHandler("logs");
            LOGGER.addHandler(fileHandler);
            writer.close();
        } catch (Exception e) {
            log(e);
        }
    }

    void log(Exception e) {
        Writer buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e.printStackTrace(pw);
        LOGGER.log(Level.SEVERE, buffer.toString(), e);
        try {
            pw.close();
            buffer.close();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "error closing writers in constructor");
        }

    }

    public Server(PrintWriter writer, Collection<String> data) {
        this.writer = writer;
        keywordsSearchEngine = new KeywordsSearchEngine(data);

    }

    /**
     * Starts streaming song - sends audio format and sets source of data in attachments's input stream.
     *
     * @param socketChannel socket channel where the audio format is written
     * @param attachment    attachment of selection key which input stream is set to
     *                      input stream to file of song with the given name
     * @param audioFormat   audio format to be sent
     * @param inputStream   source of song data to be sent to client
     * @param name          song name
     */
    public void play(SocketChannel socketChannel, Attachment attachment,
                     AudioFormat audioFormat, InputStream inputStream, String name)
            throws IOException {
        Utils.sendAudioFormat(audioFormat, socketChannel);
        attachment.setContent(name);
        attachment.setInputStream(inputStream);
        if (contentViews.containsKey(name)) {
            sortedContentViews.remove(new Pair<>(name, contentViews.get(name)));
        } else {
            contentViews.put(name, 0);
        }
        contentViews.put(name, contentViews.get(name) + 1);
        sortedContentViews.add(new Pair<>(name, contentViews.get(name)));
    }


    /**
     * Stops streaming
     *
     * @param attachment attachment of selection key which input stream is set to null
     */
    public void stopStreaming(Attachment attachment) throws IOException {
        if (attachment == null || attachment.getInputStream() == null) {
            return;
        }
        attachment.getInputStream().close();
        attachment.setInputStream(null);

        String name = attachment.getContent();
        sortedContentViews.remove(new Pair<>(name, contentViews.get(name)));
        contentViews.put(name, contentViews.get(name) - 1);
        if (contentViews.get(name) > 0) {
            sortedContentViews.add(new Pair<>(name, contentViews.get(name)));
        }

    }

    /**
     * Checks whether a line same as pattern is present in reader's source.
     *
     * @param reader  reader from which data is read
     * @param pattern pattern for which check if present
     * @return true if pattern is present, false otherwise
     */
    public boolean checkIfExist(BufferedReader reader, String pattern) throws IOException {
        String line;
        boolean found = false;
        while ((line = reader.readLine()) != null) {
            if (line.equals(pattern)) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Checks whether user with given email exists.
     *
     * @param reader reader of the user account's data
     * @param email  email to match with
     * @return true is user with given email exists, false otherwise
     */
    public boolean checkIfUserWithSuchEmailExists(BufferedReader reader, String email) throws IOException {
        String line;
        boolean found = false;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");
            if (parts[0].equals(email)) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Registers user with email and password.
     *
     * @param writer   writer to account's information source
     * @param email    email of the user
     * @param password password of the user
     */
    public void register(PrintWriter writer, String email, String password) {
        writer.println(email + " " + password);
        writer.flush();
    }

    /**
     * Tries to login user.
     *
     * @param reader       reader to user information source(used to check if user exists)
     * @param selectionKey key to which user metadata is attached
     * @param email        email of user
     * @param password     password of user
     * @return true if user with given email and password exists, false otherwise
     */
    public boolean login(BufferedReader reader, SelectionKey selectionKey, String email, String password)
            throws IOException {
        String pattern = email + " " + password;
        if (!checkIfExist(reader, pattern)) {
            return false;
        }
        selectionKey.attach(new Attachment(email, null, null));
        return true;
    }

    /**
     * Search songs by keywords and writes those with at least one keyword in them to socket channel
     *
     * @param socketChannel socket channel where songs containing at least one keyword are written
     * @param keywords      keywords by which the search is performed
     * @throws IOException if error occurs when writing to socket channel
     */
    public void search(SocketChannel socketChannel, String[] keywords) throws IOException {
        Utils.writeStringsToSocketChannel(keywordsSearchEngine.findContentByKeywords(keywords), socketChannel);
    }

    /**
     * Finds n most watched songs at the moment and writes them to socket channel.
     *
     * @param socketChannel socket channel where songs are written
     * @param n             number of songs to list
     * @throws IOException if error occurs when writing to socket channel
     */
    public void top(SocketChannel socketChannel, int n) throws IOException {
        Utils.writeStringsToSocketChannel(Utils.getTopN(sortedContentViews, n), socketChannel);
    }

    /**
     * Creates playlist(creates file with given name in directory path/email)
     *
     * @param path  home path of playlists
     * @param email email of user who creates the playlist
     * @param name  name of playlist
     * @throws IOException if error occurs when creating playlist
     */
    public boolean createPlaylist(String path, String email, String name) throws IOException {
        if (Files.exists(Path.of(path, email, name))) {
            return false;
        }
        Files.createDirectories(Path.of(path, email));
        Files.createFile(Path.of(path, email, name));
        return true;
    }

    /**
     * Add song to playlist.
     *
     * @param writer writer to playlist
     * @param song   name of song
     */
    public void addSongToPlaylist(PrintWriter writer, String song) {
        writer.println(song);
        writer.flush();
    }

    /**
     * Extracts songs names from bufferedReader and writes them to socket channel
     *
     * @param socketChannel  socket channel where songs are written
     * @param bufferedReader source of songs
     * @throws IOException if error occurs when writing to socket channel or reading songs
     */
    public void showPlaylist(SocketChannel socketChannel, BufferedReader bufferedReader) throws IOException {
        List<String> songs = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            songs.add(line);
        }
        Utils.writeStringsToSocketChannel(songs, socketChannel);
    }

    /**
     * Sends data chunk through socket channel. If no data left to send streaming is stopped.
     *
     * @param socketChannel socket channel where data is written
     * @param attachment    attachment containing source of data
     * @throws IOException if error occurs when writing to socket channel
     */
    public void sendData(SocketChannel socketChannel, Attachment attachment) throws IOException {
        if (attachment != null && attachment.getInputStream() != null) {
            if (!Utils.sendNextDataChunk(socketChannel, attachment)) {
                stopStreaming(attachment);
            }
        }
    }

    /**
     * Sends 1 to socket channel if file exists 0 otherwise.
     *
     * @param path          path of file that is checked if exists
     * @param socketChannel socket channel where data is written
     * @throws IOException if error occurs when writing to socket channel
     */
    public boolean sendFileStatus(Path path, SocketChannel socketChannel) throws IOException {
        if (!Files.exists(path)) {
            Utils.writeInt(socketChannel, 0);
            return false;
        }
        Utils.writeInt(socketChannel, 1);
        return true;

    }

    /**
     * Starts server socket channel and handles clients commands.
     */
    public void work() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()) {
            serverSocketChannel.bind(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            serverSocketChannel.configureBlocking(false);

            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (active) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    try {
                        if (key.isReadable()) {
                            Attachment attachment = ((Attachment) key.attachment());
                            SocketChannel sc = (SocketChannel) key.channel();
                            String command = Utils.readCommand(sc);
                            String[] parts = Utils.split(command);
                            if (parts.length == 0) {
                                continue;
                            }

                            if ("disconnect".equals(parts[0])) {
                                key.cancel();
                            } else if ("register".equals(parts[0])) {
                                if (!checkIfUserWithSuchEmailExists(Files.newBufferedReader(Path.of(ACCOUNTS_PATH)),
                                        parts[1])) {
                                    register(writer, parts[1], parts[2]);
                                    Utils.writeInt(sc, 1);
                                } else {
                                    Utils.writeInt(sc, 0);
                                }
                            } else if ("login".equals(parts[0])) {
                                try (var reader = Files.newBufferedReader(Path.of(ACCOUNTS_PATH))) {
                                    Utils.writeInt(sc, login(reader, key, parts[1], parts[2]) ? 1 : 0);
                                } catch (Exception e) {
                                    throw new RuntimeException("couldn't login user " + parts[1], e);
                                }
                            } else if ("search".equals(parts[0])) {
                                search(sc, Arrays.copyOfRange(parts, 1, parts.length));
                            } else if ("top".equals(parts[0])) {
                                top(sc, Integer.parseInt(parts[1]));
                            } else if ("create_playlist".equals(parts[0])) {
                                Utils.writeInt(sc, createPlaylist(PLAYLISTS_PATH, attachment.getEmail(), parts[1])
                                        ? 1 : 0);
                            } else if ("add_song_to".equals(parts[0])) {
                                Path path = Path.of(PLAYLISTS_PATH, attachment.getEmail(), parts[1]);
                                if (sendFileStatus(path, sc)) {
                                    try (var writer = new PrintWriter(new FileWriter(path.toString(), true))) {
                                        addSongToPlaylist(writer, parts[2]);
                                    } catch (Exception e) {
                                        throw new RuntimeException("error adding song to playlist " +
                                                parts[1] + " for client " + attachment.getEmail(), e);
                                    }
                                }
                            } else if ("show_playlist".equals(parts[0])) {
                                Path path = Path.of(PLAYLISTS_PATH, attachment.getEmail(), parts[1]);
                                if (Files.exists(path)) {
                                    try (var reader = Files.newBufferedReader(path)) {
                                        showPlaylist((SocketChannel) key.channel(), reader);
                                    } catch (Exception e) {
                                        throw new RuntimeException("couldn't show playlist" + parts[1], e);
                                    }
                                } else {
                                    Utils.writeInt(sc, 0);
                                }
                            } else if ("play".equals(parts[0])) {
                                String name = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                                Path content = Path.of(CONTENT_PATH, name + ".wav");
                                if (sendFileStatus(content, sc)) {
                                    AudioFormat audioFormat =
                                            AudioSystem.getAudioInputStream(new File(content.toString())).getFormat();
                                    play((SocketChannel) key.channel(), (Attachment) key.attachment(), audioFormat,
                                            Files.newInputStream(content), name);
                                }
                            } else if ("stop".equals(parts[0])) {
                                stopStreaming(attachment);
                            }
                        } else if (key.isWritable()) {
                            sendData((SocketChannel) key.channel(), (Attachment) key.attachment());
                        } else if (key.isAcceptable()) {
                            ServerSocketChannel socketChannel = (ServerSocketChannel) key.channel();
                            SocketChannel accept = socketChannel.accept();
                            accept.configureBlocking(false);
                            accept.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, null);
                        }

                        keyIterator.remove();
                    } catch (Exception e) {
                        stopStreaming((Attachment) key.attachment());
                        keyIterator.remove();
                        key.cancel();
                        key.channel().close();
                        log(e);
                    }
                }

            }

        } catch (Exception e) {
            log(e);
        }
    }

    public void stop() {
        active = false;
    }
}
