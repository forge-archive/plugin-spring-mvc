package org.jboss.forge.spring;

import javax.inject.Inject;

import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;

/**
 * Forge plugin used to configure Spring Security for a project. This plugin was designed to be used in conjunction with
 * the Spring MVC plugin for Forge.
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
   public void setupSecurity(final PipeOut out)
   {
      // Use the DependencyFacet interface to add each Spring Security dependency to the POM.
      DependencyFacet deps = project.getFacet(DependencyFacet.class);

      /* 
       * Create the dependencies using the Forge DependencyBuilder class.
       * Add the Spring Security core dependency.
       */
      DependencyBuilder springSecurityCore = DependencyBuilder
               .create("org.springframework.security:spring-security-core:3.1.0.CR2");
      deps.addDirectDependency(springSecurityCore);

      // Add the Spring Security config dependency.
      DependencyBuilder springSecurityConfig = DependencyBuilder
               .create("org.springframework.security:spring-security-config:3.1.0.CR2");
      deps.addDirectDependency(springSecurityConfig);

      // Add the Spring Security access-control list dependency, for domain object security.
      DependencyBuilder springSecurityAcl = DependencyBuilder
               .create("org.springframework.security:spring-security-acl:3.1.0.CR2");
      deps.addDirectDependency(springSecurityAcl);

      // Add the Spring Security web dependency.
      DependencyBuilder springSecurityWeb = DependencyBuilder
               .create("org.springframework.security:spring-security-web:3.1.0.CR2");
      deps.addDirectDependency(springSecurityWeb);

      // Add the Spring Security taglibs dependency.
      DependencyBuilder springSecurityTaglibs = DependencyBuilder
               .create("org.springframework.security:spring-security-taglibs:3.1.0.CR2");
      deps.addDirectDependency(springSecurityTaglibs);

      out.println("Configured the project to use Spring Security 3.1.0.CR2.");
   }

}