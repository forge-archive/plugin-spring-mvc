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
     * Get this application's applicationContext.xml file.
     */

    FileResource<?> getContextFile();

    /**
     * Get the servlet XML context file for the specified targetDir of the application.
     */

    FileResource<?> getMvcContextFile(String targetDir);

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

    /**
     * Given a servlet name, return whether or not the application's web.xml file contains a servlet definition with that
     * name.
     */

    boolean hasServlet(String servletName);
}