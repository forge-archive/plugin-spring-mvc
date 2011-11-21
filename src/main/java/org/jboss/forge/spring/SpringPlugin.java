package org.jboss.forge.spring;

import org.jboss.forge.resources.FileResource;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellPrompt;

import javax.inject.Inject;

import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.DefaultCommand;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.spec.javaee.PersistenceFacet;
import org.jboss.forge.spec.javaee.jpa.EntityPlugin;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.Project;
import org.jboss.shrinkwrap.descriptor.api.spec.jpa.persistence.PersistenceDescriptor;

/**
 * Forge plugin to create a simple Spring MVC web application.
 * 
 * @author <a href="mailto:rbradley@redhat.com">Ryan Bradley</a>
 *
 */

@Alias("spring")
public class SpringPlugin implements Plugin {

  @Inject
  private ShellPrompt prompt;
  
  @Inject
  private Shell shell;
  
  @Inject
  private Project project;
  
  @Inject
  private XMLParser parser;
  
  @DefaultCommand
  public void defaultCommand(PipeOut out) 
  {
    out.println("Welcome to the Spring plugin for Forge!  To set up the project, execute the command 'spring setup'.");
  }

  /**
   * The 'setup' command is used to initialize the project as a simple Spring Web MVC project.
   * For example, this command will add the necessary dependencies to the project's POM.xml file. 
   */
  @Command("setup")
  public void setupProject(PipeOut out)
  {  
    // Use the DependencyFacet interface to add each Spring dependency to the POM.
    DependencyFacet deps = project.getFacet(DependencyFacet.class); 
      
    /*
     * Use the Forge DependencyBuilder to add Maven dependencies to the POM.
     * Add the Spring ASM dependency.
     */ 
    DependencyBuilder springAsm = DependencyBuilder.create("org.springframework:spring-asm:3.1.0.RC1");
    deps.addDependency(springAsm);

    // Add the Spring beans dependency
    DependencyBuilder springBeans = DependencyBuilder.create("org.springframework:spring-beans:3.1.0.RC1");
    deps.addDependency(springBeans);

    // Add the Spring context dependency
    DependencyBuilder springContext = DependencyBuilder.create("org.springframework:spring-context:3.1.0.RC1");
    deps.addDependency(springContext);

    // Add the support for the Spring context dependency
    DependencyBuilder springContextSupport = DependencyBuilder.create("org.springframework:spring-context-support:3.1.0.RC1");
    deps.addDependency(springContextSupport); 

    // Add the support for the Spring core
    DependencyBuilder springCore = DependencyBuilder.create("org.springframework:spring-core:3.1.0.RC1");
    deps.addDependency(springCore); 

     // Add the support for the Spring expression dependency
    DependencyBuilder springExpression = DependencyBuilder.create("org.springframework:spring-expression:3.1.0.RC1");
    deps.addDependency(springExpression);

    // Add the support for the Spring web dependency
    DependencyBuilder springWeb = DependencyBuilder.create("org.springframework:spring-web:3.1.0.RC1");
    deps.addDependency(springWeb);

     // Add the support for the Spring MVC dependency
    DependencyBuilder springMVC = DependencyBuilder.create("org.springframework:spring-webmvc:3.1.0.RC1");
    deps.addDependency(springMVC);

    out.println("Added Spring 3.1.0.RC1 dependencies to pom.xml.");
  }

  /**
   * The 'persistence' command is used to configure the persistence layer for the application.
   * Before executing 'spring persistence', the Forge provided command 'persistence setup' should have been executed.
   * Thus, the application's persistence layer should already be mostly configured in META-INF/persistence.xml.
   * This command should perform the necessary steps to configure Spring persistence, e.g. an EntityManager JNDI look-up.
   */
  @Command("persistence")
  public void persistenceSpring(PipeOut out)
  {
      // Use a ResourceFacet object to write to a new XML file.
      ResourceFacet resources = project.getFacet(ResourceFacet.class);
      
      /*
       * Use a PersistenceFacet object to retrieve the project's persistence configuration.
       * This persistence configuration can then be used to retrieve the appropriate persistence unit name (for a JNDI lookup).
       */
      PersistenceFacet jpa = project.getFacet(PersistenceFacet.class);
      PersistenceDescriptor config = jpa.getConfig();
      String unitName = config.listUnits().get(0).getName();
      out.println(unitName);
      
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
      emf.attribute("jndi-name", "java:comp/env/persistence" + unitName);
      emf.attribute("expected-type", "javax.persistence.EntityManagerFactory");
      
      /*
       *  Add the <tx:annotation-driven/> element for use of the @Transactional annotation.
       *  This is not necessary unless we choose to annotate controller methods as @Transactional.
       */
      Node tx = new Node("tx:annotation-driven", beans);
      
      // Write the XML tree to a file, using the <beans> root node.
      String file = parser.toXMLString(beans);
      resources.createResource(file.toCharArray(), "META-INF/applicationContext.xml");
  }
  
  /**
   * The 'entity' command is used to create a new entity class.
   * This command invokes the 'entity' plugin provided with Forge.
   */
  @Command("entity")
  public void createEntity(@Option(required = true, name = "named", description = "The @Entity name") final String entityName,
                          @Option(required = false, name = "package", type = PromptType.JAVA_PACKAGE, description = "The package name") final String packageName)
  {
      EntityPlugin entityPlugin = new EntityPlugin(this.project, this.shell);
      try {
        entityPlugin.newEntity(entityName, packageName);
    } catch (Throwable e) {
        e.printStackTrace();
    }
  }
  
  /**
   * The 'mvc-setup' command configures Spring MVC in the application context.
   * This command will be necessary to deploy the application, once we have created MVC controllers.
   */
  @Command("mvc-setup")
  public void setupMVC(PipeOut out, @Option(required=true, name="package", description="Package containing Spring controllers")
                          final String mvcPackage)
  {
      ResourceFacet resources = this.project.getFacet(ResourceFacet.class);
      
      /*
       * Ensure that the META-INF/applicationContext.xml file exists.
       * If it does not exist, tell the user that they may need to execute 'spring persistence'.
       */
      if(!resources.getResource("META-INF/applicationContext.xml").exists()) {
          out.println("The file 'META-INF/applicationContext.xml' does not exist.  Have you executed 'spring persistence' yet?");
          return;
      }

      // Retrieve the current contents of applicationContext.xml.
      FileResource<?> applicationContext = resources.getResource("META-INF/applicationContext.xml");
      Node beans = parser.parse(applicationContext.getResourceInputStream());
      
      // Add the Spring <context> and Spring <mvc> namespaces.
      beans.attribute("xmlns:mvc", "http://www.springframework.org/schema/mvc");
      beans.attribute("xmlns:context", "http://www.springframework.org/schema/context");

      // Add the schema files for the <context> and <mvc> namespaces.
      String schemaLoc = "http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd";
      schemaLoc += " http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd";
      schemaLoc += " http://www.springframework.org/schema/jee http://www.springframework.org/schema/jee/spring-jee.xsd";
      schemaLoc += " http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd";
      schemaLoc += " http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx.xsd";
      beans.attribute("xsi:schemaLocation", schemaLoc);
      
      // Scan the given package for any classes with MVC annotations.
      Node contextScan = new Node("context:component-scan", beans);
      contextScan.attribute("base-package", mvcPackage);
      
      // Indicate that we will use Spring MVC annotations, such as @Controller or @RequestMapping.
      Node mvcAnnotation = new Node("mvc:annotation-driven", beans);
      
      // Use the Spring MVC default servlet handler.
      Node mvcServlet = new Node("mvc:default-servlet-handler", beans);
      
      // Unnecessary if there is no static content, but harmless.
      Node mvcStatic = new Node("mvc:resources", beans);
      mvcStatic.attribute("mapping", "/static/**");
      mvcStatic.attribute("location", "/");
      
      // Write the updated applicationContext.xml file.
      String file = parser.toXMLString(beans);
      resources.createResource(file.toCharArray(), "META-INF/applicationContext.xml");
  }
  
}
