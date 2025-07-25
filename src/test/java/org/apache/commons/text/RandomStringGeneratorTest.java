/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

import org.apache.commons.lang3.ArraySorter;
import org.apache.commons.text.RandomStringGenerator.Builder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link RandomStringGenerator}.
 */
class RandomStringGeneratorTest {

    private static final CharacterPredicate A_FILTER = codePoint -> codePoint == 'a';
    private static final CharacterPredicate B_FILTER = codePoint -> codePoint == 'b';

    private static int codePointLength(final String s) {
        return s.codePointCount(0, s.length());
    }

    @Test
    void testBadMaximumCodePoint() {
        assertThrowsExactly(IllegalArgumentException.class, () -> RandomStringGenerator.builder().withinRange(0, Character.MAX_CODE_POINT + 1));
    }

    @Test
    void testBadMinAndMax() {
        assertThrowsExactly(IllegalArgumentException.class, () -> RandomStringGenerator.builder().withinRange(2, 1));
    }

    @Test
    void testBadMinimumCodePoint() {
        assertThrowsExactly(IllegalArgumentException.class, () -> RandomStringGenerator.builder().withinRange(-1, 1));
    }

    @Test
    void testBuildDeprecated() {
        final RandomStringGenerator.Builder builder = RandomStringGenerator.builder().withinRange('a', 'z').filteredBy(A_FILTER);
        final String str = builder.filteredBy(B_FILTER).build().generate(100);
        for (final char c : str.toCharArray()) {
            assertEquals('b', c);
        }
    }

    @Test
    void testChangeOfFilter() {
        final RandomStringGenerator.Builder builder = RandomStringGenerator.builder().withinRange('a', 'z').filteredBy(A_FILTER);
        final String str = builder.filteredBy(B_FILTER).get().generate(100);
        for (final char c : str.toCharArray()) {
            assertEquals('b', c);
        }
    }

    @Test
    void testGenerateMinMaxLength() {
        final int minLength = 0;
        final int maxLength = 3;
        final RandomStringGenerator generator = RandomStringGenerator.builder().get();
        final String str = generator.generate(minLength, maxLength);
        final int codePointLength = codePointLength(str);
        assertTrue(codePointLength >= minLength && codePointLength <= maxLength);
    }

    @Test
    void testGenerateMinMaxLengthInvalidLength() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            final RandomStringGenerator generator = RandomStringGenerator.builder().get();
            generator.generate(-1, 0);
        });
    }

    @Test
    void testGenerateMinMaxLengthMinGreaterThanMax() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            final RandomStringGenerator generator = RandomStringGenerator.builder().get();
            generator.generate(1, 0);
        });
    }

    @Test
    void testGenerateTakingIntThrowsNullPointerException() {
        assertThrowsExactly(NullPointerException.class, () -> {
            final RandomStringGenerator.Builder randomStringGeneratorBuilder = RandomStringGenerator.builder();
            final CharacterPredicate[] characterPredicateArray = new CharacterPredicate[2];
            randomStringGeneratorBuilder.filteredBy(characterPredicateArray);
            final RandomStringGenerator randomStringGenerator = randomStringGeneratorBuilder.get();
            randomStringGenerator.generate(18);
        });
    }

    @Test
    void testInvalidLength() {
        assertThrowsExactly(IllegalArgumentException.class, () -> RandomStringGenerator.builder().get().generate(-1));
    }

    @Test
    void testMultipleFilters() {
        final String str = RandomStringGenerator.builder().withinRange('a', 'd').filteredBy(A_FILTER, B_FILTER).get().generate(5000);
        boolean aFound = false;
        boolean bFound = false;
        for (final char c : str.toCharArray()) {
            if (c == 'a') {
                aFound = true;
            } else if (c == 'b') {
                bFound = true;
            } else {
                fail("Invalid character");
            }
        }
        assertTrue(aFound && bFound);
    }

    @Test
    void testNoLoneSurrogates() {
        final int length = 5000;
        final String str = RandomStringGenerator.builder().get().generate(length);
        char lastChar = str.charAt(0);
        for (int i = 1; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (Character.isLowSurrogate(c)) {
                assertTrue(Character.isHighSurrogate(lastChar));
            }
            if (Character.isHighSurrogate(lastChar)) {
                assertTrue(Character.isLowSurrogate(c));
            }
            if (Character.isHighSurrogate(c)) {
                // test this isn't the last character in the string
                assertTrue(i + 1 < str.length());
            }
            lastChar = c;
        }
    }

    @Test
    void testNoPrivateCharacters() {
        final int startOfPrivateBMPChars = 0xE000;
        // Request a string in an area of the Basic Multilingual Plane that is
        // largely occupied by private characters
        final String str = RandomStringGenerator.builder().withinRange(startOfPrivateBMPChars, Character.MIN_SUPPLEMENTARY_CODE_POINT - 1).get()
                .generate(5000);
        int i = 0;
        do {
            final int codePoint = str.codePointAt(i);
            assertFalse(Character.getType(codePoint) == Character.PRIVATE_USE);
            i += Character.charCount(codePoint);
        } while (i < str.length());
    }

    @Test
    void testPasswordExample() {
        final char[] punctuation = ArraySorter
                .sort(new char[] { '!', '"', '#', '$', '&', '\'', '(', ')', ',', '.', ':', ';', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~' });
        // @formatter:off
        final RandomStringGenerator generator = RandomStringGenerator.builder()
                .setAccumulate(true)
                .withinRange('a', 'z')
                .withinRange('A', 'Z')
                .withinRange('0', '9')
                .selectFrom(punctuation)
                .get();
        // @formatter:on
        final String randomText = generator.generate(10);
        for (final char c : randomText.toCharArray()) {
            assertTrue(Character.isLetter(c) || Character.isDigit(c) || Arrays.binarySearch(punctuation, c) >= 0);
        }
    }

    @Test
    void testRemoveFilters() {
        final RandomStringGenerator.Builder builder = RandomStringGenerator.builder().withinRange('a', 'z').filteredBy(A_FILTER);
        builder.filteredBy();
        final String str = builder.get().generate(100);
        for (final char c : str.toCharArray()) {
            if (c != 'a') {
                // filter was successfully removed
                return;
            }
        }
        fail("Filter appears to have remained in place");
    }

    @Test
    void testSelectFromCharArray() {
        final String str = "abc";
        final char[] charArray = str.toCharArray();
        final RandomStringGenerator generator = RandomStringGenerator.builder().selectFrom(charArray).get();
        final String randomText = generator.generate(5);
        for (final char c : randomText.toCharArray()) {
            assertTrue(str.indexOf(c) != -1);
        }
    }

    @Test
    void testSelectFromCharVarargs() {
        final String str = "abc";
        final RandomStringGenerator generator = RandomStringGenerator.builder().selectFrom('a', 'b', 'c').get();
        final String randomText = generator.generate(5);
        for (final char c : randomText.toCharArray()) {
            assertTrue(str.indexOf(c) != -1);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSelectFromCharVarargs2(final boolean accumulate) {
        final String str = "abcde";
        // @formatter:off
        final RandomStringGenerator generator = RandomStringGenerator.builder()
                .setAccumulate(accumulate)
                .selectFrom()
                .selectFrom(null)
                .selectFrom('a', 'b')
                .selectFrom('a', 'b', 'c')
                .selectFrom('a', 'b', 'c', 'd')
                .selectFrom('a', 'b', 'c', 'd', 'e') // only this last call matters when accumulate is false
                .get();
        // @formatter:on
        final String randomText = generator.generate(10);
        for (final char c : randomText.toCharArray()) {
            assertTrue(str.indexOf(c) != -1);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSelectFromCharVarargs3(final boolean accumulate) {
        final String str = "abcde";
        // @formatter:off
        final RandomStringGenerator generator = RandomStringGenerator.builder()
                .setAccumulate(accumulate)
                .selectFrom('a', 'b', 'c', 'd', 'e')
                .selectFrom('a', 'b', 'c', 'd')
                .selectFrom('a', 'b', 'c')
                .selectFrom('a', 'b')
                .selectFrom(null)
                .selectFrom()
                .get();
        // @formatter:on
        final String randomText = generator.generate(10);
        for (final char c : randomText.toCharArray()) {
            assertEquals(accumulate, str.indexOf(c) != -1);
        }
    }

    @Test
    void testSelectFromCharVarargSize1() {
        final RandomStringGenerator generator = RandomStringGenerator.builder().selectFrom('a').get();
        final String randomText = generator.generate(5);
        for (final char c : randomText.toCharArray()) {
            assertEquals('a', c);
        }
    }

    @Test
    void testSelectFromEmptyCharVarargs() {
        final RandomStringGenerator generator = RandomStringGenerator.builder().selectFrom().get();
        final String randomText = generator.generate(5);
        for (final char c : randomText.toCharArray()) {
            assertTrue(c >= Character.MIN_CODE_POINT && c <= Character.MAX_CODE_POINT);
        }
    }

    @Test
    void testSelectFromNullCharVarargs() {
        final int length = 5;
        RandomStringGenerator generator = RandomStringGenerator.builder().selectFrom(null).get();
        String randomText = generator.generate(length);
        assertEquals(length, codePointLength(randomText));
        for (final char c : randomText.toCharArray()) {
            assertTrue(c >= Character.MIN_CODE_POINT && c <= Character.MAX_CODE_POINT);
        }
        //
        final Builder builder = RandomStringGenerator.builder().selectFrom('a');
        generator = builder.get();
        randomText = generator.generate(length);
        for (final char c : randomText.toCharArray()) {
            assertEquals('a', c);
        }
        // null input resets
        generator = builder.selectFrom(null).get();
        randomText = generator.generate(length);
        assertEquals(length, codePointLength(randomText));
        for (final char c : randomText.toCharArray()) {
            assertTrue(c >= Character.MIN_CODE_POINT && c <= Character.MAX_CODE_POINT);
        }
    }

    @Test
    void testSetLength() {
        final int length = 99;
        final RandomStringGenerator generator = RandomStringGenerator.builder().get();
        final String str = generator.generate(length);
        assertEquals(length, codePointLength(str));
    }

    @Test
    void testUsingRandomIntUnaryOperator() {
        final char testChar = 'a';
        final IntUnaryOperator testRandom = n -> testChar;
        final String str = RandomStringGenerator.builder().usingRandom(testRandom).get().generate(10);
        for (final char c : str.toCharArray()) {
            assertEquals(testChar, c);
        }
    }

    @Test
    void testUsingRandomTextRandomProvider() {
        final char testChar = 'a';
        final TextRandomProvider testRandom = n -> testChar;
        final String str = RandomStringGenerator.builder().usingRandom(testRandom).get().generate(10);
        for (final char c : str.toCharArray()) {
            assertEquals(testChar, c);
        }
    }

    @Test
    void testWithinMultipleRanges() {
        final int length = 5000;
        final char[][] pairs = { { 'a', 'z' }, { '0', '9' } };
        // @formatter:off
        final RandomStringGenerator generator = RandomStringGenerator.builder()
                .withinRange()
                .withinRange((char[][]) null)
                .withinRange(pairs)
                .get();
        // @formatter:on
        final String str = generator.generate(length);
        int minimumCodePoint = 0, maximumCodePoint = 0;
        for (final char[] pair : pairs) {
            minimumCodePoint = Math.min(minimumCodePoint, pair[0]);
            maximumCodePoint = Math.max(maximumCodePoint, pair[1]);
        }
        int i = 0;
        do {
            final int codePoint = str.codePointAt(i);
            assertTrue(codePoint >= minimumCodePoint && codePoint <= maximumCodePoint);
            i += Character.charCount(codePoint);
        } while (i < str.length());
    }

    @Test
    void testWithinRange() {
        final int length = 5000;
        final int minimumCodePoint = 'a';
        final int maximumCodePoint = 'z';
        final RandomStringGenerator generator = RandomStringGenerator.builder().withinRange(minimumCodePoint, maximumCodePoint).get();
        final String str = generator.generate(length);
        int i = 0;
        do {
            final int codePoint = str.codePointAt(i);
            assertTrue(codePoint >= minimumCodePoint && codePoint <= maximumCodePoint);
            i += Character.charCount(codePoint);
        } while (i < str.length());
    }

    @Test
    void testZeroLength() {
        final RandomStringGenerator generator = RandomStringGenerator.builder().get();
        assertEquals("", generator.generate(0));
    }
}
