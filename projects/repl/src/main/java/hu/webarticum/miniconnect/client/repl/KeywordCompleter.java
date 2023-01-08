package hu.webarticum.miniconnect.client.repl;

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import hu.webarticum.miniconnect.lang.ImmutableList;

public class KeywordCompleter implements Completer {

    private final ImmutableList<String> keywords;
    
    
    public KeywordCompleter(ImmutableList<String> keywords) {
        this.keywords = keywords;
    }
    
    
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        candidates.addAll(keywords.map(Candidate::new).asList());
    }

}
