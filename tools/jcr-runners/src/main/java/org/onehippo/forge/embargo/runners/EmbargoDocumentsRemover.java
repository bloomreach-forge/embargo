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
package org.onehippo.forge.embargo.runners;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.forge.jcrrunner.JcrHelper;
import org.onehippo.forge.jcrrunner.plugins.AbstractRunnerPlugin;

public class EmbargoDocumentsRemover extends AbstractRunnerPlugin {

    @Override
    public void visit(final Node node) {
        String path = "";
        try {
            path = node.getPath();
            getLogger().info("Removing node: {}", path);
            node.remove();
            JcrHelper.save();
        } catch (RepositoryException e) {
            getLogger().error("Could not remove document " + path, e);
        }
    }

    @Override
    public void visitEnd(final Node node) {
        //TODO Remove when https://issues.onehippo.com/browse/HIPPLUG-656 is resolved
    }
}
