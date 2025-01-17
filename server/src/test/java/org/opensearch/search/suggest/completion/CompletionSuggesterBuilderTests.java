/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.search.suggest.completion;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.common.xcontent.ToXContent;
import org.opensearch.index.analysis.AnalyzerScope;
import org.opensearch.index.analysis.NamedAnalyzer;
import org.opensearch.index.mapper.CompletionFieldMapper.CompletionFieldType;
import org.opensearch.index.mapper.MappedFieldType;
import org.opensearch.search.suggest.AbstractSuggestionBuilderTestCase;
import org.opensearch.search.suggest.SuggestionSearchContext.SuggestionContext;
import org.opensearch.search.suggest.completion.context.CategoryQueryContext;
import org.opensearch.search.suggest.completion.context.ContextBuilder;
import org.opensearch.search.suggest.completion.context.ContextMapping;
import org.opensearch.search.suggest.completion.context.ContextMapping.InternalQueryContext;
import org.opensearch.search.suggest.completion.context.ContextMappings;
import org.opensearch.search.suggest.completion.context.GeoQueryContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.instanceOf;

public class CompletionSuggesterBuilderTests extends AbstractSuggestionBuilderTestCase<CompletionSuggestionBuilder> {

    private static final String[] SHUFFLE_PROTECTED_FIELDS = new String[] { CompletionSuggestionBuilder.CONTEXTS_FIELD.getPreferredName() };
    private static String categoryContextName;
    private static String geoQueryContextName;
    private static List<ContextMapping<?>> contextMappings = new ArrayList<>();

    @Override
    protected CompletionSuggestionBuilder randomSuggestionBuilder() {
        return randomCompletionSuggestionBuilder();
    }

    public static CompletionSuggestionBuilder randomCompletionSuggestionBuilder() {
        // lazy initialization of context names and mappings, cannot be done in some init method because other test
        // also create random CompletionSuggestionBuilder instances
        if (categoryContextName == null) {
            categoryContextName = randomAlphaOfLength(10);
        }
        if (geoQueryContextName == null) {
            geoQueryContextName = randomAlphaOfLength(10);
        }
        if (contextMappings.isEmpty()) {
            contextMappings.add(ContextBuilder.category(categoryContextName).build());
            contextMappings.add(ContextBuilder.geo(geoQueryContextName).build());
        }
        // lazy initialization of context names and mappings, cannot be done in some init method because other test
        // also create random CompletionSuggestionBuilder instances
        if (categoryContextName == null) {
            categoryContextName = randomAlphaOfLength(10);
        }
        if (geoQueryContextName == null) {
            geoQueryContextName = randomAlphaOfLength(10);
        }
        if (contextMappings.isEmpty()) {
            contextMappings.add(ContextBuilder.category(categoryContextName).build());
            contextMappings.add(ContextBuilder.geo(geoQueryContextName).build());
        }
        CompletionSuggestionBuilder testBuilder = new CompletionSuggestionBuilder(randomAlphaOfLengthBetween(2, 20));
        setCommonPropertiesOnRandomBuilder(testBuilder);
        switch (randomIntBetween(0, 3)) {
            case 0:
                testBuilder.prefix(randomAlphaOfLength(10));
                break;
            case 1:
                testBuilder.prefix(randomAlphaOfLength(10), FuzzyOptionsTests.randomFuzzyOptions());
                break;
            case 2:
                testBuilder.prefix(randomAlphaOfLength(10), randomFrom(Fuzziness.ZERO, Fuzziness.ONE, Fuzziness.TWO));
                break;
            case 3:
                testBuilder.regex(randomAlphaOfLength(10), RegexOptionsTests.randomRegexOptions());
                break;
        }
        Map<String, List<? extends ToXContent>> contextMap = new HashMap<>();
        if (randomBoolean()) {
            int numContext = randomIntBetween(1, 5);
            List<CategoryQueryContext> contexts = new ArrayList<>(numContext);
            for (int i = 0; i < numContext; i++) {
                contexts.add(CategoryQueryContextTests.randomCategoryQueryContext());
            }
            contextMap.put(categoryContextName, contexts);
        }
        if (randomBoolean()) {
            int numContext = randomIntBetween(1, 5);
            List<GeoQueryContext> contexts = new ArrayList<>(numContext);
            for (int i = 0; i < numContext; i++) {
                contexts.add(GeoQueryContextTests.randomGeoQueryContext());
            }
            contextMap.put(geoQueryContextName, contexts);
        }
        testBuilder.contexts(contextMap);
        testBuilder.skipDuplicates(randomBoolean());
        return testBuilder;
    }

    /**
     * exclude the "contexts" field from recursive random shuffling in fromXContent tests or else
     * the equals() test will fail because their {@link BytesReference} representation isn't the same
     */
    @Override
    protected String[] shuffleProtectedFields() {
        return SHUFFLE_PROTECTED_FIELDS;
    }

    @Override
    protected void mutateSpecificParameters(CompletionSuggestionBuilder builder) throws IOException {
        switch (randomIntBetween(0, 5)) {
            case 0:
                int nCatContext = randomIntBetween(1, 5);
                List<CategoryQueryContext> contexts = new ArrayList<>(nCatContext);
                for (int i = 0; i < nCatContext; i++) {
                    contexts.add(CategoryQueryContextTests.randomCategoryQueryContext());
                }
                builder.contexts(Collections.singletonMap(randomAlphaOfLength(10), contexts));
                break;
            case 1:
                int nGeoContext = randomIntBetween(1, 5);
                List<GeoQueryContext> geoContexts = new ArrayList<>(nGeoContext);
                for (int i = 0; i < nGeoContext; i++) {
                    geoContexts.add(GeoQueryContextTests.randomGeoQueryContext());
                }
                builder.contexts(Collections.singletonMap(randomAlphaOfLength(10), geoContexts));
                break;
            case 2:
                builder.prefix(randomAlphaOfLength(10), FuzzyOptionsTests.randomFuzzyOptions());
                break;
            case 3:
                builder.prefix(randomAlphaOfLength(10), randomFrom(Fuzziness.ZERO, Fuzziness.ONE, Fuzziness.TWO));
                break;
            case 4:
                builder.regex(randomAlphaOfLength(10), RegexOptionsTests.randomRegexOptions());
                break;
            case 5:
                builder.skipDuplicates(!builder.skipDuplicates);
                break;
            default:
                throw new IllegalStateException("should not through");
        }
    }

    @Override
    protected MappedFieldType mockFieldType(String fieldName, boolean analyzerSet) {
        if (analyzerSet == false) {
            CompletionFieldType completionFieldType = new CompletionFieldType(fieldName, null, Collections.emptyMap());
            completionFieldType.setContextMappings(new ContextMappings(contextMappings));
            return completionFieldType;
        }
        CompletionFieldType completionFieldType = new CompletionFieldType(fieldName,
            new NamedAnalyzer("fieldSearchAnalyzer", AnalyzerScope.INDEX, new SimpleAnalyzer()),
            Collections.emptyMap());
        completionFieldType.setContextMappings(new ContextMappings(contextMappings));
        return completionFieldType;
    }

    @Override
    protected void assertSuggestionContext(CompletionSuggestionBuilder builder, SuggestionContext context) throws IOException {
        assertThat(context, instanceOf(CompletionSuggestionContext.class));
        assertThat(context.getSuggester(), instanceOf(CompletionSuggester.class));
        CompletionSuggestionContext completionSuggestionCtx = (CompletionSuggestionContext) context;
        assertThat(completionSuggestionCtx.getFieldType(), instanceOf(CompletionFieldType.class) );
        assertEquals(builder.fuzzyOptions, completionSuggestionCtx.getFuzzyOptions());
        Map<String, List<InternalQueryContext>> parsedContextBytes;
        parsedContextBytes = CompletionSuggestionBuilder.parseContextBytes(builder.contextBytes, xContentRegistry(),
                new ContextMappings(contextMappings));
        Map<String, List<InternalQueryContext>> queryContexts = completionSuggestionCtx.getQueryContexts();
        assertEquals(parsedContextBytes.keySet(), queryContexts.keySet());
        for (String contextName : queryContexts.keySet()) {
            assertEquals(parsedContextBytes.get(contextName), queryContexts.get(contextName));
        }
        assertEquals(builder.regexOptions, completionSuggestionCtx.getRegexOptions());
        assertEquals(builder.skipDuplicates, completionSuggestionCtx.isSkipDuplicates());
    }
}
