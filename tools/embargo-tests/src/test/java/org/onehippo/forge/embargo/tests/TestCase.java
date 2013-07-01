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
import java.net.URL;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.LocalHippoRepository;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

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
        String[] files = new String[]{".lock", "repository", "version", "workspaces"};
        for (final String file1 : files) {
            File file = new File(file1);
            delete(file);
        }
    }

    static protected void fixture() {
        final URL resource = TestCase.class.getResource("/dump.zip"); //a clean dump...
        if (resource != null) {
            File fixtureDump = new File(resource.getFile());
            if (fixtureDump.exists()) {
                FileInputStream fixtureStream = null;
                try {
                    fixtureStream = new FileInputStream(fixtureDump);
                    JarInputStream istream = new JarInputStream(fixtureStream);
                    ZipEntry ze;
                    do {
                        ze = istream.getNextEntry();
                        if (ze != null) {
                            if (ze.isDirectory()) {
                                String name = ze.getName();
                                File file = new File(name);
                                file.mkdir();
                            } else {
                                FileOutputStream ostream = new FileOutputStream(ze.getName());
                                byte[] buffer = new byte[1024];
                                int len;
                                do {
                                    len = istream.read(buffer);
                                    if (len >= 0) {
                                        ostream.write(buffer, 0, len);
                                    }
                                } while (len >= 0);
                                ostream.close();
                            }
                        }
                    } while (ze != null);
                    istream.close();
                    fixtureStream.close();
                } catch (IOException ex) {
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    try {
                        if (fixtureStream != null) {
                            fixtureStream.close();
                        }
                    } catch (IOException e) {
                        clear();
                    }
                }
            }
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

}
