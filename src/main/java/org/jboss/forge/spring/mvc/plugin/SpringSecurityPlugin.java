/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.forge.spring.mvc.plugin;

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
	    deps.addDirectDependency(secCore);
	    
	    // Add the Spring Security config dependency.	    
	    DependencyBuilder secConfig = DependencyBuilder.create("org.springframework.security:spring-security-config:{spring.security.version}");
	    deps.addDirectDependency(secConfig);
	    
	    // Add the Spring Security access-control list dependency, for domain object security.
	    DependencyBuilder secAcl = DependencyBuilder.create("org.springframework.security:spring-security-acl:{spring.security.version}");
	    deps.addDirectDependency(secAcl);
	    
	    // Add the Spring Security web dependency.
	    DependencyBuilder secWeb = DependencyBuilder.create("org.springframework.security:spring-security-web:{spring.security.version}");
	    deps.addDirectDependency(secWeb);
	    
	    // Add the Spring Security taglibs dependency.
	    DependencyBuilder secTaglibs = DependencyBuilder.create("org.springframework.security:spring-security-taglibs:{spring.security.version}");
	    deps.addDirectDependency(secTaglibs);
	    
        out.println("Configured the project to use Spring Security 3.1.0.CR2.");
	}

}