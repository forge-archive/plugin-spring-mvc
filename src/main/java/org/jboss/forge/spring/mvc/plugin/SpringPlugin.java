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

import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.Alias;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.shell.plugins.DefaultCommand;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.shell.plugins.Topic;
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
import org.jboss.forge.project.Project;
import org.jboss.shrinkwrap.descriptor.api.spec.jpa.persistence.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.spec.jpa.persistence.PersistenceUnitDef;

/**
 * Forge plugin to create a simple Spring MVC web application.
 * 
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 *
 */

@Alias("spring")
@Topic("Web Application Setup")
@Help("Spring MVC applications")
@RequiresProject
@RequiresFacet({ DependencyFacet.class,
        PackagingFacet.class,
        ResourceFacet.class,
        WebResourceFacet.class,
        MetadataFacet.class,
        PersistenceFacet.class,
        JavaSourceFacet.class })
public class SpringPlugin implements Plugin {

    //
    // Private members
    //
    
    @Inject
    private Project project;

    @Inject
    private Event<InstallFacets> install;

    private static final String XMLNS_PREFIX = "xmlns:";

    /**
     * The 'setup' command is used to initialize the project as a simple Spring Web MVC project.
     * For example, this command will add the necessary dependencies to the project's POM.xml file. 
    */

    @SetupCommand
    public void setup(PipeOut out)
    {  
        // Get the required Facets to add dependencies and create web.xml and the business context XML file

        DependencyFacet deps = project.getFacet(DependencyFacet.class);
        MetadataFacet meta = project.getFacet(MetadataFacet.class);
        PackagingFacet packaging = project.getFacet(PackagingFacet.class);
        ResourceFacet resources = project.getFacet(ResourceFacet.class);

        // If the project was not created as a WAR, change the packaging type to 'WAR' and install a WebResourceFacet

        if(packaging.getPackagingType() != PackagingType.WAR)
        {
            packaging.setPackagingType(PackagingType.WAR);
            this.install.fire(new InstallFacets(WebResourceFacet.class));
        }

        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        // Use the Forge DependencyFacet to add Spring dependencies to the POM

        String springVersion = "3.1.0.RELEASE";

        deps.setProperty("spring.version", springVersion);

        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-asm:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-beans:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-context:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-context-support:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-core:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-expression:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-tx:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-web:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-webmvc:${spring.version}"));
 
        out.println("Added Spring " + springVersion + " dependencies to pom.xml.");

        // Create the applicationContext.xml file

        Node beans = new Node("beans");

        // Add the necessary schema files to the application context

        beans.attribute("xmlns", "http://www.springframework.org/schema/beans");
        beans.attribute(XMLNS_PREFIX + "xsi", "http://www.w3.org/2001/XMLSchema-instance");
        String schemaLoc = "http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd";
        beans.attribute("xsi:schemaLocation", schemaLoc);

        // Write applicationContext.xml

        String file = XMLParser.toXMLString(beans);
        resources.createResource(file.toCharArray(), "META-INF/spring/applicationContext.xml");

        // Create a web.xml file for the application

        Node webapp = new Node("web-app");
        webapp.attribute("version", "3.0");
        webapp.attribute("xmlns", "http://java.sun.com/xml/ns/javaee");
        webapp.attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        webapp.attribute("xsi:schemaLocation", "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd");
        webapp.attribute("metadata-complete", "true");

        // Add the project display name to web.xml

        if (webapp.get("display-name").isEmpty())
        {
            Node display = webapp.createChild("display-name");
            display.text(meta.getProjectName());            
        }

        // Add applicationContext.xml to the web application's context

        if (webapp.get("context-param").isEmpty())
        {
            Node contextParam = new Node("context-param", webapp);
            Node contextConfig = new Node("param-name", contextParam);
            contextConfig.text("contextConfigLocation");
            Node configLocation = new Node("param-value", contextParam);
            configLocation.text("classpath:/META-INF/spring/applicationContext.xml");            
        }

        // Define a ContextLoaderListener

        if (webapp.get("listener").isEmpty())
        {
            Node listener = new Node("listener", webapp);
            Node cll = new Node("listener-class", listener);
            cll.text("org.springframework.web.context.ContextLoaderListener");            
        }

        // Save the web.xml file to WEB-INF/web.xml

        file = XMLParser.toXMLString(webapp);
        web.createWebResource(file.toCharArray(), "WEB-INF/web.xml");
    }

    /**
     * The 'persistence' command is used to configure the persistence layer for the application.
     * Before executing 'spring persistence', the Forge provided command 'persistence setup' should have been executed.
     * Thus, the application's persistence layer should already be mostly configured in META-INF/persistence.xml.
     * This command should perform the necessary steps to configure Spring persistence, e.g. an EntityManager JNDI look-up.
     */
  
    @Command("persistence")
    public void springPersistence(PipeOut out)
    {
      // First, check to see that a PersistenceFacet has been installed, otherwise, 'persistence setup' may not have been executed.

        if(!project.hasFacet(PersistenceFacet.class))
        {
          out.println("No PersistenceFacet installed, have you executed 'persistence setup' yet?");
          return;
        }

        // Retrieve the required facets for the command.

        ResourceFacet resources = project.getFacet(ResourceFacet.class);
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);
        PersistenceFacet persistence = project.getFacet(PersistenceFacet.class);
        MetadataFacet meta = project.getFacet(MetadataFacet.class);

        Node beans = XMLParser.parse(resources.getResource("META-INF/spring/applicationContext.xml").getResourceInputStream());
        beans.attribute(XMLNS_PREFIX + "jee", "http://www.springframework.org/schema/jee");
        beans.attribute(XMLNS_PREFIX + "tx", "http://www.springframework.org/schema/tx");
        beans.attribute(XMLNS_PREFIX + "context", "http://www.springframework.org/schema/context");

        String schemaLoc = beans.getAttribute("xsi:schemaLocation");
        schemaLoc += " http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd";
        schemaLoc += " http://www.springframework.org/schema/jee http://www.springframework.org/schema/jee/spring-jee.xsd";
        schemaLoc += " http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx.xsd";
        beans.attribute("xsi:schemaLocation", schemaLoc);

        // Scan the application for any @Repository annotated classes in the repository package.

        if (beans.get("context:component-scan").isEmpty())
        {
            Node componentScan = new Node("context:component-scan", beans);
            componentScan.attribute("base-package", meta.getTopLevelPackage() + ".repo");
        }

        // Add a JTA Transaction Manager to the web application

        if (beans.get("tx:jta-transaction-manager").isEmpty())
        {
            beans.createChild("tx:jta-transaction-manager");
        }

        // Indicate that Spring transactions will be annotation driven (potentially move to 'spring persistence' command?)

        if (beans.get("tx:annotation-driven").isEmpty())
        {
            beans.createChild("tx:annotation-driven");
        }
        
        // Perform a JNDI lookup to retrieve an EntityManagerFactory, of type javax.persistence.EntityManagerFactory.

        PersistenceDescriptor descriptor =  persistence.getConfig();
        PersistenceUnitDef defaultUnit = descriptor.listUnits().get(0);

        if (beans.get("jee:jndi-lookup").isEmpty())
        {
            Node emf = new Node("jee:jndi-lookup", beans);
            emf.setComment(false);
            emf.attribute("id", "entityManager");
            emf.attribute("jndi-name", "java:comp/env/persistence/" + defaultUnit.getName() + "/entityManager");
            emf.attribute("expected-type", "javax.persistence.EntityManager");           
        }

        // Write the XML tree to a file, using the <beans> root node.
    
        String file = XMLParser.toXMLString(beans);
        resources.createResource(file.toCharArray(), "META-INF/spring/applicationContext.xml");
          
        // Add a persistence unit definition in web.xml.

        Node webapp = XMLParser.parse(web.getWebResource("WEB-INF/web.xml").getResourceInputStream());

        // Define a persistence unit to be referenced in the application context.

        if (webapp.get("persistence-context-ref").isEmpty())
        {
            Node persistenceContextRef = new Node("persistence-context-ref", webapp);
            Node persistenceContextRefName = new Node("persistence-context-ref-name", persistenceContextRef);
            persistenceContextRefName.text("persistence/" + defaultUnit.getName() + "/entityManager");
            Node persistenceUnitName = new Node("persistence-unit-name", persistenceContextRef);
            persistenceUnitName.text(defaultUnit.getName());            
        }

        // Save the updated web.xml file.

        file = XMLParser.toXMLString(webapp);
        web.createWebResource(file.toCharArray(), "WEB-INF/web.xml");
    }

    @DefaultCommand
    @Command("help")
    public void help(PipeOut out)
    {
        out.println("Welcome to the Spring plugin for Forge!  To set up the project for Spring MVC use, execute the command 'spring setup'.");
    }
}