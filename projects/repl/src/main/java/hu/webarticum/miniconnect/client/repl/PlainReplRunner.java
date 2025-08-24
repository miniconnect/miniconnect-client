package hu.webarticum.miniconnect.client.repl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.function.Consumer;

public class PlainReplRunner implements ReplRunner {

    private final Reader in;
    
    private final AnsiAppendable out;
    
    private final Consumer<Exception> exceptionHandler;
    
    private boolean wasCarriageReturn = false;
    
    private boolean wasEndReached = false;

    public PlainReplRunner(InputStream in, Appendable out) {
        this(in, out, null);
    }

    public PlainReplRunner(InputStream in, Appendable out, Consumer<Exception> exceptionHandler) {
        this.in = new InputStreamReader(in);
        this.out = new PlainAnsiAppendable(out);
        this.exceptionHandler = exceptionHandler != null ? exceptionHandler : e -> e.printStackTrace();
    }

    @Override
    public void run(Repl repl) {
        try {
            runThrows(repl);
        } catch (InterruptedException e) {
            bye(repl);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            exceptionHandler.accept(e);
        }
    }

    private void runThrows(Repl repl) throws IOException, InterruptedException {
        repl.welcome(out);
        repl.prompt(out);

        StringBuilder currentQueryBuilder = new StringBuilder();

        String line;
        while ((line = readLine(in)) != null) { // NOSONAR
            currentQueryBuilder.append(line);
            String query = currentQueryBuilder.toString();
            if (!repl.isCommandComplete(query)) {
                currentQueryBuilder.append('\n');
                repl.prompt2(out);
                continue;
            }
            currentQueryBuilder = new StringBuilder();
            if (!repl.execute(query, out)) {
                break;
            }
            repl.prompt(out);
        }

        bye(repl);
    }
    
    private void bye(Repl repl) {
        try {
            repl.bye(out);
        } catch (IOException e) {
            // nothing to do
        }
    }
    
    private String readLine(Reader reader) throws IOException, InterruptedException {
        if (wasEndReached) {
            return null;
        }
        
        StringBuilder resultBuilder = new StringBuilder();
        while (true) {
            int r = readNextChar(reader);
            if (r == -1) {
                wasEndReached = true;
                break;
            }
            char c = (char) r;
            if (c== '\n') {
                if (wasCarriageReturn) {
                    wasCarriageReturn = false;
                    continue;
                } else {
                    break;
                }
            } else if (c == '\r') {
                wasCarriageReturn = false;
                break;
            } else {
                resultBuilder.append(c);
            }
        }
        return resultBuilder.toString();
    }
    
    private int readNextChar(Reader reader) throws IOException, InterruptedException {
        int sleep = 1;
        while (!reader.ready()) {
            Thread.sleep(sleep);
            if (sleep < 64) {
                sleep = sleep * 4;
            }
        }
        return reader.read();
    }

}
