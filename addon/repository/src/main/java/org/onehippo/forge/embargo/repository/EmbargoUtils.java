/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class EmbargoUtils {

    private static Logger log = LoggerFactory.getLogger(EmbargoUtils.class);

    public static String[] getCurrentUserEmbargoEnabledGroups(Session session, String userIdentity) throws RepositoryException {

        Query selectGroupsQuery = session.getWorkspace().getQueryManager().createQuery(
                EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", userIdentity),
                Query.SQL);
        NodeIterator groupNodes = selectGroupsQuery.execute().getNodes();
        if (!groupNodes.hasNext()) {
            return new String[]{};
        }

        List<String> groupNames = new ArrayList<String>();
        List<String> embargoEnabledGroupNames = getAllEmbargoEnabledGroups(session);

        while (groupNodes.hasNext()) {
            String groupName = groupNodes.nextNode().getName();
            if (embargoEnabledGroupNames.contains(groupName)) {
                groupNames.add(groupName);
            }
        }

        return groupNames.toArray(new String[groupNames.size()]);
    }

    public static List<String> getAllEmbargoEnabledGroups(Session session) throws RepositoryException {
        Value[] embargoGroups = session.getRootNode()
                .getNode(EmbargoConstants.EMBARGO_GROUPS_MAPPING_NODE_PATH).getProperty(HippoNodeType.HIPPO_GROUPS).getValues();
        List<String> embargoGroupNames = new ArrayList<String>();
        for (final Value embargoGroup : embargoGroups) {
            embargoGroupNames.add(embargoGroup.getString());
        }
        return embargoGroupNames;

    }

}
