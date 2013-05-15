package org.onehippo.forge.embargo.repository;

/**
 * @version $Id$
 */
public class EmbargoConstants {

    public static final String EMBARGO_MIXIN_NAME = "embargo:embargo";
    public static final String EMBARGO_GROUP_PROPERTY_NAME = "embargo:groups";
    public static final String EMBARGO_SCHEDULE_REQUEST_NODE_NAME = "embargo:request";
    public static final String SELECT_GROUPS_QUERY = "SELECT * FROM hipposys:group WHERE jcr:primaryType='hipposys:group' AND hipposys:members='{}'";
    public static final String EMBARGO_GROUPS_MAPPING_NODE_PATH = "hippo:configuration/hippo:domains/embargo/hipposys:authrole";

    public static final String HIPPOSCHED_TRIGGERS_DEFAULT = "hipposched:triggers/default";
    public static final String HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME = "hipposched:fireTime";



}
