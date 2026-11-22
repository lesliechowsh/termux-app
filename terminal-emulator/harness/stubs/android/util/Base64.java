package android.util;

import java.nio.charset.StandardCharsets;

/**
 * JVM harness stub mirroring android.util.Base64 semantics. Unlike the other stubs in this
 * directory this is a REAL implementation on top of java.util.Base64, because protocol
 * transmit/receive tests (OSC 52, iTerm2/Kitty image payloads) depend on honest encoding.
 *
 * Semantics matched from AOSP:
 * - DEFAULT (0): standard alphabet, padded, wrapped at 76 chars with '\n' (a separator
 *   terminates the last line too).
 * - NO_WRAP: no line separators at all. NO_PADDING: omit '=' padding.
 * - CRLF: line separator becomes "\r\n". URL_SAFE: '-'/'_' alphabet.
 * - NO_CLOSE: only meaningful for the OutputStream API; accepted here as a no-op flag.
 * - decode skips whitespace and accepts missing padding; garbage chars throw
 *   IllegalArgumentException("bad base-64") like AOSP.
 */
public final class Base64 {

    public static final int DEFAULT = 0;
    public static final int NO_PADDING = 1;
    public static final int NO_WRAP = 2;
    public static final int CRLF = 4;
    public static final int URL_SAFE = 8;
    public static final int NO_CLOSE = 16;

    private static final String LINE_SEPARATOR = "\n";
    private static final int LINE_LENGTH = 76; // 19 groups of 4

    private Base64() {}

    public static String encodeToString(byte[] input, int flags) {
        byte[] encoded = encode(input, flags);
        return encoded == null ? null : new String(encoded, StandardCharsets.US_ASCII);
    }

    public static byte[] encode(byte[] input, int flags) {
        if (input == null) return null;
        boolean noWrap = (flags & NO_WRAP) != 0;
        boolean crlf = (flags & CRLF) != 0;
        java.util.Base64.Encoder encoder =
                (flags & URL_SAFE) != 0 ? java.util.Base64.getUrlEncoder() : java.util.Base64.getEncoder();
        if ((flags & NO_PADDING) != 0) encoder = encoder.withoutPadding();
        byte[] raw = encoder.encode(input);
        if (noWrap) return raw;

        String separator = crlf ? "\r\n" : LINE_SEPARATOR;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(
                raw.length + (raw.length / LINE_LENGTH + 1) * separator.length());
        int sinceLastLine = 0;
        for (byte b : raw) {
            out.write(b);
            if (++sinceLastLine == LINE_LENGTH) {
                for (int i = 0; i < separator.length(); i++) out.write(separator.charAt(i));
                sinceLastLine = 0;
            }
        }
        if (sinceLastLine > 0) {
            for (int i = 0; i < separator.length(); i++) out.write(separator.charAt(i));
        }
        return out.toByteArray();
    }

    public static byte[] decode(String str, int flags) {
        if (str == null) return null;
        return decode(str.toCharArray(), flags);
    }

    public static byte[] decode(byte[] input, int flags) {
        if (input == null) return null;
        // ISO-8859-1 maps bytes 1:1 to chars so every byte value survives filtering.
        return decode(new String(input, StandardCharsets.ISO_8859_1).toCharArray(), flags);
    }

    private static byte[] decode(char[] chars, int flags) {
        boolean urlSafe = (flags & URL_SAFE) != 0;
        StringBuilder clean = new StringBuilder(chars.length);
        for (char c : chars) {
            if (isAlphabet(c, urlSafe) || c == '=') {
                clean.append(c);
            } else if (c > ' ') {
                // AOSP skips whitespace and throws on anything else.
                throw new IllegalArgumentException("bad base-64");
            }
        }
        int len = clean.length();
        if (len == 0) return new byte[0];
        int remainder = len % 4;
        if (remainder == 1) throw new IllegalArgumentException("bad base-64");
        if (remainder == 2) clean.append("==");
        else if (remainder == 3) clean.append('=');
        java.util.Base64.Decoder decoder =
                urlSafe ? java.util.Base64.getUrlDecoder() : java.util.Base64.getDecoder();
        return decoder.decode(clean.toString());
    }

    private static boolean isAlphabet(char c, boolean urlSafe) {
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) return true;
        return urlSafe ? (c == '-' || c == '_') : (c == '+' || c == '/');
    }
}
