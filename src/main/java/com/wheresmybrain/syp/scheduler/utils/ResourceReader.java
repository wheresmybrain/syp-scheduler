package com.wheresmybrain.syp.scheduler.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads a file as an <code>InputStream</code> using one of three methods:
 * <ol>
 * <li>specified as an absolute path</li>
 * <li>specified relative to the classpath</li>
 * <li>specified relative to the rootdir (if it's set)</li>
 * </ol>
 * <p/>
 * The "root" directory must be set as a reference on this class  in order to
 * support reading files relative to that directory. Here is an example of how
 * to instantiate ResourceReader in web applications so it can be used to read
 * files relative to docroot:
 * <pre>
 *    String docroot = this.getServletContext().getRealPath("/");
 *    ResourceReader resourceReader = new ResourceReader(docroot);
 *    ...[snip]...
 *    // read config file
 *    InputStream is = resourceReader.readResource("/WEB-INF/xml/myConfig.xml");
 * </pre>
 * <p/>
 * By default the <code>ClassLoader</code> that loads this class (typically the
 * default "application" ClassLoader) is used to locate the resource file relative
 * to the classpath. However, a custom ClassLoader can be specified using the
 * {@link #setClassLoader(ClassLoader)} method, which can be chained to the
 * constructor (see example in method javadoc).
 *
 * @author <a href="mailto:chris.mcfarland@gmail.com">chris.mcfarland</a>
 */
public class ResourceReader {

    Logger log = LoggerFactory.getLogger(ResourceReader.class);

    /**
     * Used for specifying relative paths to the rootdir.
     */
    private File rootdir;

    private ClassLoader classLoader = this.getClass().getClassLoader();

    /**
     * Use this constructor when rootdir is not needed. In this case
     * only methods 1 and 2 are used.
     */
    public ResourceReader() {
    }

    /**
     * Use this constructor to specify the root directory so
     * files can be read w/ the 3rd method - relative to the rootdir.
     * @throws IOException if rootdir is not a valid directory.
     */
    public ResourceReader(File rootdir) throws IOException {
        if (rootdir.exists() && rootdir.isDirectory()) {
            this.rootdir = rootdir;
        } else {
            throw new IOException("rootdir is not a valid directory: "+rootdir.getAbsolutePath());
        }
    }

    /**
     * Use this constructor to specify the root directory so
     * files can be read w/ the 3rd method - relative to the rootdir.
     * @throws IOException if rootpath is not a valid directory.
     */
    public ResourceReader(String rootpath) throws IOException {
        this(new File(rootpath));
    }

    /**
     * Sets a custom ClassLoader for locating resources relative to the classpath.
     * This "builder" method can be chained to the constructor like the following example:
     * <pre><code>
     *     ResourceReader reader = new ResourceReader().setClassLoader(myClassLoader);
     * </code></pre>
     *
     * @return reference to this object so the method can be chained to the constructor.
     */
    public ResourceReader setClassLoader(ClassLoader classLoader) {
        if (classLoader != null) this.classLoader = classLoader;
        return this;
    }

    /**
     * Returns file as InputStream using one of three methods:
     * <li>Resource specified as an absolute path.</li>
     * <li>Resource specified relative to the rootdir (most desirable).</li>
     * <li>Resource specified relative to a classpath element (also desirable).</li>
     *
     * @param filepath String path to file either as an absolute path, path
     *   relative to rootdir or path relative to classpath. Specifying leading
     *   slash ('/') is optional for relative paths.
     * @returns InputStream of the file or throws an exception.
     * @throws IOException if the file cannot be read.
     * @throws FileNotFoundException if the file cannot be located using any path method.
     */
    public InputStream readResource(String filepath) throws IOException {

        log.debug("ResourceReader - reading file: "+filepath);

        InputStream resourceStream;

        // Method 1 - try reading file as an absolute path
        File file = new File(filepath);
        if (file.exists()) {
            if (file.canRead()) {
                resourceStream = new FileInputStream(file);
                log.debug("Reading file as absolute path: "+filepath);
            } else {
                throw new IOException("File does not have READ permission: "+filepath);
            }
        } else {
            filepath = filepath.replace(File.separatorChar, '/');
            if (rootdir != null) {
                // Method 2 - try reading file as a path relative to rootdir
                file = new File(rootdir, filepath);
                if (file.exists()) {
                    if (file.canRead()) {
                        resourceStream = new FileInputStream(file);
                        log.debug("Reading file as relative path: "+filepath);
                    } else {
                        throw new IOException("File does not have READ permission: "+file.getAbsolutePath());
                    }
                } else {
                    // Method 3 - try reading file as path relative to a classpath element
                    if (filepath.startsWith("/")) filepath = filepath.substring(1);
                    resourceStream = this.classLoader.getResourceAsStream(filepath);
                    if (resourceStream != null) {
                        log.debug("Reading file relative to classpath: "+filepath);
                    } else {
                        throw new FileNotFoundException("could not read file using any method: "+filepath);
                    }
                }
            } else {
                // Method 3 - try reading file as path relative to a classpath element
                if (filepath.startsWith("/")) filepath = filepath.substring(1);
                resourceStream = this.classLoader.getResourceAsStream(filepath);
                if (resourceStream != null) {
                    log.debug("Reading file relative to classpath: "+filepath);
                } else {
                    throw new FileNotFoundException("could not read file using any method: "+filepath);
                }
            }
        }

        return resourceStream;
    }

    /**
     * Returns the root directory, or null if root directory was not set.
     */
    public File getRootdir() {
        return this.rootdir;
    }
}
