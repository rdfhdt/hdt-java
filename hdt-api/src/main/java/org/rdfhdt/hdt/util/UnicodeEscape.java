package org.rdfhdt.hdt.util;


import java.io.IOException;

public class UnicodeEscape {
	
	private UnicodeEscape() {}
	
	   /**
     * Checks whether the supplied character is a letter or number
     * according to the N-Triples specification.
     * @see #isLetter
     * @see #isNumber
     */
    private static boolean isLetterOrNumber(int c) {
        return isLetter(c) || isNumber(c);
    }

    /**
     * Checks whether the supplied character is a letter according to
     * the N-Triples specification. N-Triples letters are A - Z and a - z.
     */
    private static boolean isLetter(int c) {
        return (c >= 65 && c <= 90) || // A - Z
                (c >= 97 && c <= 122); // a - z
    }

    /**
     * Checks whether the supplied character is a number according to
     * the N-Triples specification. N-Triples numbers are 0 - 9.
     */
    private static boolean isNumber(int c) {
        return c >= 48 && c <= 57; // 0 - 9
    }

    /**
     * Escapes a Unicode string to an all-ASCII character sequence. Any special
     * characters are escaped using backslashes (<tt>"</tt> becomes <tt>\"</tt>,
     * etc.), and non-ascii/non-printable characters are escaped using Unicode
     * escapes (<tt>&#x5C;uxxxx</tt> and <tt>&#x5C;Uxxxxxxxx</tt>).
     */
    public static String escapeString(String label) {
        try {
            StringBuilder sb = new StringBuilder(2 * label.length());
            escapeString(label, sb);
            return sb.toString();
        }
        catch (IOException e) {
            throw new AssertionError();
        }
    }

    /**
     * Escapes a Unicode string to an all-ASCII character sequence. Any special
     * characters are escaped using backslashes (<tt>"</tt> becomes <tt>\"</tt>,
     * etc.), and non-ascii/non-printable characters are escaped using Unicode
     * escapes (<tt>&#x5C;uxxxx</tt> and <tt>&#x5C;Uxxxxxxxx</tt>).
     * 
     * @throws IOException
     */
    public static void escapeString(String label, Appendable appendable)
        throws IOException
    {
      
        int first = 0;
        int last = label.length();

        if(last>1 && label.charAt(0)=='<' && label.charAt(last-1)=='>') {
        	first++;
        	last--;
        } else if(label.charAt(0)=='"') {
			first = 1;
			appendable.append('"');
			
			for(int i=last-1;i>0; i--) {
				char curr = label.charAt(i);
				
				if(curr=='"') {
					// The datatype or lang must be after the last " symbol.
					last=i-1;
					break;
				}
				
				char prev = label.charAt(i-1);
				if(curr=='@' && prev=='"') {
					last = i-2;
					break;
				}
				if(curr=='^' && prev=='^') {
					last = i-3;
					break;
				}
			}
		}
        
        for (int i = first; i <= last; i++) {
            char c = label.charAt(i);
            int cInt = c;

            if (c == '\\') {
                appendable.append("\\\\");
            }
            else if (c == '"') {
                appendable.append("\\\"");
            }
            else if (c == '\n') {
                appendable.append("\\n");
            }
            else if (c == '\r') {
                appendable.append("\\r");
            }
            else if (c == '\t') {
                appendable.append("\\t");
            }
            else if (
                cInt >= 0x0 && cInt <= 0x8 ||
                cInt == 0xB || cInt == 0xC ||
                cInt >= 0xE && cInt <= 0x1F ||
                cInt >= 0x7F && cInt <= 0xFFFF)
            {
                appendable.append("\\u");
                appendable.append(toHexString(cInt, 4));
            }
            else if (cInt >= 0x10000 && cInt <= 0x10FFFF) {
                appendable.append("\\U");
                appendable.append(toHexString(cInt, 8));
            }
            else {
                appendable.append(c);
            }
        }
        
        appendable.append(label.subSequence(last+1, label.length()));
    }
    
    /**
     * Unescapes an escaped Unicode string. Any Unicode sequences
     * (<tt>&#x5C;uxxxx</tt> and <tt>&#x5C;Uxxxxxxxx</tt>) are restored to the
     * value indicated by the hexadecimal argument and any backslash-escapes
     * (<tt>\"</tt>, <tt>\\</tt>, etc.) are decoded to their original form.
     * 
     * @param s An escaped Unicode string.
     * @return The unescaped string.
     * @throws IllegalArgumentException If the supplied string is not a
     * correctly escaped N-Triples string.
     */
    public static String unescapeString(String s) {
        int backSlashIdx = s.indexOf('\\');

        if (backSlashIdx == -1) {
            // No escaped characters found
            return s;
        }

        int startIdx = 0;
        int sLength = s.length();
        StringBuilder sb = new StringBuilder(sLength);

        while (backSlashIdx != -1) {
            sb.append(s.substring(startIdx, backSlashIdx));

            if (backSlashIdx + 1 >= sLength) {
                throw new IllegalArgumentException("Unescaped backslash in: " + s);
            }

            char c = s.charAt(backSlashIdx + 1);

            if (c == 't') {
                sb.append('\t');
                startIdx = backSlashIdx + 2;
            }
            else if (c == 'r') {
                sb.append('\r');
                startIdx = backSlashIdx + 2;
            }
            else if (c == 'n') {
                sb.append('\n');
                startIdx = backSlashIdx + 2;
            }
            else if (c == '"') {
                sb.append('"');
                startIdx = backSlashIdx + 2;
            }
            else if (c == '\\') {
                sb.append('\\');
                startIdx = backSlashIdx + 2;
            }
            else if (c == 'u') {
                // \\uxxxx
                if (backSlashIdx + 5 >= sLength) {
                    throw new IllegalArgumentException(
                            "Incomplete Unicode escape sequence in: " + s);
                }
                String xx = s.substring(backSlashIdx + 2, backSlashIdx + 6);

                try {
                    c = (char)Integer.parseInt(xx, 16);
                    sb.append(c);

                    startIdx = backSlashIdx + 6;
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Illegal Unicode escape sequence '\\u" + xx + "' in: " + s);
                }
            }
            else if (c == 'U') {
                // \\Uxxxxxxxx
                if (backSlashIdx + 9 >= sLength) {
                    throw new IllegalArgumentException(
                            "Incomplete Unicode escape sequence in: " + s);
                }
                String xx = s.substring(backSlashIdx + 2, backSlashIdx + 10);

                try {
                    c = (char)Integer.parseInt(xx, 16);
                    sb.append(c);

                    startIdx = backSlashIdx + 10;
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Illegal Unicode escape sequence '\\U" + xx + "' in: " + s);
                }
            }
            else {
                throw new IllegalArgumentException("Unescaped backslash in: " + s);
            }

            backSlashIdx = s.indexOf('\\', startIdx);
        }

        sb.append(s.substring(startIdx));

        return sb.toString();
    }

    /**
     * Converts a decimal value to a hexadecimal string representation
     * of the specified length.
     * 
     * @param decimal A decimal value.
     * @param stringLength The length of the resulting string.
     */
    public static String toHexString(int decimal, int stringLength) {
        StringBuilder sb = new StringBuilder(stringLength);

        String hexVal = Integer.toHexString(decimal).toUpperCase();

        // insert zeros if hexVal has less than stringLength characters:
        int nofZeros = stringLength - hexVal.length();
        for (int i = 0; i < nofZeros; i++) {
            sb.append('0');
        }

        sb.append(hexVal);

        return sb.toString();
    }
}
