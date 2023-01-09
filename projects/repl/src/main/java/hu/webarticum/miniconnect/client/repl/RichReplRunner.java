package hu.webarticum.miniconnect.client.repl;

import java.io.IOException;

import org.jline.reader.Completer;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class RichReplRunner implements ReplRunner {
    
    private final Highlighter highlighter;
    
    private final Completer completer;
    
    private final AnsiAppendable out = new RichAnsiAppendable(System.out); // NOSONAR System.out is necessary
    
    
    public RichReplRunner() {
        this(new DefaultHighlighter(), null);
    }

    public RichReplRunner(Highlighter highlighter, Completer completer) {
        this.highlighter = highlighter;
        this.completer = completer;
    }
    

    @Override
    public void run(Repl repl) {
        try {
            runThrows(repl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void runThrows(Repl repl) throws IOException {
        repl.welcome(out);

        StringBuilder currentQueryBuilder = new StringBuilder();
        
        try (Terminal terminal = createTerminal()) {
            LineReader reader = createLineReader(terminal);
            boolean wasComplete = true;
            while (true) { // NOSONAR
                String prompt = composePrompt(repl, wasComplete);
                String line;
                try {
                    line = reader.readLine(prompt);
                } catch (UserInterruptException e) {
                    break;
                }

                currentQueryBuilder.append(line);
                String query = currentQueryBuilder.toString();
                wasComplete = repl.isCommandComplete(query);
                if (!wasComplete) {
                    currentQueryBuilder.append('\n');
                    continue;
                }
                currentQueryBuilder = new StringBuilder();
                if (!repl.execute(query, out)) {
                    break;
                }
            }
        }
        
        repl.bye(out);
    }

    private Terminal createTerminal() throws IOException {
        return TerminalBuilder.builder()
                .color(true)
                .jansi(true)
                .system(true)
                .build();
    }

    private LineReader createLineReader(Terminal terminal) {
        return LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .history(new DefaultHistory())
                .highlighter(highlighter)
                .completer(completer)
                .variable(LineReader.BLINK_MATCHING_PAREN, 0)
                .build();
    }
    
    private String composePrompt(Repl repl, boolean wasComplete) throws IOException {
        StringBuilder promptBuilder = new StringBuilder();
        AnsiAppendable promptOut = new RichAnsiAppendable(promptBuilder);
        if (wasComplete) {
            repl.prompt(promptOut);
        } else {
            repl.prompt2(promptOut);
        }
        return promptBuilder.toString();
    }
    
}
