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

package org.apache.commons.text.translate;

import java.io.IOException;
import java.io.Writer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Translates a value using a lookup table.
 *
 * @since 1.0
 */
public class LookupTranslator extends CharSequenceTranslator {

    /** The mapping to be used in translation. */
    private final Map<String, String> lookupMap;
    /** The first character of each key in the lookupMap. */
    private final BitSet prefixSet;
    /** The length of the shortest key in the lookupMap. */
    private final int shortest;
    /** The length of the longest key in the lookupMap. */
    private final int longest;

    /**
     * Constructs the lookup table to be used in translation.
     *
     * Note that, as of Lang 3.1 (the origin of this code), the key to the lookup table is converted to a java.lang.String. This is because we need the key to
     * support hashCode and equals(Object), allowing it to be the key for a HashMap. See LANG-882.
     *
     * @param lookupMap Map&lt;CharSequence, CharSequence&gt; table of translator mappings, may not be null.
     */
    public LookupTranslator(final Map<CharSequence, CharSequence> lookupMap) {
        Objects.requireNonNull(lookupMap, "lookupMap");
        this.lookupMap = new HashMap<>();
        this.prefixSet = new BitSet();
        int currentShortest = Integer.MAX_VALUE;
        int currentLongest = 0;
        for (final Map.Entry<CharSequence, CharSequence> pair : lookupMap.entrySet()) {
            this.lookupMap.put(pair.getKey().toString(), pair.getValue().toString());
            this.prefixSet.set(pair.getKey().charAt(0));
            final int sz = pair.getKey().length();
            if (sz < currentShortest) {
                currentShortest = sz;
            }
            if (sz > currentLongest) {
                currentLongest = sz;
            }
        }
        this.shortest = currentShortest;
        this.longest = currentLongest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int translate(final CharSequence input, final int index, final Writer writer) throws IOException {
        // check if translation exists for the input at position index
        if (prefixSet.get(input.charAt(index))) {
            int max = longest;
            if (index + longest > input.length()) {
                max = input.length() - index;
            }
            // implement greedy algorithm by trying maximum match first
            for (int i = max; i >= shortest; i--) {
                final CharSequence subSeq = input.subSequence(index, index + i);
                final String result = lookupMap.get(subSeq.toString());
                if (result != null) {
                    writer.write(result);
                    return Character.codePointCount(subSeq, 0, subSeq.length());
                }
            }
        }
        return 0;
    }
}
