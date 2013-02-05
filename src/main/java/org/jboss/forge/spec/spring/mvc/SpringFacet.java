package org.jboss.forge.spec.spring.mvc;

import java.util.List;

import org.jboss.forge.project.Facet;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.project.Project;

/**
 * If installed, this {@link Project} supports features from Spring MVC.
 * 
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public interface SpringFacet extends Facet
{
    /**
     * Change the location of the application context file (the default location is
     * src/main/resources/META-INF/spring/applicationContext.xml
     * 
     * @returns true, if the specified file exists and the default location is changed, otherwise false.
     */

    boolean setContextFileLocation(String location);

    /**
     * @returns the String value of the location of the application context XML file.
     */

    String getContextFileLocation();

    /**
     * Get this application's applicationContext.xml file.
     */

    FileResource<?> getContextFile();

    /**
     * Get the servlet XML context file for the specified targetDir of the application.
     */

    FileResource<?> getMVCContextFile(String targetDir);

    /**
     * Get this application's currently configured servlet mappings from the web.xml
     */

    List<String> getSpringServletMappings();

    /**
     * Set this application's servlet mapping
     */

    void setSpringMapping(String mapping);

    /**
     * For a given {@link Resource}, if the resource is a web-resource, return all known context-relative URLs with which
     * that resource may be accessed.
     * <p>
     * E.g: If the servlet were mapped to *, given the resource "$PROJECT_HOME/src/main/webapp/example.jsp",
     * this method would return "/example"
     */

    /**
     * First, check whether a servlet exists with the name specified in the method parameter.
     * If one does not exist, create a Spring DispatcherServlet with that servlet name.  The new servlet will be mapped
     * to /{servletName}, assuming that servlet name does not start with a '/' character.
     * <p>
     * E.g: If the servletName specified was 'admin', a new DispatcherServlet, mapped to '/admin' would be created.  If
     * it were '/admin', a new servlet named 'admin' would be created, mapping to '/admin'.
     */

    void addServlet(String servletName, String contextFile);

    /**
     * Add a servlet mapped to '/', with the name 'root', assuming one does not exist already.
     */

    void addRootServlet(String contextFile);

    /**
     * Given a servlet name, return whether or not the application's web.xml file contains a servlet definition with that
     * name.
     */

    boolean hasServlet(String servletName);

    List<String> getWebPaths(Resource<?> r);

    /**
     * For a given view-ID, if the view-ID is valid, return all known context-relative URLs with which that resource
     * may be accessed.
     * <p>
     * E.g: If the servlet were mapped to *, given the view-ID "/example.jsp", this method would return
     * "/example"
     */

    List<String> getWebPaths(String path);

    /**
     * Given a web path, return the corresponding resource to which that path would resolve when the application is
     * deployed.
     * <p>
     * E.g: If the servlet were mapped to *, given a web path of, "/example", this method would return a
     * {@link Resource} reference to "$PROJECT_HOME/src/main/webapp/example.jsp"
     */

    Resource<?> getResourceForWebPath(String path);

	boolean installSecurity();
}