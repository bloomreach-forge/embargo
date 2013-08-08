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
package org.onehippo.forge.embargo.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.LocalHippoRepository;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ripped this one from Repository project... Utility class for writing tests against repository.
 * <p/>
 */
@Ignore
public class TestCase {

    /**
     * System property indicating whether to use the same repository server across all test invocations. If this
     * property is false or not present a new repository will be created for every test. Sometimes this is unavoidable
     * because you need a clean repository. If you don't then your test performance will benefit greatly by setting this
     * property.
     */
    private static final String KEEPSERVER_PROP = "org.onehippo.repository.test.keepserver";

    /**
     * System property indicating whether to perform a consistency check during test case teardown. This property only
     * has effect when the KEEPSERVER_PROP system property is set to false.
     */
    private static final String CONSISTENCYCHECK_PROP = "org.onehippo.repository.test.check";


    protected static final String SYSTEMUSER_ID = "admin";
    protected static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private static final Logger log = LoggerFactory.getLogger(TestCase.class);

    protected static HippoRepository external = null;
    protected static HippoRepository background = null;
    protected HippoRepository server = null;
    protected Session session = null;

    public TestCase() {
    }

    static private void delete(File path) {
        if (path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (final File file : files) {
                    delete(file);
                }
            }
            path.delete();
        }
    }

    public static void clear() {
        if (background != null) {
            background.close();
            background = null;
        }
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true;deregister=true");
        } catch (SQLException e) {
            // a shutdown command always raises a SQLException
        }
        final String[] files = new String[]{".lock", "repository", "version", "workspaces"};
        final String repositoryPath = getRepositoryPath();
        for (final String file1 : files) {
            File file = new File(repositoryPath, file1);
            delete(file);
        }
    }

    static protected void fixture() {
        InputStream fixtureStream = TestCase.class.getResourceAsStream("/dump.zip");
        if (fixtureStream != null) {
            final String repositoryPath = getRepositoryPath();
            log.info("Unpacking repository fixture at {}", repositoryPath);

            JarInputStream jarStream = null;
            try {
                jarStream = new JarInputStream(fixtureStream);
                ZipEntry ze;
                do {
                    ze = jarStream.getNextEntry();
                    if (ze != null) {
                        if (ze.isDirectory()) {
                            String name = ze.getName();
                            File file = new File(repositoryPath, name);
                            file.mkdir();
                        } else {
                            FileOutputStream ostream = new FileOutputStream(ze.getName());
                            byte[] buffer = new byte[1024];
                            int len;
                            do {
                                len = jarStream.read(buffer);
                                if (len >= 0) {
                                    ostream.write(buffer, 0, len);
                                }
                            } while (len >= 0);
                            ostream.close();
                        }
                    }
                } while (ze != null);
            } catch (IOException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                try {
                    if (fixtureStream != null) {
                        fixtureStream.close();
                    }
                } catch (IOException e) {
                    clear();
                }
            } finally {
                IOUtils.closeQuietly(jarStream);
                IOUtils.closeQuietly(fixtureStream);
            }
        } else {
            throw new IllegalStateException("Cannot setup test case fixture, dump.zip is missing from the jar file");
        }
    }


    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    protected static void setUpClass(boolean clearRepository) throws Exception {
        if (clearRepository) {
            clear();
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        tearDownClass(false);
    }

    public static void tearDownClass(boolean clearRepository) throws Exception {
        if (clearRepository) {
            clear();
        }
    }

    @Before
    public void setUp() throws Exception {
        setUp(false);
    }

    protected void setUp(boolean clearRepository) throws Exception {
        if (external != null) {
            if (clearRepository) {
                throw new IllegalArgumentException("Cannot clear the repository in a remote test");
            }
            server = external;
        } else {
            if (clearRepository) {
                clear();
                if (Boolean.getBoolean("org.onehippo.repository.test.usefixture") ||
                        Boolean.getBoolean("org.onehippo.repository.test.forcefixture") ||
                        Boolean.getBoolean("org.onehippo.repository.test.installfixture")) {
                    fixture();
                }
            } else {
                if (Boolean.getBoolean("org.onehippo.repository.test.forcefixture")) {
                    clear();
                    fixture();
                } else if (Boolean.getBoolean("org.onehippo.repository.test.installfixture") && !(new File("repository").exists())) {
                    fixture();
                }
            }
            if (Boolean.getBoolean(KEEPSERVER_PROP)) {
                if (background != null) {
                    server = background;
                } else {
                    server = background = HippoRepositoryFactory.getHippoRepository();
                }
            } else {
                server = HippoRepositoryFactory.getHippoRepository();
            }
        }
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
            session.refresh(false);
        }
    }

    @After
    public void tearDown() throws Exception {
        tearDown(false);
    }

    public void tearDown(boolean clearRepository) throws Exception {
        if (session != null) {
            session.refresh(false);
            while (session.getRootNode().hasNode("test")) {
                session.getRootNode().getNode("test").remove();
                session.save();
            }
            session.logout();
            session = null;
        }
        if (external == null && server != null) {
            if (!Boolean.getBoolean(KEEPSERVER_PROP)) {
                server.close();
                if (server instanceof LocalHippoRepository) {
                    if (Boolean.getBoolean(CONSISTENCYCHECK_PROP) && !((LocalHippoRepository) server).check(false)) {
                        server = null;
                        clear();
                        throw new Exception("Repository inconsistent");
                    }
                }
            }
            server = null;
        }
        if (clearRepository) {
            clear();
        }
    }

    private static String getRepositoryPath() {
        return System.getProperty("repo.path", new File(System.getProperty("user.dir")).getAbsolutePath());
    }

}
