package fitnesse.wikitext.parser;

import util.Maybe;
import java.util.ArrayList;
import java.util.List;

public class Matcher {

    public static final Matcher noMatch = new NullMatcher();
    
    private interface ScanMatch {
        Maybe<Integer> match(ScanString input, int offset);
    }

    private static final ArrayList<Character> defaultList = new ArrayList<Character>();
    static {
        defaultList.add('\0');
    }

    private ArrayList<ScanMatch> matches = new ArrayList<ScanMatch>();
    private ArrayList<Character> firsts = null;

    public List<Character> getFirsts() {
        return firsts != null ? firsts : defaultList;
    }

    public Matcher whitespace() {
        if (firsts == null) firsts = defaultList;
        matches.add(new ScanMatch() {
            public Maybe<Integer> match(ScanString input, int offset) {
                int length = input.whitespaceLength(offset);
                return length > 0 ? new Maybe<Integer>(length) : Maybe.noInteger;
            }
        });
        return this;
    }

    public Matcher startLine() {
        matches.add(new ScanMatch() {
            public Maybe<Integer> match(ScanString input, int offset) {
                return input.startsLine(offset) ? new Maybe<Integer>(0) : Maybe.noInteger;
            }
        });
        return this;
    }

    public Matcher startLineOrCell() {
        matches.add(new ScanMatch() {
            public Maybe<Integer> match(ScanString input, int offset) {
                return input.startsLine(offset, "\n|") ? new Maybe<Integer>(0) : Maybe.noInteger;
            }
        });
        return this;
    }

    public Matcher string(final String delimiter) {
        if (firsts == null) {
            firsts = new ArrayList<Character>();
            firsts.add(delimiter.charAt(0));
        }
        matches.add(new ScanMatch() {
            public Maybe<Integer> match(ScanString input, int offset) {
                return input.matches(delimiter, offset) ? new Maybe<Integer>(delimiter.length()) : Maybe.noInteger;
            }
        });
        return this;
    }

    public Matcher string(final String[] delimiters) {
        if (firsts == null) {
            firsts = new ArrayList<Character>();
            for (String delimiter: delimiters) firsts.add(delimiter.charAt(0));
        }
        matches.add(new ScanMatch() {
            public Maybe<Integer> match(ScanString input, int offset) {
                for (String delimiter: delimiters) {
                    if (input.matches(delimiter, offset)) return new Maybe<Integer>(delimiter.length());
                }
                return Maybe.noInteger;
            }
        });
        return this;
    }

    public Matcher repeat(final char delimiter) {
        if (firsts == null) {
            firsts = new ArrayList<Character>();
            firsts.add(delimiter);
        }
        matches.add(new ScanMatch() {
            public Maybe<Integer> match(ScanString input, int offset) {
                int size = 0;
                while (input.charAt(offset + size) == delimiter) size++;
                return size > 0 ? new Maybe<Integer>(size) : Maybe.noInteger;
            }
        });
        return this;
    }

    public Matcher endsWith(final char[] terminators) {
        matches.add(new ScanMatch() {
            public Maybe<Integer> match(ScanString input, int offset) {
                int size = 0;
                while (true) {
                    char candidate = input.charAt(offset + size);
                    if (candidate == 0) return Maybe.noInteger;
                    if (contains(terminators, candidate)) break;
                    size++;
                }
                return size > 0 ? new Maybe<Integer>(size + 1) : Maybe.noInteger;
            }

            private boolean contains(char[] terminators, char candidate) {
                for (char terminator: terminators) if (candidate == terminator) return true;
                return false;
            }
        });
        return this;
    }

    public SymbolMatch makeMatch(SymbolType type, ScanString input)  {
        int totalLength = 0;
        for (ScanMatch match: matches) {
            Maybe<Integer> matchLength = match.match(input, totalLength);
            if (matchLength.isNothing()) return SymbolMatch.noMatch;
            totalLength += matchLength.getValue();
        }

        return new SymbolMatch(new Symbol(type, input.substring(0, totalLength)), totalLength);
    }
   
}
