package org.jboss.forge.spec.spring.security;

import org.jboss.forge.project.Facet;
import org.jboss.forge.project.Project;
import org.jboss.forge.resources.FileResource;

/**
 * If installed, this {@link Project} supports features from Spring MVC.
 * 
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public interface SpringSecurityFacet extends Facet
{
    FileResource<?> getSecurityContextFile(String targetDir);

    String getTargetDir();

    void setTargetDir(String targetDir);

}