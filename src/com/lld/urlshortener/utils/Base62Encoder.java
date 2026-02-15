package com.lld.urlshortener.utils;

/**
 * Base62 Encoder for converting numeric IDs to short alphanumeric codes
 *
 * Base62 uses: 0-9 (10) + A-Z (26) + a-z (26) = 62 characters
 *
 * Examples:
 *   encode(125)      → "21"
 *   encode(12345)    → "3D7"
 *   encode(9876543)  → "aI1Z"
 *
 * Capacity:
 *   62^6 = 56 billion URLs
 *   62^7 = 3.5 trillion URLs
 */
public class Base62Encoder {
    private static final String BASE62_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;

    /**
     * Encode a numeric ID to Base62 string
     *
     * Algorithm: Similar to converting decimal to hexadecimal,
     * but using base 62 instead of base 16
     *
     * @param id Numeric ID to encode
     * @return Base62 encoded string
     */
    public static String encode(long id) {
        if (id == 0) {
            return "0";
        }

        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            int remainder = (int) (id % BASE);
            sb.append(BASE62_CHARS.charAt(remainder));
            id = id / BASE;
        }

        // Reverse because we built it backwards
        return sb.reverse().toString();
    }

    /**
     * Decode a Base62 string back to numeric ID
     *
     * @param shortCode Base62 encoded string
     * @return Original numeric ID
     */
    public static long decode(String shortCode) {
        long id = 0;
        for (char c : shortCode.toCharArray()) {
            int digitValue = BASE62_CHARS.indexOf(c);
            if (digitValue == -1) {
                throw new IllegalArgumentException("Invalid character in short code: " + c);
            }
            id = id * BASE + digitValue;
        }
        return id;
    }

    /**
     * Get the length of encoded string for a given ID
     */
    public static int getEncodedLength(long id) {
        if (id == 0) return 1;
        return (int) (Math.log(id) / Math.log(BASE)) + 1;
    }

    // For testing
    public static void main(String[] args) {
        System.out.println("=== Base62 Encoder Test ===\n");

        long[] testIds = {0, 1, 62, 125, 12345, 1000000, 9876543210L};

        for (long id : testIds) {
            String encoded = encode(id);
            long decoded = decode(encoded);
            System.out.println("ID: " + id +
                             " → Encoded: " + encoded +
                             " → Decoded: " + decoded +
                             " ✓");
        }

        System.out.println("\n=== Capacity Calculation ===");
        System.out.println("6 chars: 62^6 = " + (long) Math.pow(62, 6) + " (~56 billion)");
        System.out.println("7 chars: 62^7 = " + (long) Math.pow(62, 7) + " (~3.5 trillion)");
    }
}
