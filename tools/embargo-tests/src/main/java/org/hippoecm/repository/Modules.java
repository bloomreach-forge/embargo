/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.jar.Manifest;

import org.hippoecm.repository.ext.WorkflowManagerModule;
import org.hippoecm.repository.quartz.EmbargoWorkflowModule;
import org.hippoecm.repository.quartz.SchedulerWorkflowModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hacking...
 *
 * Unable to get the Embargo Workflow Module picked up. So I fork the Modules class just to add it.
 *
 * @param <T>
 */
public final class Modules<T extends Object> implements Iterable<T> {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id: Modules.java 34057 2012-04-12 10:40:27Z bvanhalderen $";

    static final Logger log = LoggerFactory.getLogger(Modules.class);

    private static Modules allModules;

    private Set modules;

    public Modules() {
        modules = new TreeSet<T>(new Comparator() {
            public int compare(Object o1, Object o2) {
                return o1.getClass().getName().compareTo(o2.getClass().getName());
            }
        });
    }

    public Modules(Collection<T> modules) {
        this();
        this.modules.addAll(modules);
    }

    public Modules(Modules modules, Class<T> clazz) {
        this();
        for (Object instance : modules) {
            if (clazz.isInstance(instance)) {
                this.modules.add((T) instance);
            }
        }
    }

    public Modules(ClassLoader loader) {
        this(loader, null);
    }

    public Modules(Modules<T> original) {
        this();
        for (T instance : original) {
            Class<? extends T> moduleClass = (Class<? extends T>) instance.getClass();
            try {
                T moduleInstance = moduleClass.newInstance();
                modules.add(moduleInstance);
            } catch (InstantiationException ex) {
                log.warn("Failure instantiating module class {}: {}", moduleClass.getName(), ex);
            } catch (IllegalAccessException ex) {
                log.warn("Failed instantiating module class {}: {}", moduleClass.getName(), ex);
            }
        }
    }

    public Modules(ClassLoader loader, Class<T> clazz) {
        this();
        try {
            for (Enumeration<URL> e = loader.getResources("META-INF/MANIFEST.MF"); e.hasMoreElements(); ) {
                URL url = e.nextElement();
                try {
                    Manifest manifest = new Manifest(url.openStream());
                    String modulesString = manifest.getMainAttributes().getValue("Hippo-Modules");
                    if (modulesString != null) {
                        log.info("Hippo-Modules specified by manifest {} : {}", url.toString(), modulesString);
                    } else {
                        log.debug("Hippo-Modules specified by manifest  {} : none", url.toString());
                    }
                    if (modulesString != null) {
                        for (StringTokenizer tok = new StringTokenizer(modulesString); tok.hasMoreTokens(); ) {
                            String moduleClassName = tok.nextToken().trim();
                            if (!moduleClassName.equals("")) {
                                try {
                                    Class moduleClass = Class.forName(moduleClassName, true, loader);
                                    if (clazz == null || clazz.isAssignableFrom(moduleClass)) {
                                        Object moduleInstance = moduleClass.newInstance();
                                        modules.add(moduleInstance);
                                        /**
                                         * Unable to get the Embargo Workflow Module picked up. So I fork the Modules class just to add it.
                                         */
                                        if(moduleInstance instanceof SchedulerWorkflowModule){
                                            modules.add(new EmbargoWorkflowModule());
                                        }
                                    }
                                } catch (NoClassDefFoundError ex) {
                                    log.warn("Cannot instantiate module class {}: {}", moduleClassName, ex);
                                } catch (ClassNotFoundException ex) {
                                    log.warn("Cannot instantiate module class {}: {}", moduleClassName, ex);
                                } catch (InstantiationException ex) {
                                    log.warn("Failure instantiating module class {}: {}", moduleClassName, ex);
                                } catch (IllegalAccessException ex) {
                                    log.warn("Failed instantiating module class {}: {}", moduleClassName, ex);
                                }
                            }
                        }
                    }
                } catch (NullPointerException ex) {
                    log.error("Cannot access manifest " + url.toString(), ex);
                } catch (IOException ex) {
                    log.error("Cannot access manifest " + url.toString(), ex);
                } catch (Throwable ex) {
                    log.info("Failure accessing manifest " + url.toString(), ex);
                }
            }
        } catch (IOException ex) {
            log.error("Cannot access any manifest", ex);
        }
        if (clazz != null && clazz.isAssignableFrom(WorkflowManagerModule.class)) {
            modules.add(new EmbargoWorkflowModule());
        }
    }

    public Iterator<T> iterator() {
        return modules.iterator();
    }

    public static Modules getModules() {
        return allModules;
    }

    static void setModules(Modules modules) {
        allModules = modules;
        if (log.isInfoEnabled()) {
            log.info("Default list of modules set to:");
            for (Object module : allModules) {
                log.info("  " + module.getClass().getName());
            }
        }
    }
}
