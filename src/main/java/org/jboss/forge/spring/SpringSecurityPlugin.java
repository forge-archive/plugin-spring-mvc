package org.jboss.forge.spring;


import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.ShellPrompt;
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
	private ShellPrompt prompt;
	
	@Inject
	private Project project;
	
	@Command("setup")
	public void setupSecurity(PipeOut out)
	{	    
	    // Use the DependencyFacet interface to add each Spring Security dependency to the POM.
	    DependencyFacet deps = project.getFacet(DependencyFacet.class);
	    
	    /* 
	     * Create the dependencies using the Forge DependencyBuilder class.
	     * Add the Spring Security core dependency.
	     */
	    DependencyBuilder springSecurityCore = DependencyBuilder.create("org.springframework.security:spring-security-core:3.1.0.CR2");
	    deps.addDependency(springSecurityCore);
	    
	    // Add the Spring Security config dependency.	    
	    DependencyBuilder springSecurityConfig = DependencyBuilder.create("org.springframework.security:spring-security-config:3.1.0.CR2");
	    deps.addDependency(springSecurityConfig);
	    
	    // Add the Spring Security access-control list dependency, for domain object security.
	    DependencyBuilder springSecurityAcl = DependencyBuilder.create("org.springframework.security:spring-security-acl:3.1.0.CR2");
	    deps.addDependency(springSecurityAcl);
	    
	    // Add the Spring Security web dependency.
	    DependencyBuilder springSecurityWeb = DependencyBuilder.create("org.springframework.security:spring-security-web:3.1.0.CR2");
	    deps.addDependency(springSecurityWeb);
	    
	    // Add the Spring Security taglibs dependency.
	    DependencyBuilder springSecurityTaglibs = DependencyBuilder.create("org.springframework.security:spring-security-taglibs:3.1.0.CR2");
	    deps.addDependency(springSecurityTaglibs);
	    
        out.println("Configured the project to use Spring Security 3.1.0.CR2.");
	}

}