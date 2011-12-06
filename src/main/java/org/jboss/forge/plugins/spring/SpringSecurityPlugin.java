package org.jboss.forge.plugins.spring;

import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.Alias;
import javax.inject.Inject;


/**
 * Forge plugin used to configure Spring Security for a project.
 * This plugin was designed to be used in conjunction with the Spring MVC plugin for Forge.
 * 
 * @author <a href="mailto:rbradley@redhat.com">Ryan Bradley</a>
 *
 */

@Alias("spring-security")
public class SpringSecurityPlugin implements Plugin 
{

	@Inject
	private Project project;
	
	@Command("setup")
	public void setupSecurity(PipeOut out)
	{	    
	    // Use the DependencyFacet interface to add each Spring Security dependency to the POM.
	    DependencyFacet deps = project.getFacet(DependencyFacet.class);
	    deps.setProperty("spring.security.version", "3.1.0.CR2");
	    
	    /* 
	     * Create the dependencies using the Forge DependencyBuilder class.
	     * Add the Spring Security core dependency.
	     */
	    DependencyBuilder secCore = DependencyBuilder.create("org.springframework.security:spring-security-core:{spring.security.version}");
	    deps.addDependency(secCore);
	    
	    // Add the Spring Security config dependency.	    
	    DependencyBuilder secConfig = DependencyBuilder.create("org.springframework.security:spring-security-config:{spring.security.version}");
	    deps.addDependency(secConfig);
	    
	    // Add the Spring Security access-control list dependency, for domain object security.
	    DependencyBuilder secAcl = DependencyBuilder.create("org.springframework.security:spring-security-acl:{spring.security.version}");
	    deps.addDependency(secAcl);
	    
	    // Add the Spring Security web dependency.
	    DependencyBuilder secWeb = DependencyBuilder.create("org.springframework.security:spring-security-web:{spring.security.version}");
	    deps.addDependency(secWeb);
	    
	    // Add the Spring Security taglibs dependency.
	    DependencyBuilder secTaglibs = DependencyBuilder.create("org.springframework.security:spring-security-taglibs:{spring.security.version}");
	    deps.addDependency(secTaglibs);
	    
        out.println("Configured the project to use Spring Security 3.1.0.CR2.");
	}

}