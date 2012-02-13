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

import org.jboss.forge.scaffold.spring.SpringScaffold;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.Alias;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.DefaultCommand;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.spec.javaee.PersistenceFacet;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.PackagingFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.project.services.ProjectFactory;
import org.jboss.forge.project.Project;

/**
 * Forge plugin to create a simple Spring MVC web application.
 * 
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 *
 */

@Alias("spring")
@RequiresFacet({ DependencyFacet.class,
        PackagingFacet.class,
        ResourceFacet.class,
        WebResourceFacet.class,
        MetadataFacet.class,
        PersistenceFacet.class,
        JavaSourceFacet.class,
        SpringScaffold.class })
public class SpringPlugin implements Plugin {

  @Inject
  private Project project;
  
  @Inject
  private Event<InstallFacets> installFacets;

  // Default Forge persistence unit name.

  public static final String DEFAULT_UNIT_NAME = "forge-default";
  
  @DefaultCommand
  public void defaultCommand(PipeOut out) 
  {
    out.println("Welcome to the Spring plugin for Forge!  To set up the project for Spring MVC use, execute the command 'spring setup'.");
  }

  /**
   * The 'setup' command is used to initialize the project as a simple Spring Web MVC project.
   * For example, this command will add the necessary dependencies to the project's POM.xml file. 
   */
  
  @SetupCommand
  public void setup(PipeOut out)
  {  
    // Use the DependencyFacet interface to add each Spring dependency to the POM.

    DependencyFacet deps = project.getFacet(DependencyFacet.class);
    
    // Use the PackagingFacet interface to get, and potentially modify, the project's packaging type.
    
    PackagingFacet packaging = project.getFacet(PackagingFacet.class);
    
    // If the project was not created as a WAR, change the packaging type to 'WAR' and install a WebResourceFacet.

    if(packaging.getPackagingType() != PackagingType.WAR)
    {
        packaging.setPackagingType(PackagingType.WAR);
    }
    
    // Install facets that will be required

    // Use the Forge DependencyFacet to add Maven dependencies to the POM.

    String springVersion = "3.1.0.RC1";
    
    deps.setProperty("spring.version", springVersion);
    
    // Add the Spring ASM dependency.

    DependencyBuilder springAsm = DependencyBuilder.create("org.springframework:spring-asm:${spring.version}");
    deps.addDirectDependency(springAsm);

    // Add the Spring beans dependency.

    DependencyBuilder springBeans = DependencyBuilder.create("org.springframework:spring-beans:${spring.version}");
    deps.addDirectDependency(springBeans);

    // Add the Spring context dependency.

    DependencyBuilder springContext = DependencyBuilder.create("org.springframework:spring-context:${spring.version}");
    deps.addDirectDependency(springContext);

    // Add the support for the Spring context dependency.

    DependencyBuilder springContextSupport = DependencyBuilder.create("org.springframework:spring-context-support:${spring.version}");
    deps.addDirectDependency(springContextSupport); 

    // Add the Spring core.

    DependencyBuilder springCore = DependencyBuilder.create("org.springframework:spring-core:${spring.version}");
    deps.addDirectDependency(springCore); 

     // Add the Spring expression dependency.

    DependencyBuilder springExpression = DependencyBuilder.create("org.springframework:spring-expression:${spring.version}");
    deps.addDirectDependency(springExpression);

    // Add the Spring web dependency.

    DependencyBuilder springWeb = DependencyBuilder.create("org.springframework:spring-web:${spring.version}");
    deps.addDirectDependency(springWeb);

     // Add the Spring MVC dependency.

    DependencyBuilder springMVC = DependencyBuilder.create("org.springframework:spring-webmvc:${spring.version}");
    deps.addDirectDependency(springMVC);
    
    out.println("Added Spring " + springVersion + " dependencies to pom.xml.");
}

  /**
   * The 'persistence' command is used to configure the persistence layer for the application.
   * Before executing 'spring persistence', the Forge provided command 'persistence setup' should have been executed.
   * Thus, the application's persistence layer should already be mostly configured in META-INF/persistence.xml.
   * This command should perform the necessary steps to configure Spring persistence, e.g. an EntityManager JNDI look-up.
   */
  
  @SuppressWarnings("unused")
  @Command("persistence")
  public void springPersistence(PipeOut out)
  {
      // First, check to see that a PersistenceFacet has been installed, otherwise, 'persistence setup' may not have been executed.

      if(!project.hasFacet(PersistenceFacet.class)) {
          out.println("No PersistenceFacet installed, have you executed 'persistence setup' yet?");
          return;
      }

      // Use a ResourceFacet object to write to a new XML file.

	  ResourceFacet resources = project.getFacet(ResourceFacet.class);
      
      // Use a WebResourceFacet object to write a new web.xml file.

      WebResourceFacet web = project.getFacet(WebResourceFacet.class);

      // Use a MetadataFacet object to retrieve the project's name.

      MetadataFacet meta = project.getFacet(MetadataFacet.class);
      String projectName = meta.getProjectName();

      // Use the XMLParser provided by Forge to create an applicationContext.xml file.
      
      // The top-level element of the XML file, <beans>, will contain schema inclusions.

      Node beans = new Node("beans");
      beans.setComment(false);
      
      // Add each schema as a separate attribute of the <beans> element.

      beans.attribute("xmlns", "http://www.springframework.org/schema/beans");
      beans.attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
      beans.attribute("xmlns:tx", "http://www.springframework.org/schema/tx");
      beans.attribute("xmlns:jee", "http://www.springframework.org/schema/jee");
      
      // schemaLoc contains the locations of each schema file.

      String schemaLoc = "http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd";
      schemaLoc += " http://www.springframework.org/schema/jee http://www.springframework.org/schema/jee/spring-jee.xsd";
      schemaLoc += " http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx.xsd";
      beans.attribute("xsi:schemaLocation", schemaLoc);
      
      // Perform a JNDI lookup to retrieve an EntityManagerFactory, of type javax.persistence.EntityManagerFactory.

      Node emf = new Node("jee:jndi-lookup", beans);
      emf.setComment(false);
      emf.attribute("id", "entityManagerFactory");
      emf.attribute("jndi-name", "java:comp/env/persistence/" + DEFAULT_UNIT_NAME);
      emf.attribute("expected-type", "javax.persistence.EntityManager");
      
      /*
       *  Add the <tx:annotation-driven/> element for use of the @Transactional annotation.
       *  This is not necessary unless we choose to annotate controller methods as @Transactional.
       */

      Node tx = new Node("tx:annotation-driven", beans);
      
      // Write the XML tree to a file, using the <beans> root node.

      String file = XMLParser.toXMLString(beans);
      resources.createResource(file.toCharArray(), "META-INF/applicationContext.xml");
      
      // Create a web.xml file and define the persistence unit in web.xml.

      Node webapp = new Node("webapp");
      webapp.attribute("version", "3.0");
      webapp.attribute("xmlns", "http://java.sun.com/xml/ns/javaee");
      webapp.attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
      webapp.attribute("xsi:schemaLocation", "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd");
      webapp.attribute("metadata-complete", "true");
      
      // Add the project name as an attribute of web.xml.

      Node displayName = new Node("display");
      displayName.text(projectName);
      
      // Include the files containing the web application's context.

      Node contextParam = new Node("context-param", displayName);
      Node contextConfig = new Node("param-name", contextParam);
      contextConfig.text("contextConfigLocation");
      Node configLocation = new Node("param-value", contextConfig);
      configLocation.text("classpath:/META-INF/applicationContext.xml");
      
      // Define a ContextLoaderListener.

      Node listener = new Node("listener", webapp);
      Node cll = new Node("listener-class", listener);
      cll.text("org.springframework.web.context.ContextLoaderListener");
      
      // Define a persistence unit to be referenced in the application context.

      Node persistenceContextRef = new Node("persistence-context-ref", webapp);
      Node persistenceContextRefName = new Node("persistence-context-ref-name", persistenceContextRef);
      persistenceContextRefName.text("persistence/" + DEFAULT_UNIT_NAME + "/entityManager");
      Node persistenceUnitName = new Node("persistence-unit-name", persistenceContextRef);
      persistenceUnitName.text(DEFAULT_UNIT_NAME);
      
      // Save the updated web.xml file.
      
      file = XMLParser.toXMLString(webapp);
      web.createWebResource(file.toCharArray(), "WEB-INF/web.xml");
  }
}