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
