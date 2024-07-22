package bg.sofia.uni.fmi.mjt.spotify.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeywordsSearchEngine {
    private final Map<String, List<String>> byKeyword;

    public KeywordsSearchEngine(Collection<String> data) {
        byKeyword = new HashMap<>();
        for (String word : data) {
            String[] parts = word.split(" ");
            for (String part : parts) {
                if (!byKeyword.containsKey(part)) {
                    byKeyword.put(part, new ArrayList<>());
                }
                byKeyword.get(part).add(word);
            }
        }
    }

    /**
     * Finds all word sequences which contain at least one keyword from keywords.
     *
     * @param keywords the keywords to search for
     * @return set of word sequences which have at least one keyword in them
     */
    public Set<String> findContentByKeywords(String... keywords) {
        Set<String> matching = new HashSet<>();
        if (keywords == null) {
            return matching;
        }
        for (String keyword : keywords) {
            if (byKeyword.containsKey(keyword)) {
                matching.addAll(byKeyword.get(keyword));
            }
        }
        return matching;
    }
}
