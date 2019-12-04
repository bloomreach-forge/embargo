/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.embargo.frontend.plugins.cms.browse.list.comparators;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.comparators.NodeComparator;
import org.onehippo.forge.embargo.frontend.plugins.cms.browse.list.resolvers.EmbargoDocumentView;

/**
 * Comparator between EmbargoDocumentView resolvers.
 */
public abstract class EmbargoDocumentViewComparator extends NodeComparator {

    @Override
    public int compare(JcrNodeModel o1, JcrNodeModel o2) {
        return compare(new EmbargoDocumentView(o1), new EmbargoDocumentView(o2));
    }

    protected abstract int compare(EmbargoDocumentView embargoDocumentView1, EmbargoDocumentView embargoDocumentView2);

}
