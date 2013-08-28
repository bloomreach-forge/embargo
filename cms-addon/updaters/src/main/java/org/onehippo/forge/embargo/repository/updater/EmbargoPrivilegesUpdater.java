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
package org.onehippo.forge.embargo.repository.updater;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

/**
 * @author Jeroen Reijn
 */
public class EmbargoPrivilegesUpdater implements UpdaterModule {

    @Override
    public void register(final UpdaterContext context) {
        context.registerName("embargo-v1-updater");
        context.registerStartTag("v1-embargo");
        context.registerEndTag("v1_01_6-embargo");

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:workflows/embargo/embargo-workflow") {

            @Override
            protected void leaving(final Node node, final int level) throws RepositoryException {
                if(!node.hasProperty("hipposys:privileges")) {
                    node.setProperty("hipposys:privileges", new String[]{"hippo:author"});
                }
            }
        });
    }

}
