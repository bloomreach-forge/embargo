package org.onehippo.forge.embargo.hst.addons;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository;
import org.hippoecm.hst.core.jcr.pool.MultipleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class SessionRefreshEventListener implements EventListener {

    private static Logger log = LoggerFactory.getLogger(SessionRefreshEventListener.class);

    private MultipleRepository repository;
    private SimpleCredentials credentials;

    @Override
    public void onEvent(final EventIterator events) {

        if(repository.containsRepositoryByCredentials(credentials)){
            final Repository repositoryByCredentials = repository.getRepositoryByCredentials(credentials);

            if (repositoryByCredentials instanceof BasicPoolingRepository) {
                final BasicPoolingRepository reinitRepo = (BasicPoolingRepository) repositoryByCredentials;
                try {
                    reinitRepo.initialize();
                } catch (RepositoryException e) {
                    log.error("", e);
                }
            }
        }
    }

    public SimpleCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(final SimpleCredentials credentials) {
        this.credentials = credentials;
    }

    public MultipleRepository getRepository() {
        return repository;
    }

    public void setRepository(final MultipleRepository repository) {
        this.repository = repository;
    }
}
