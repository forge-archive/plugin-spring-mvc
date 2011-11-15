package org.jboss.forge.spring;

import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.ShellPrompt;
import javax.inject.Inject;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.DefaultCommand;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.Shell;

import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.Project;

@Alias("spring")
public class SpringPlugin implements org.jboss.forge.shell.plugins.Plugin {

  @Inject
  private ShellPrompt prompt;

  @Inject
  private Shell shell;

  @DefaultCommand
  public void defaultCommand(PipeOut out) 
  {
    out.println("Welcome to the Spring plugin for Forge!  To set up the project, execute the command 'spring setup'.");
  }

  /*
   * The 'setup' command is used to initialize the project as a simple Spring Web MVC project.
   * For example, this command will add the necessary dependencies to the project's POM.xml file. 
   */
  @Command("setup")
  public void setupProject(PipeOut out)
  {
    Project project = shell.getCurrentProject();
    
    // Use the DependencyFacet interface to add each Spring dependency to the POM.
    DependencyFacet deps = project.getFacet(DependencyFacet.class); 
      
    /*
     * Use the Forge DependencyBuilder to add Maven dependencies to the POM.
     * Add the Spring ASM dependency.
     */ 
    DependencyBuilder springAsm = DependencyBuilder.create("org.springframework:spring-asm:3.1.0.RC1").setScopeType(ScopeType.COMPILE);
    deps.addDependency(springAsm);

    // Add the Spring beans dependency
    DependencyBuilder springBeans = DependencyBuilder.create("org.springframework:spring-beans:3.1.0.RC1").setScopeType(ScopeType.COMPILE);
    deps.addDependency(springBeans);

    // Add the Spring context dependency
    DependencyBuilder springContext = DependencyBuilder.create("org.springframework:spring-context:3.1.0.RC1").setScopeType(ScopeType.COMPILE);
    deps.addDependency(springContext);

    // Add the support for the Spring context dependency
    DependencyBuilder springContextSupport = DependencyBuilder.create("org.springframework:spring-context-support:3.1.0.RC1").setScopeType(ScopeType.COMPILE);
    deps.addDependency(springContextSupport); 

    // Add the support for the Spring core
    DependencyBuilder springCore = DependencyBuilder.create("org.springframework:spring-core:3.1.0.RC1").setScopeType(ScopeType.COMPILE);
    deps.addDependency(springCore); 

     // Add the support for the Spring expression dependency
    DependencyBuilder springExpression = DependencyBuilder.create("org.springframework:spring-expression:3.1.0.RC1").setScopeType(ScopeType.COMPILE);
    deps.addDependency(springExpression);

    // Add the support for the Spring web dependency
    DependencyBuilder springWeb = DependencyBuilder.create("org.springframework:spring-web:3.1.0.RC1").setScopeType(ScopeType.COMPILE);
    deps.addDependency(springWeb);

     // Add the support for the Spring MVC dependency
    DependencyBuilder springMVC = DependencyBuilder.create("org.springframework:spring-webmvc:3.1.0.RC1").setScopeType(ScopeType.COMPILE);
    deps.addDependency(springMVC);

    out.println("Added Spring 3.1.0.RC1 dependencies to pom.xml.");
  }

  /*
   * The 'persistence' command is used to set up persistence for the application.
   * This command is very similar to the 'persistence' plugin for Forge, except it also adds a JNDI look-up to configure Spring.
   */
  @Command("persistence")
  public void persistence(PipeOut out)
  {
	  out.println("Setting up persistence for the web app, very similar to Forge's 'persistence' plugin.");
  }
}
