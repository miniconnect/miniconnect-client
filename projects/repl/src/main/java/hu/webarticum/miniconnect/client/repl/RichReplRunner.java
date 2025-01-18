package hu.webarticum.miniconnect.client.repl;

import java.io.IOException;
import java.util.function.Consumer;

import org.jline.reader.Completer;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class RichReplRunner implements ReplRunner {
    
    private final Highlighter highlighter;
    
    private final Completer completer;
    
    private final Consumer<Exception> exceptionHandler;
    
    private final AnsiAppendable out = new RichAnsiAppendable(System.out); // NOSONAR System.out is necessary
    
    
    public RichReplRunner() {
        this(new DefaultHighlighter(), null, null);
    }

    public RichReplRunner(Consumer<Exception> exceptionHandler) {
        this(new DefaultHighlighter(), null, exceptionHandler);
    }

    public RichReplRunner(Highlighter highlighter, Completer completer) {
        this(highlighter, completer, null);
    }

    public RichReplRunner(Highlighter highlighter, Completer completer, Consumer<Exception> exceptionHandler) {
        this.highlighter = highlighter;
        this.completer = completer;
        this.exceptionHandler = exceptionHandler != null ? exceptionHandler : e -> e.printStackTrace();
    }
    

    @Override
    public void run(Repl repl) {
        try {
            runThrows(repl);
        } catch (Exception e) {
            exceptionHandler.accept(e);
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
                String line = reader.readLine(prompt);
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
