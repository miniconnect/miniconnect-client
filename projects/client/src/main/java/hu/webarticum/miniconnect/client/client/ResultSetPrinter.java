package hu.webarticum.miniconnect.client.client;

import java.io.IOException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hu.webarticum.miniconnect.api.MiniColumnHeader;
import hu.webarticum.miniconnect.client.repl.AnsiAppendable;
import hu.webarticum.miniconnect.client.repl.AnsiUtil;
import hu.webarticum.miniconnect.lang.ImmutableList;
import hu.webarticum.miniconnect.record.ResultField;
import hu.webarticum.miniconnect.record.ResultRecord;
import hu.webarticum.miniconnect.record.ResultTable;
import hu.webarticum.miniconnect.record.translator.ValueTranslator;

public class ResultSetPrinter {

    // TODO: make these configurable
    private static final String TOP_LINE_CHARS = "\u2500\u250C\u2510\u252C";

    private static final String HEADER_ROW_CHARS = " \u2502\u2502\u2502";

    private static final String MIDDLE_LINE_CHARS = "\u2500\u251C\u2524\u253C";

    private static final String DATA_ROW_CHARS = " \u2502\u2502\u2502";

    private static final String BOTTOM_LINE_CHARS = "\u2500\u2514\u2518\u2534";

    private static final String NULL_PLACEHOLDER = "[NULL]";

    private static final int ROWS_BUFFER_SIZE = 20;

    private static final int LOW_MAX_STRING_LENGTH = 20;

    private static final int HIGH_MAX_STRING_LENGTH = 100;

    private static final int ESTIMATED_MAX_TABLE_WIDTH = 120;
    
    private static final String STRING_OVERFLOW_ELLIPSIS = "\u2025\u2025";
    
    private static final Locale OUTPUT_LOCALE = Locale.ENGLISH;

    
    public void print(ResultTable resultTable, AnsiAppendable out) throws IOException {
        ImmutableList<MiniColumnHeader> columnHeaders = resultTable.resultSet().columnHeaders();
        ImmutableList<String> columnNames = columnHeaders.map(MiniColumnHeader::name);
        List<ImmutableList<Object>> decodedRowsBuffer = new ArrayList<>();
        boolean foundAny = false;
        for (ResultRecord resultRecord : resultTable) {
            foundAny = true;
            ImmutableList<Object> decodedRow = resultRecord.getAll().map(ResultField::get);
            decodedRowsBuffer.add(decodedRow);
            if (decodedRowsBuffer.size() == ROWS_BUFFER_SIZE) {
                printDecodedRows(decodedRowsBuffer, columnNames, resultTable.valueTranslators(), out);
                decodedRowsBuffer.clear();
            }
        }
        if (!decodedRowsBuffer.isEmpty()) {
            printDecodedRows(decodedRowsBuffer, columnNames, resultTable.valueTranslators(), out);
        }
        if (!foundAny) {
            printNoRows(out);
        }
    }
    
    private void printNoRows(AnsiAppendable out) throws IOException {
        out.append("  Result set contains no rows!\n\n");
    }
    
    private void printDecodedRows(
            List<ImmutableList<Object>> decodedRows,
            ImmutableList<String> columnNames,
            ImmutableList<ValueTranslator> valueTranslators,
            AnsiAppendable out
            ) throws IOException {
        int columnCount = columnNames.size();
        int[] widths = new int[columnCount];
        boolean[] aligns = new boolean[columnCount];
        for (int i = 0; i < columnCount; i++) {
            String columnName = columnNames.get(i);
            widths[i] = columnName.length();
        }
        
        int numericColumnCount = 0;
        if (!decodedRows.isEmpty()) {
            for (int i = 0; i < columnCount; i++) {
                if (isNumeric(valueTranslators.get(i))) {
                    aligns[i] = true;
                    numericColumnCount++;
                }
            }
        }
        
        int decorationWidth = (columnCount * 3) + 1;
        int estimatedNumericWidth = numericColumnCount * 3;
        int fullRemainingStringWidth = ESTIMATED_MAX_TABLE_WIDTH - decorationWidth - estimatedNumericWidth;
        int nonNumericColumnCount = columnCount - numericColumnCount;
        int rawMaxStringLength = (int) Math.ceil(fullRemainingStringWidth / ((double) nonNumericColumnCount));
        int maxStringLength = Math.max(LOW_MAX_STRING_LENGTH, Math.min(HIGH_MAX_STRING_LENGTH, rawMaxStringLength));
        List<ImmutableList<ValueOutputHolder>> outputRows = new ArrayList<>(decodedRows.size());
        for (ImmutableList<Object> decodedRow : decodedRows) {
            ImmutableList<ValueOutputHolder> outputRow = decodedRow.map((i, v) -> outputOf(v, maxStringLength));
            outputRows.add(outputRow);
            for (int i = 0; i < columnCount; i++) {
                String stringValue = outputRow.get(i).plainString;
                widths[i] = Math.max(widths[i], stringValue.length());
            }
        }

        printLine(widths, new LineDefinition(TOP_LINE_CHARS), out);
        
        printOutputRow(columnNames.map(this::outputOfHeader), widths, new boolean[columnCount], new LineDefinition(HEADER_ROW_CHARS), out);
        
        printLine(widths, new LineDefinition(MIDDLE_LINE_CHARS), out);
        
        for (ImmutableList<ValueOutputHolder> outputRow : outputRows) {
            printOutputRow(outputRow, widths, aligns, new LineDefinition(DATA_ROW_CHARS), out);
        }
        
        printLine(widths, new LineDefinition(BOTTOM_LINE_CHARS), out);

        out.append('\n');
    }
    
    private boolean isNumeric(ValueTranslator valueTranslator) {
        Class<?> clazz;
        try {
            clazz = Class.forName(valueTranslator.assuredClazzName());
        } catch (ClassNotFoundException e) {
            return false;
        }
        
        return Number.class.isAssignableFrom(clazz);
    }

    private void printLine(
            int[] widths, LineDefinition lineDefinition, AnsiAppendable out) throws IOException {
        out.append("  ");
        boolean first = true;
        for (int width : widths) {
            if (first) {
                out.append(lineDefinition.left);
                first = false;
            } else {
                out.append(lineDefinition.cross);
            }
            out.append(lineDefinition.inner);
            for (int i = 0; i < width; i++) {
                out.append(lineDefinition.inner);
            }
            out.append(lineDefinition.inner);
        }
        out.append(lineDefinition.right);
        out.append('\n');
    }

    private void printOutputRow(
            ImmutableList<ValueOutputHolder> outputRow,
            int[] widths,
            boolean[] aligns,
            LineDefinition lineDefinition,
            AnsiAppendable out
            ) throws IOException {
        out.append("  ");
        char lineChar = lineDefinition.left;
        for (int i = 0; i < widths.length; i++) {
            out.append(lineChar);
            out.append(' ');
            printValueOutput(outputRow.get(i), widths[i], aligns[i], out);
            out.append(' ');
            lineChar = lineDefinition.cross;
        }
        out.append(lineDefinition.right);
        out.append('\n');
    }
    
    private void printValueOutput(
            ValueOutputHolder valueOutputHolder, int width, boolean align, AnsiAppendable out) throws IOException {
        Object value = valueOutputHolder.value;
        int valueWidth = valueOutputHolder.plainString.length();
        int padWidth = Math.max(0, width - valueWidth);
        int leftPadWidth = (value != null && align) ? alignValueOutput(valueOutputHolder, padWidth) : 0;
        int rightPadWidth = padWidth - leftPadWidth;
        out.append(spaces(leftPadWidth));
        out.appendAnsi(valueOutputHolder.ansiString);
        out.append(spaces(rightPadWidth));
    }

    private int alignValueOutput(ValueOutputHolder valueOutputHolder, int padWidth) {
        return padWidth;
    }
    
    private String spaces(int length) {
        StringBuilder resultBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            resultBuilder.append(' ');
        }
        return resultBuilder.toString();
    }

    private ValueOutputHolder outputOfHeader(String headerName) {
        int ctrolPos = getCtrlPos(headerName, headerName.length());
        String displayName = ctrolPos == -1 ? headerName : headerName.substring(0, ctrolPos);
        return new ValueOutputHolder(headerName, displayName, AnsiUtil.formatAsHeader(displayName));
    }
    
    private ValueOutputHolder outputOf(Object value, int maxStringLength) {
        if (value == null) {
            return new ValueOutputHolder(value, NULL_PLACEHOLDER, AnsiUtil.formatAsNone(NULL_PLACEHOLDER));
        } else if (
                value instanceof Float ||
                value instanceof Double) {
            String numberString = String.format(OUTPUT_LOCALE, "%.3f", value);
            return new ValueOutputHolder(value, numberString, AnsiUtil.formatAsNumber(numberString));
        } else if (value instanceof Number || value instanceof Temporal || value instanceof TemporalAmount) {
            String numberString = value.toString();
            return new ValueOutputHolder(value, numberString, AnsiUtil.formatAsNumber(numberString));
        } else if (value instanceof Temporal || value instanceof TemporalAmount) {
            String temporalString = value.toString();
            return new ValueOutputHolder(value, temporalString, temporalString);
        } else {
            return shortenedOutputOf(value.toString(), maxStringLength);
        }
    }
    
    private ValueOutputHolder shortenedOutputOf(Object value, int maxLength) {
        String stringValue = value.toString();
        
        int length = stringValue.length();
        int ctrlPos = getCtrlPos(stringValue, maxLength);
        if (length <= maxLength && ctrlPos == -1) {
            return new ValueOutputHolder(value, stringValue, stringValue);
        }
        
        if (ctrlPos == 0) {
            return new ValueOutputHolder(value, STRING_OVERFLOW_ELLIPSIS, AnsiUtil.formatAsNone(STRING_OVERFLOW_ELLIPSIS));
        }

        int ellipsisLength = STRING_OVERFLOW_ELLIPSIS.length();

        int cutLength = maxLength;
        if (ctrlPos != -1 && ctrlPos + ellipsisLength < cutLength) {
            cutLength = ctrlPos + ellipsisLength;
        }

        int innerLength = Math.max(1, cutLength - ellipsisLength);
        String excerpt = stringValue.substring(0, innerLength);
        return new ValueOutputHolder(value, excerpt + STRING_OVERFLOW_ELLIPSIS, excerpt + AnsiUtil.formatAsNone(STRING_OVERFLOW_ELLIPSIS));
    }

    private int getCtrlPos(String stringValue, int maxLength) {
        int until = Math.min(maxLength, stringValue.length());
        for (int i = 0; i < until; i++) {
            if (Character.isISOControl(stringValue.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
    
    
    private static class LineDefinition {

        private final char inner;
        
        private final char left;
        
        private final char right;
        
        private final char cross;
        
        
        private LineDefinition(String chars) {
            this(chars.charAt(0), chars.charAt(1), chars.charAt(2), chars.charAt(3));
        }

        private LineDefinition(char inner, char left, char right, char cross) {
            this.inner = inner;
            this.left = left;
            this.right = right;
            this.cross = cross;
        }

    }

    private static class ValueOutputHolder {
        
        private final Object value;
        
        private final String plainString;
        
        private final String ansiString;
        
        
        private ValueOutputHolder(Object value, String plainString, String ansiString) {
            this.value = value;
            this.plainString = plainString;
            this.ansiString = ansiString;
        }
        
    }

}
