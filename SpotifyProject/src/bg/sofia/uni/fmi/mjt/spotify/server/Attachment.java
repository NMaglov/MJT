package bg.sofia.uni.fmi.mjt.spotify.server;

import java.io.InputStream;

/**
 * Class used for attachment to selection key containing metadata.
 */
public class Attachment {
    private String email;
    private String content;
    private InputStream inputStream;

    public Attachment(String email, String content, InputStream inputStream) {
        this.email = email;
        this.content = content;
        this.inputStream = inputStream;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

}
