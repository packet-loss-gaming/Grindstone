/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

public class StringUtil {
    private static boolean isWhitespaceChar(char c) {
        return Character.isWhitespace(c) || c == '_' || c == '-';
    }

    public static String toTitleCase(String text) {
        StringBuilder builder = new StringBuilder();
        boolean whitespace = true;

        for (int i = 0; i < text.length(); ++i) {
            char letter = text.charAt(i);

            // If whitespace or whitespace substitute insert whitespace
            if (isWhitespaceChar(letter)) {
                // If we already had whitespace don't insert more whitespace
                if (!whitespace) {
                    builder.append(' ');
                }

                whitespace = true;
                continue;
            }

            // If we are preceded by whitespace, use a title case letter
            // otherwise, use a lower case letter.
            if (whitespace) {
                builder.append(Character.toTitleCase(letter));
            } else {
                builder.append(Character.toLowerCase(letter));
            }

            whitespace = false;
        }

        return builder.toString();
    }

    public static String toUppercaseTitle(String text) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < text.length(); ++i) {
            char letter = text.charAt(i);

            // If whitespace or whitespace substitute insert whitespace
            if (isWhitespaceChar(letter)) {
                builder.append(' ');
                continue;
            }

            // If we are preceded by whitespace, use a title case letter
            // otherwise, use a lower case letter.
            builder.append(Character.toUpperCase(letter));
        }

        return builder.toString();
    }
}
