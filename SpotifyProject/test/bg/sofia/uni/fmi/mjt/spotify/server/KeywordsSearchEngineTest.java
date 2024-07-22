package bg.sofia.uni.fmi.mjt.spotify.server;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class KeywordsSearchEngineTest {
    private static KeywordsSearchEngine keywordsSearchEngine;

    @BeforeAll
    static void setUp() {
        List<String> data = new ArrayList<>();
        data.add("Metallica Nothing Else Matters");
        data.add("Eminem Not Afraid");
        data.add("Metallica Wherever I May Roam");
        data.add("Metallica The Unforgiven");
        data.add("Eminem Lose Yourself");
        data.add("Beethoven Symphony 9");
        data.add("Vivaldi Four Seasons");
        keywordsSearchEngine = new KeywordsSearchEngine(data);
    }

    @Test
    void findContentByKeywordsTest() {
        Set<String> matching = keywordsSearchEngine.findContentByKeywords("Eminem", "Vivaldi");
        assertEquals(3, matching.size(), "keywords Eminem and Vivaldi are present in 3 songs-Eminem Not Afraid,Eminem Lose Yourself,Vivaldi Four Seasons");
        assertTrue(matching.contains("Eminem Not Afraid"), "Eminem Not Afraid contains keyword Eminem");
        assertTrue(matching.contains("Eminem Lose Yourself"), "Eminem Lose Yourself contains keyword Eminem");
        assertTrue(matching.contains("Vivaldi Four Seasons"), "Vivaldi Four Seasons contains keyword Vivaldi");
    }

    @Test
    void findContentByKeywordsNoKeywordsTest() {
        Set<String> matching = keywordsSearchEngine.findContentByKeywords();
        assertEquals(0, matching.size(), "if no keywords keywords provided, then there is no matching songs");
    }

    @Test
    void findContentByKeywordsNullTest() {
        Set<String> matching = keywordsSearchEngine.findContentByKeywords(null);
        assertEquals(0, matching.size(), "if keywords is null, then there is no matching songs");
    }
}