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

/**
 * @version $Id$
 */
public class EmbargoConstants {

    public static final String EMBARGO_MIXIN_NAME = "embargo:embargo";
    public static final String EMBARGO_DOCUMENT_MIXIN_NAME = "embargo:document";
    public static final String EMBARGO_GROUP_PROPERTY_NAME = "embargo:groups";
    public static final String EMBARGO_SCHEDULE_REQUEST_NODE_NAME = "embargo:request";
    public static final String SELECT_GROUPS_QUERY = "SELECT * FROM hipposys:group WHERE jcr:primaryType='hipposys:group' AND hipposys:members='{}'";
    public static final String EMBARGO_GROUPS_MAPPING_NODE_PATH = "hippo:configuration/hippo:domains/embargo/hipposys:authrole";

    public static final String HIPPOSCHED_TRIGGERS_DEFAULT = "hipposched:triggers/default";
    public static final String HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME = "hipposched:fireTime";



}
