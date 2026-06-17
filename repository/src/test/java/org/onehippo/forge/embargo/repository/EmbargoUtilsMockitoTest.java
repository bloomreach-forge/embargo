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

import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JUnit 5 / Mockito unit tests for {@link EmbargoUtils}.
 * These tests are independent of any live JCR repository.
 */
@ExtendWith(MockitoExtension.class)
class EmbargoUtilsMockitoTest {

    @Mock
    private Session session;
    @Mock
    private Workspace workspace;
    @Mock
    private QueryManager queryManager;
    @Mock
    private Query query;
    @Mock
    private QueryResult queryResult;
    @Mock
    private NodeIterator nodeIterator;
    @Mock
    private Node rootNode;
    @Mock
    private Node handleNode;
    @Mock
    private Node documentNode;
    @Mock
    private Node requestNode;
    @Mock
    private Node triggersNode;
    @Mock
    private Property property;
    @Mock
    private Value value;
    @Mock
    private VersionManager versionManager;

    // -------------------------------------------------------------------------
    // getAllUserGroups
    // -------------------------------------------------------------------------

    @Test
    void getAllUserGroups_repositoryException_returnsEmptyArray() throws Exception {
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenThrow(new RepositoryException("db down"));

        String[] result = EmbargoUtils.getAllUserGroups(session, "user1");

        assertArrayEquals(EmbargoUtils.EMPTY_ARRAY, result);
    }

    @Test
    void getAllUserGroups_noGroupNodes_returnsEmptyArray() throws Exception {
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        String expectedSql = EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", "alice");
        when(queryManager.createQuery(eq(expectedSql), eq(Query.SQL))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(false);

        String[] result = EmbargoUtils.getAllUserGroups(session, "alice");

        assertEquals(0, result.length);
    }

    @Test
    void getAllUserGroups_singleGroupNode_returnsGroupName() throws Exception {
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        String expectedSql = EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", "bob");
        when(queryManager.createQuery(eq(expectedSql), eq(Query.SQL))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, false);
        when(nodeIterator.nextNode()).thenReturn(handleNode);
        when(handleNode.getName()).thenReturn("editors");

        String[] result = EmbargoUtils.getAllUserGroups(session, "bob");

        assertArrayEquals(new String[]{"editors"}, result);
    }

    // -------------------------------------------------------------------------
    // isAdminUser
    // -------------------------------------------------------------------------

    @Test
    void isAdminUser_userInAdminGroup_returnsTrue() throws Exception {
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        String expectedSql = EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", "superuser");
        when(queryManager.createQuery(eq(expectedSql), eq(Query.SQL))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, false);
        when(nodeIterator.nextNode()).thenReturn(handleNode);
        when(handleNode.getName()).thenReturn("admin");

        assertTrue(EmbargoUtils.isAdminUser(session, "superuser"));
    }

    @Test
    void isAdminUser_userNotInAdminGroup_returnsFalse() throws Exception {
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        String expectedSql = EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", "regularuser");
        when(queryManager.createQuery(eq(expectedSql), eq(Query.SQL))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, false);
        when(nodeIterator.nextNode()).thenReturn(handleNode);
        when(handleNode.getName()).thenReturn("editors");

        assertFalse(EmbargoUtils.isAdminUser(session, "regularuser"));
    }

    @Test
    void isAdminUser_noGroups_returnsFalse() throws Exception {
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        String expectedSql = EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", "nobody");
        when(queryManager.createQuery(eq(expectedSql), eq(Query.SQL))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(false);

        assertFalse(EmbargoUtils.isAdminUser(session, "nobody"));
    }

    // -------------------------------------------------------------------------
    // getAllEmbargoEnabledGroups
    // -------------------------------------------------------------------------

    @Test
    void getAllEmbargoEnabledGroups_embargoDomainMissing_returnsEmptyList() throws Exception {
        when(session.getRootNode()).thenReturn(rootNode);
        when(rootNode.getNode(EmbargoConstants.EMBARGO_DOMAIN_PATH)).thenThrow(new RepositoryException("not found"));

        List<String> result = EmbargoUtils.getAllEmbargoEnabledGroups(session);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllEmbargoEnabledGroups_noAuthRoleChildren_returnsEmptyList() throws Exception {
        when(session.getRootNode()).thenReturn(rootNode);
        when(rootNode.getNode(EmbargoConstants.EMBARGO_DOMAIN_PATH)).thenReturn(handleNode);
        when(handleNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(false);

        List<String> result = EmbargoUtils.getAllEmbargoEnabledGroups(session);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllEmbargoEnabledGroups_authRoleWithGroups_returnsGroups() throws Exception {
        when(session.getRootNode()).thenReturn(rootNode);
        when(rootNode.getNode(EmbargoConstants.EMBARGO_DOMAIN_PATH)).thenReturn(handleNode);
        when(handleNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, false);
        when(nodeIterator.nextNode()).thenReturn(documentNode);
        when(documentNode.isNodeType(EmbargoConstants.HIPPOSYS_AUTHROLE)).thenReturn(true);
        when(documentNode.hasProperty(HippoNodeType.HIPPO_GROUPS)).thenReturn(true);
        when(documentNode.getProperty(HippoNodeType.HIPPO_GROUPS)).thenReturn(property);
        when(property.getValues()).thenReturn(new Value[]{value});
        when(value.getString()).thenReturn("embargo-editors");

        List<String> result = EmbargoUtils.getAllEmbargoEnabledGroups(session);

        assertEquals(1, result.size());
        assertTrue(result.contains("embargo-editors"));
    }

    @Test
    void getAllEmbargoEnabledGroups_nonAuthRoleChild_isIgnored() throws Exception {
        when(session.getRootNode()).thenReturn(rootNode);
        when(rootNode.getNode(EmbargoConstants.EMBARGO_DOMAIN_PATH)).thenReturn(handleNode);
        when(handleNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, false);
        when(nodeIterator.nextNode()).thenReturn(documentNode);
        when(documentNode.isNodeType(EmbargoConstants.HIPPOSYS_AUTHROLE)).thenReturn(false);

        List<String> result = EmbargoUtils.getAllEmbargoEnabledGroups(session);

        assertTrue(result.isEmpty());
        verify(documentNode, never()).hasProperty(anyString());
    }

    // -------------------------------------------------------------------------
    // getEmbargoExpirationDate
    // -------------------------------------------------------------------------

    @Test
    void getEmbargoExpirationDate_nodeNotHandle_returnsNull() throws Exception {
        when(handleNode.isNodeType(HippoNodeType.NT_HANDLE)).thenReturn(false);

        Calendar result = EmbargoUtils.getEmbargoExpirationDate(handleNode);

        assertNull(result);
    }

    @Test
    void getEmbargoExpirationDate_handleWithoutRequestNode_returnsNull() throws Exception {
        when(handleNode.isNodeType(HippoNodeType.NT_HANDLE)).thenReturn(true);
        when(handleNode.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).thenReturn(false);

        Calendar result = EmbargoUtils.getEmbargoExpirationDate(handleNode);

        assertNull(result);
    }

    @Test
    void getEmbargoExpirationDate_requestNodeWithoutTriggersNode_returnsNull() throws Exception {
        when(handleNode.isNodeType(HippoNodeType.NT_HANDLE)).thenReturn(true);
        when(handleNode.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).thenReturn(true);
        when(handleNode.getNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).thenReturn(requestNode);
        when(requestNode.hasNode(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT)).thenReturn(false);

        Calendar result = EmbargoUtils.getEmbargoExpirationDate(handleNode);

        assertNull(result);
    }

    @Test
    void getEmbargoExpirationDate_triggerNodeWithoutFiretimeProperty_returnsNull() throws Exception {
        when(handleNode.isNodeType(HippoNodeType.NT_HANDLE)).thenReturn(true);
        when(handleNode.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).thenReturn(true);
        when(handleNode.getNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).thenReturn(requestNode);
        when(requestNode.hasNode(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT)).thenReturn(true);
        when(requestNode.getNode(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT)).thenReturn(triggersNode);
        when(triggersNode.hasProperty(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME)).thenReturn(false);

        Calendar result = EmbargoUtils.getEmbargoExpirationDate(handleNode);

        assertNull(result);
    }

    @Test
    void getEmbargoExpirationDate_allPresent_returnsCalendar() throws Exception {
        Calendar expected = Calendar.getInstance();
        when(handleNode.isNodeType(HippoNodeType.NT_HANDLE)).thenReturn(true);
        when(handleNode.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).thenReturn(true);
        when(handleNode.getNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).thenReturn(requestNode);
        when(requestNode.hasNode(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT)).thenReturn(true);
        when(requestNode.getNode(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT)).thenReturn(triggersNode);
        when(triggersNode.hasProperty(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME)).thenReturn(true);
        when(triggersNode.getProperty(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME)).thenReturn(property);
        when(property.getDate()).thenReturn(expected);

        Calendar result = EmbargoUtils.getEmbargoExpirationDate(handleNode);

        assertSame(expected, result);
    }

    // -------------------------------------------------------------------------
    // isVisibleInPreview
    // -------------------------------------------------------------------------

    @Test
    void isVisibleInPreview_previewAvailability_returnsTrue() throws Exception {
        Value previewValue = org.mockito.Mockito.mock(Value.class);
        when(previewValue.getString()).thenReturn("preview");
        when(documentNode.hasProperty(HippoNodeType.HIPPO_AVAILABILITY)).thenReturn(true);
        when(documentNode.getProperty(HippoNodeType.HIPPO_AVAILABILITY)).thenReturn(property);
        when(property.getValues()).thenReturn(new Value[]{previewValue});

        assertTrue(EmbargoUtils.isVisibleInPreview(documentNode));
    }

    @Test
    void isVisibleInPreview_liveOnlyAvailability_returnsFalse() throws Exception {
        Value liveValue = org.mockito.Mockito.mock(Value.class);
        when(liveValue.getString()).thenReturn("live");
        when(documentNode.hasProperty(HippoNodeType.HIPPO_AVAILABILITY)).thenReturn(true);
        when(documentNode.getProperty(HippoNodeType.HIPPO_AVAILABILITY)).thenReturn(property);
        when(property.getValues()).thenReturn(new Value[]{liveValue});

        assertFalse(EmbargoUtils.isVisibleInPreview(documentNode));
    }

    @Test
    void isVisibleInPreview_noAvailabilityProperty_returnsFalse() throws Exception {
        when(documentNode.hasProperty(HippoNodeType.HIPPO_AVAILABILITY)).thenReturn(false);
        when(documentNode.getPath()).thenReturn("/content/test");

        assertFalse(EmbargoUtils.isVisibleInPreview(documentNode));
    }

    @Test
    void isVisibleInPreview_repositoryException_returnsFalse() throws Exception {
        when(documentNode.hasProperty(HippoNodeType.HIPPO_AVAILABILITY)).thenThrow(new RepositoryException("error"));

        assertFalse(EmbargoUtils.isVisibleInPreview(documentNode));
    }

    @Test
    void isVisibleInPreview_multipleAvailabilities_includesPreview_returnsTrue() throws Exception {
        Value liveValue = org.mockito.Mockito.mock(Value.class);
        Value previewValue = org.mockito.Mockito.mock(Value.class);
        when(liveValue.getString()).thenReturn("live");
        when(previewValue.getString()).thenReturn("preview");
        when(documentNode.hasProperty(HippoNodeType.HIPPO_AVAILABILITY)).thenReturn(true);
        when(documentNode.getProperty(HippoNodeType.HIPPO_AVAILABILITY)).thenReturn(property);
        when(property.getValues()).thenReturn(new Value[]{liveValue, previewValue});

        assertTrue(EmbargoUtils.isVisibleInPreview(documentNode));
    }

    // -------------------------------------------------------------------------
    // getDocumentVariants
    // -------------------------------------------------------------------------

    @Test
    void getDocumentVariants_noChildren_returnsEmptyArray() throws Exception {
        when(handleNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(false);

        Node[] result = EmbargoUtils.getDocumentVariants(handleNode);

        assertEquals(0, result.length);
    }

    @Test
    void getDocumentVariants_childWithSameName_returned() throws Exception {
        when(handleNode.getName()).thenReturn("my-document");
        when(handleNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, false);
        when(nodeIterator.nextNode()).thenReturn(documentNode);
        when(documentNode.getName()).thenReturn("my-document");

        Node[] result = EmbargoUtils.getDocumentVariants(handleNode);

        assertEquals(1, result.length);
        assertSame(documentNode, result[0]);
    }

    @Test
    void getDocumentVariants_childWithDifferentName_excluded() throws Exception {
        when(handleNode.getName()).thenReturn("my-document");
        when(handleNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, false);
        when(nodeIterator.nextNode()).thenReturn(documentNode);
        when(documentNode.getName()).thenReturn("hippo:request");

        Node[] result = EmbargoUtils.getDocumentVariants(handleNode);

        assertEquals(0, result.length);
    }

    // -------------------------------------------------------------------------
    // extractHandle
    // -------------------------------------------------------------------------

    @Test
    void extractHandle_nullNode_returnsNull() throws Exception {
        Node result = EmbargoUtils.extractHandle(null);

        assertNull(result);
    }

    @Test
    void extractHandle_nodeIsHandle_returnsSelf() throws Exception {
        when(handleNode.isNodeType(EmbargoConstants.HIPPO_HANDLE)).thenReturn(true);

        Node result = EmbargoUtils.extractHandle(handleNode);

        assertSame(handleNode, result);
    }

    @Test
    void extractHandle_parentIsHandle_returnsParent() throws Exception {
        when(documentNode.isNodeType(EmbargoConstants.HIPPO_HANDLE)).thenReturn(false);
        when(documentNode.getParent()).thenReturn(handleNode);
        when(handleNode.isNodeType(EmbargoConstants.HIPPO_HANDLE)).thenReturn(true);

        Node result = EmbargoUtils.extractHandle(documentNode);

        assertSame(handleNode, result);
    }

    @Test
    void extractHandle_neitherNodeNorParentIsHandle_returnsNull() throws Exception {
        when(documentNode.isNodeType(EmbargoConstants.HIPPO_HANDLE)).thenReturn(false);
        when(documentNode.getParent()).thenReturn(handleNode);
        when(handleNode.isNodeType(EmbargoConstants.HIPPO_HANDLE)).thenReturn(false);

        Node result = EmbargoUtils.extractHandle(documentNode);

        assertNull(result);
    }

    @Test
    void extractHandle_parentIsNull_returnsNull() throws Exception {
        when(documentNode.isNodeType(EmbargoConstants.HIPPO_HANDLE)).thenReturn(false);
        when(documentNode.getParent()).thenReturn(null);

        Node result = EmbargoUtils.extractHandle(documentNode);

        assertNull(result);
    }

    // -------------------------------------------------------------------------
    // removeMixin
    // -------------------------------------------------------------------------

    @Test
    void removeMixin_mixinPresent_callsRemoveMixin() throws Exception {
        NodeType mixinType = org.mockito.Mockito.mock(NodeType.class);
        when(mixinType.getName()).thenReturn(EmbargoConstants.EMBARGO_MIXIN_NAME);
        when(handleNode.getMixinNodeTypes()).thenReturn(new NodeType[]{mixinType});

        EmbargoUtils.removeMixin(handleNode, EmbargoConstants.EMBARGO_MIXIN_NAME);

        verify(handleNode).removeMixin(EmbargoConstants.EMBARGO_MIXIN_NAME);
    }

    @Test
    void removeMixin_mixinAbsent_doesNotCallRemoveMixin() throws Exception {
        NodeType otherMixin = org.mockito.Mockito.mock(NodeType.class);
        when(otherMixin.getName()).thenReturn("mix:referenceable");
        when(handleNode.getMixinNodeTypes()).thenReturn(new NodeType[]{otherMixin});

        EmbargoUtils.removeMixin(handleNode, EmbargoConstants.EMBARGO_MIXIN_NAME);

        verify(handleNode, never()).removeMixin(anyString());
    }

    @Test
    void removeMixin_noMixins_doesNotCallRemoveMixin() throws Exception {
        when(handleNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});

        EmbargoUtils.removeMixin(handleNode, EmbargoConstants.EMBARGO_MIXIN_NAME);

        verify(handleNode, never()).removeMixin(anyString());
    }

    // -------------------------------------------------------------------------
    // removeEmbargoForHandle
    // -------------------------------------------------------------------------

    @Test
    void removeEmbargoForHandle_checkedIn_checksOut() throws Exception {
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getVersionManager()).thenReturn(versionManager);
        when(handleNode.isCheckedOut()).thenReturn(false);
        when(handleNode.getPath()).thenReturn("/content/test");
        when(handleNode.hasProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME)).thenReturn(false);
        when(handleNode.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).thenReturn(false);
        when(handleNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(handleNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(false);

        EmbargoUtils.removeEmbargoForHandle(session, handleNode);

        verify(versionManager).checkout("/content/test");
    }

    @Test
    void removeEmbargoForHandle_withGroupProperty_removesProperty() throws Exception {
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getVersionManager()).thenReturn(versionManager);
        when(handleNode.isCheckedOut()).thenReturn(true);
        when(handleNode.hasProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME)).thenReturn(true);
        when(handleNode.getProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME)).thenReturn(property);
        when(handleNode.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).thenReturn(false);
        when(handleNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(handleNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(false);

        EmbargoUtils.removeEmbargoForHandle(session, handleNode);

        verify(property).remove();
    }

    @Test
    void removeEmbargoForHandle_withRequestNode_removesNode() throws Exception {
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getVersionManager()).thenReturn(versionManager);
        when(handleNode.isCheckedOut()).thenReturn(true);
        when(handleNode.hasProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME)).thenReturn(false);
        when(handleNode.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).thenReturn(true);
        when(handleNode.getNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).thenReturn(requestNode);
        when(handleNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(handleNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(false);

        EmbargoUtils.removeEmbargoForHandle(session, handleNode);

        verify(requestNode).remove();
    }

    // -------------------------------------------------------------------------
    // getCurrentUserEmbargoEnabledGroups
    // -------------------------------------------------------------------------

    @Test
    void getCurrentUserEmbargoEnabledGroups_userInEmbargoGroup_returnsMatchingGroups() throws Exception {
        // getAllUserGroups returns ["editors", "embargo-editors"]
        // getAllEmbargoEnabledGroups returns ["embargo-editors"]
        // intersection => ["embargo-editors"]
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);

        // User groups query
        String userGroupsSql = EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", "carol");
        when(queryManager.createQuery(eq(userGroupsSql), eq(Query.SQL))).thenReturn(query);
        NodeIterator userGroupIterator = org.mockito.Mockito.mock(NodeIterator.class);
        Node editorsNode = org.mockito.Mockito.mock(Node.class);
        Node embargoEditorsNode = org.mockito.Mockito.mock(Node.class);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getNodes()).thenReturn(userGroupIterator);
        when(userGroupIterator.hasNext()).thenReturn(true, true, false);
        when(userGroupIterator.nextNode()).thenReturn(editorsNode, embargoEditorsNode);
        when(editorsNode.getName()).thenReturn("editors");
        when(embargoEditorsNode.getName()).thenReturn("embargo-editors");

        // Embargo domain query
        when(session.getRootNode()).thenReturn(rootNode);
        when(rootNode.getNode(EmbargoConstants.EMBARGO_DOMAIN_PATH)).thenReturn(handleNode);
        NodeIterator domainIterator = org.mockito.Mockito.mock(NodeIterator.class);
        when(handleNode.getNodes()).thenReturn(domainIterator);
        Node authRoleNode = org.mockito.Mockito.mock(Node.class);
        when(domainIterator.hasNext()).thenReturn(true, false);
        when(domainIterator.nextNode()).thenReturn(authRoleNode);
        when(authRoleNode.isNodeType(EmbargoConstants.HIPPOSYS_AUTHROLE)).thenReturn(true);
        when(authRoleNode.hasProperty(HippoNodeType.HIPPO_GROUPS)).thenReturn(true);
        Property groupsProp = org.mockito.Mockito.mock(Property.class);
        Value embargoValue = org.mockito.Mockito.mock(Value.class);
        when(authRoleNode.getProperty(HippoNodeType.HIPPO_GROUPS)).thenReturn(groupsProp);
        when(groupsProp.getValues()).thenReturn(new Value[]{embargoValue});
        when(embargoValue.getString()).thenReturn("embargo-editors");

        String[] result = EmbargoUtils.getCurrentUserEmbargoEnabledGroups(session, "carol");

        assertArrayEquals(new String[]{"embargo-editors"}, result);
    }

    @Test
    void getCurrentUserEmbargoEnabledGroups_userNotInAnyEmbargoGroup_returnsEmpty() throws Exception {
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);

        String userGroupsSql = EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", "dave");
        when(queryManager.createQuery(eq(userGroupsSql), eq(Query.SQL))).thenReturn(query);
        NodeIterator userGroupIterator = org.mockito.Mockito.mock(NodeIterator.class);
        Node editorsNode = org.mockito.Mockito.mock(Node.class);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getNodes()).thenReturn(userGroupIterator);
        when(userGroupIterator.hasNext()).thenReturn(true, false);
        when(userGroupIterator.nextNode()).thenReturn(editorsNode);
        when(editorsNode.getName()).thenReturn("editors");

        // embargo domain has different groups
        when(session.getRootNode()).thenReturn(rootNode);
        when(rootNode.getNode(EmbargoConstants.EMBARGO_DOMAIN_PATH)).thenReturn(handleNode);
        NodeIterator domainIterator = org.mockito.Mockito.mock(NodeIterator.class);
        when(handleNode.getNodes()).thenReturn(domainIterator);
        Node authRoleNode = org.mockito.Mockito.mock(Node.class);
        when(domainIterator.hasNext()).thenReturn(true, false);
        when(domainIterator.nextNode()).thenReturn(authRoleNode);
        when(authRoleNode.isNodeType(EmbargoConstants.HIPPOSYS_AUTHROLE)).thenReturn(true);
        when(authRoleNode.hasProperty(HippoNodeType.HIPPO_GROUPS)).thenReturn(true);
        Property groupsProp = org.mockito.Mockito.mock(Property.class);
        Value embargoValue = org.mockito.Mockito.mock(Value.class);
        when(authRoleNode.getProperty(HippoNodeType.HIPPO_GROUPS)).thenReturn(groupsProp);
        when(groupsProp.getValues()).thenReturn(new Value[]{embargoValue});
        when(embargoValue.getString()).thenReturn("embargo-authors");

        String[] result = EmbargoUtils.getCurrentUserEmbargoEnabledGroups(session, "dave");

        assertEquals(0, result.length);
    }
}
