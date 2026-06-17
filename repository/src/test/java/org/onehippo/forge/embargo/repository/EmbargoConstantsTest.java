/*
 * Copyright 2026 Bloomreach B.V. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.embargo.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EmbargoConstants} — verifies constant values that downstream
 * code depends on (JCR node type names, query templates, property names).
 */
class EmbargoConstantsTest {

    @Test
    void embargoJobConstant_hasExpectedValue() {
        assertEquals("embargo:job", EmbargoConstants.EMBARGO_JOB);
    }

    @Test
    void embargoMixinName_hasExpectedValue() {
        assertEquals("embargo:handle", EmbargoConstants.EMBARGO_MIXIN_NAME);
    }

    @Test
    void embargoDocumentMixinName_hasExpectedValue() {
        assertEquals("embargo:document", EmbargoConstants.EMBARGO_DOCUMENT_MIXIN_NAME);
    }

    @Test
    void embargoGroupPropertyName_hasExpectedValue() {
        assertEquals("embargo:groups", EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME);
    }

    @Test
    void embargoScheduleRequestNodeName_hasExpectedValue() {
        assertEquals("embargo:request", EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME);
    }

    @Test
    void selectGroupsQuery_containsPlaceholder() {
        assertNotNull(EmbargoConstants.SELECT_GROUPS_QUERY);
        assertTrue(EmbargoConstants.SELECT_GROUPS_QUERY.contains("{}"),
                "Query template must contain '{}' placeholder for user identity substitution");
    }

    @Test
    void selectGroupsQuery_containsExpectedFromClause() {
        assertTrue(EmbargoConstants.SELECT_GROUPS_QUERY.contains("hipposys:group"),
                "Query must target hipposys:group nodes");
    }

    @Test
    void embargoDomainPath_hasExpectedValue() {
        assertEquals("hippo:configuration/hippo:domains/embargo", EmbargoConstants.EMBARGO_DOMAIN_PATH);
    }

    @Test
    void hippoSchedTriggersDefault_hasExpectedValue() {
        assertEquals("hipposched:triggers/embargo", EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT);
    }

    @Test
    void hippoSchedFiretimeProperty_hasExpectedValue() {
        assertEquals("hipposched:startTime", EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME);
    }

    @Test
    void adminGroupName_hasExpectedValue() {
        assertEquals("admin", EmbargoConstants.ADMIN_GROUP_NAME);
    }

    @Test
    void hippoHandle_hasExpectedValue() {
        assertEquals("hippo:handle", EmbargoConstants.HIPPO_HANDLE);
    }
}
