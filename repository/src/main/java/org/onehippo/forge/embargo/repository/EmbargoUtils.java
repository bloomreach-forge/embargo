/*
 * Copyright 2024 Bloomreach B.V. (http://www.bloomreach.com)
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.forge.embargo.repository.EmbargoConstants.EMBARGO_DOMAIN_PATH;
import static org.onehippo.forge.embargo.repository.EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME;
import static org.onehippo.forge.embargo.repository.EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT;
import static org.onehippo.forge.embargo.repository.EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME;

public final class EmbargoUtils {

    public static final String[] EMPTY_ARRAY = {};

    private EmbargoUtils() {
    }

    private static Logger log = LoggerFactory.getLogger(EmbargoUtils.class);

    public static String[] getCurrentUserEmbargoEnabledGroups(Session session, String userIdentity) {
        String[] allUserGroups = getAllUserGroups(session, userIdentity);
        List<String> embargoEnabledUserGroups = new ArrayList<String>();
        List<String> allEmbargoEnabledGroups = getAllEmbargoEnabledGroups(session);

        for (final String allUserGroup : allUserGroups) {
            if (allEmbargoEnabledGroups.contains(allUserGroup)) {
                embargoEnabledUserGroups.add(allUserGroup);
            }
        }
        return embargoEnabledUserGroups.toArray(new String[embargoEnabledUserGroups.size()]);
    }

    public static boolean isAdminUser(Session session, String userIdentity) {
        String[] allUserGroups = getAllUserGroups(session, userIdentity);
        for (final String allUserGroup : allUserGroups) {
            if (EmbargoConstants.ADMIN_GROUP_NAME.equals(allUserGroup)) {
                return true;
            }
        }
        return false;
    }


    public static String[] getAllUserGroups(Session session, String userIdentity) {
        try {
            @SuppressWarnings("deprecation")
            Query selectGroupsQuery = session
                    .getWorkspace()
                    .getQueryManager()
                    .createQuery(EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", userIdentity), Query.SQL);
            NodeIterator groupNodes = selectGroupsQuery.execute().getNodes();
            List<String> groupNames = new ArrayList<String>();
            while (groupNodes.hasNext()) {
                groupNames.add(groupNodes.nextNode().getName());
            }
            return groupNames.toArray(new String[groupNames.size()]);

        } catch (RepositoryException e) {
            log.error("Could not retrieve user groups", e);
            return EMPTY_ARRAY;
        }
    }

    public static List<String> getAllEmbargoEnabledGroups(Session session) {
        Node embargoGroupsMappingNode;
        try {
            embargoGroupsMappingNode = session.getRootNode().getNode(EMBARGO_DOMAIN_PATH);
        } catch (RepositoryException e) {
            log.error("Embargo domain does not exist at {}", EMBARGO_DOMAIN_PATH);
            return new ArrayList<>();
        }

        try {
            final Set<String> embargoGroupNames = new HashSet<>();
            for (final Node authRole : getEmbargoAuthRoleNodes(embargoGroupsMappingNode)) {
                if (authRole.hasProperty(HippoNodeType.HIPPO_GROUPS)) {
                    Value[] embargoGroups = authRole.getProperty(HippoNodeType.HIPPO_GROUPS).getValues();
                    for (final Value embargoGroup : embargoGroups) {
                        embargoGroupNames.add(embargoGroup.getString());
                    }
                }
            }
            return new ArrayList<>(embargoGroupNames);
        } catch (RepositoryException e) {
            log.error("Could not retrieve embargo enabled groups", e);
            return new ArrayList<>();
        }
    }

    private static List<Node> getEmbargoAuthRoleNodes(final Node embargoGroupsMappingNode) throws RepositoryException {
        final List<Node> authRoleNodes = new ArrayList<>();
        final NodeIterator nodes = embargoGroupsMappingNode.getNodes();
        while (nodes.hasNext()) {
            final Node node = nodes.nextNode();
            if (node.isNodeType(EmbargoConstants.HIPPOSYS_AUTHROLE)) {
                authRoleNodes.add(node);
            }
        }
        return authRoleNodes;
    }

    public static Calendar getEmbargoExpirationDate(Node hippoHandleNode) throws RepositoryException {
        if (hippoHandleNode.isNodeType(HippoNodeType.NT_HANDLE)
                && hippoHandleNode.hasNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME)) {
            Node requestNode = hippoHandleNode.getNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME);
            if (requestNode.hasNode(HIPPOSCHED_TRIGGERS_DEFAULT)) {
                Node defaultTriggerNode = requestNode.getNode(HIPPOSCHED_TRIGGERS_DEFAULT);
                if (defaultTriggerNode.hasProperty(HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME)) {
                    return defaultTriggerNode.getProperty(HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME).getDate();
                }
            }
        }
        return null;
    }

    /**
     * We only want to show the menu for the 'preview' variant of a document
     *
     * @param documentNode
     * @return
     */
    public static boolean isVisibleInPreview(Node documentNode) {
        try {
            if (documentNode.hasProperty(HippoNodeType.HIPPO_AVAILABILITY)) {
                for (Value availability : documentNode.getProperty(HippoNodeType.HIPPO_AVAILABILITY).getValues()) {
                    if ("preview".equals(availability.getString())) {
                        return true;
                    }
                }
            } else {
                log.warn("Document '{}' does not contain the property '{}'. No 'View' menu items will be shown.",
                        documentNode.getPath(), HippoNodeType.HIPPO_AVAILABILITY);
            }
        } catch (RepositoryException e) {
            log.error("Error getting " + HippoNodeType.HIPPO_AVAILABILITY + " property from document", e);
        }
        return false;
    }

    public static Node[] getDocumentVariants(Node documentHandleNode) throws RepositoryException {
        NodeIterator nodeIterator = documentHandleNode.getNodes();
        List<Node> documentNodes = new ArrayList<Node>();
        while (nodeIterator.hasNext()) {
            final Node documentNode = nodeIterator.nextNode();
            if (documentNode.getName().equals(documentHandleNode.getName())) {
                documentNodes.add(documentNode);
            }
        }
        return documentNodes.toArray(new Node[documentNodes.size()]);
    }

    public static Node extractHandle(final Node node) throws RepositoryException {
        if (node == null) {
            return null;
        }
        if (node.isNodeType(EmbargoConstants.HIPPO_HANDLE)) {
            return node;
        } else {
            final Node parent = node.getParent();
            if (parent == null) {
                return null;
            }
            if (parent.isNodeType(EmbargoConstants.HIPPO_HANDLE)) {
                return parent;
            }
        }

        return null;

    }

    public static void removeEmbargoForHandle(final Session session, final Node handle) throws RepositoryException {
        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        if (!handle.isCheckedOut()) {
            versionManager.checkout(handle.getPath());
        }
        //remove embargo:groups
        if (handle.hasProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME)) {
            handle.getProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME).remove();
        }
        //remove any embargo:request
        if (handle.hasNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME)) {
            handle.getNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME).remove();
        }
        //remove embargo mixin from handle
        removeMixin(handle, EmbargoConstants.EMBARGO_MIXIN_NAME);

        //remove embargo mixin from document(s)
        for (Node documentNode : getDocumentVariants(handle)) {
            if (!documentNode.isCheckedOut()) {
                versionManager.checkout(documentNode.getPath());
            }
            removeMixin(documentNode, EmbargoConstants.EMBARGO_DOCUMENT_MIXIN_NAME);
        }
    }

    public static void removeMixin(final Node handle, final String mixin) throws RepositoryException {
        final NodeType[] mixinNodeTypes = handle.getMixinNodeTypes();
        for (NodeType mixinNodeType : mixinNodeTypes) {
            if (mixinNodeType.getName().equals(mixin)) {
                handle.removeMixin(mixin);
                return;
            }
        }
    }
}
