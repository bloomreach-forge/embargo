package org.onehippo.forge.embargo.runners;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.forge.jcrrunner.plugins.AbstractRunnerPlugin;

public class EmbargoDocumentsRemover extends AbstractRunnerPlugin {

    @Override
    public void visit(final Node node) {
        String path = "";
        try {
            path = node.getPath();
            getLogger().info("Removing node: {}", path);
            node.remove();
            node.getSession().save();
        } catch (RepositoryException e) {
            getLogger().error("Could not remove document " + path, e);
        }
    }
}
