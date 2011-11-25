package org.jboss.forge.plugins.spring.mvc;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.Alias;
import javax.inject.Inject;
import javax.persistence.Entity;

import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.DefaultCommand;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.spec.javaee.PersistenceFacet;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.Project;
import org.jboss.seam.render.TemplateCompiler;
import org.jboss.seam.render.template.CompiledTemplateResource;
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
  @Current
  private Resource<?> currentResource;
    
  @Inject
  private Project project;
  
  @Inject
  private XMLParser parser;
  
  // Members for the 'mvc-from-entity' command.
  private static final String SPRING_CONTROLLER_TEMPLATE = "org/jboss/forge/plugins/spring/mvc/SpringController.jv";

  @Inject
  private TemplateCompiler compiler;
  
  private CompiledTemplateResource springControllerTemplate;
  
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
    
    // Add support for MVEL, for controller generation.
    DependencyBuilder mvel = DependencyBuilder.create("org.mvel:mvel2:2.0.11");
    deps.addDependency(mvel);
    
    // Add Seam Render dependency for controller generation.
    DependencyBuilder seamRender = DependencyBuilder.create("org.jboss.seam.render:seam-render:1.0.0.Alpha4");
    deps.addDependency(seamRender);
    
    out.println("Added Spring 3.1.0.RC1 dependencies to pom.xml.");
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
      // Use a ResourceFacet object to write to a new XML file.
      ResourceFacet resources = project.getFacet(ResourceFacet.class);
      
      // Use a MetadataFacet object to retrieve the project's name.
      MetadataFacet meta = project.getFacet(MetadataFacet.class);
      String projectName = meta.getProjectName();
      
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
      emf.attribute("jndi-name", "java:comp/env/persistence/" + unitName);
      emf.attribute("expected-type", "javax.persistence.EntityManager");
      
      /*
       *  Add the <tx:annotation-driven/> element for use of the @Transactional annotation.
       *  This is not necessary unless we choose to annotate controller methods as @Transactional.
       */
      Node tx = new Node("tx:annotation-driven", beans);
      
      // Write the XML tree to a file, using the <beans> root node.
      String file = parser.toXMLString(beans);
      resources.createResource(file.toCharArray(), "META-INF/applicationContext.xml");
      
      // Create a web.xml file and define the persistence unit in web.xml.
      Node webapp = new Node("webapp");
      webapp.attribute("version", "3.0");
      webapp.attribute("xmlns", "http://java.sun.com/xml/ns/javaee");
      webapp.attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
      webapp.attribute("xsi:schemaLocation", "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd");
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
      persistenceContextRefName.text("persistence/" + unitName + "/entityManager");
      Node persistenceUnitName = new Node("persistence-unit-name", persistenceContextRef);
      persistenceUnitName.text(unitName);
      
      file = parser.toXMLString(webapp);
      resources.createResource(file.toCharArray(), "../webapp/WEB-INF/web.xml");
  }
  
  /**
   * The 'web-mvc' command configures Spring MVC in the application context.
   * This command will be necessary to deploy the application, once we have created MVC controllers.
   */
  @Command("web-mvc")
  public void setupMVC(PipeOut out, @Option(required=true, name="package", description="Package containing Spring controllers")
                          final String mvcPackage)
  {
      // Use a ResourceFacet object to retrieve and update XML context files.
      ResourceFacet resources = this.project.getFacet(ResourceFacet.class);
      
      // Use a MetadataFacet object to retrieve the project's name.
      MetadataFacet meta = this.project.getFacet(MetadataFacet.class);
      String projectName = meta.getProjectName();
      
      /*
       * Ensure that the META-INF/applicationContext.xml file exists.
       * If it does not exist, tell the user that they may need to execute 'spring persistence'.
       */
      if(!resources.getResource("../webapp/WEB-INF/web.xml").exists()) {
          out.println("The file 'WEB-INF/web.xml' does not exist.  Have you executed 'spring persistence' yet?");
          return;
      }

      // Create a new mvc-context.xml file for the application.
      Node beans = new Node("beans");
      
      // Add the appropriate Schema references.
      beans.attribute("xmlns", "http://www.springframework.org/schema/beans");
      beans.attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");    
      beans.attribute("xmlns:mvc", "http://www.springframework.org/schema/mvc");
      beans.attribute("xmlns:context", "http://www.springframework.org/schema/context");

      // Add the schema files for the <context> and <mvc> namespaces.
      String schemaLoc = "http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd";
      schemaLoc += " http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd";
      schemaLoc += " http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd";
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
      
      // Write the mvc-context.xml file.
      String file = parser.toXMLString(beans);
      String filename = projectName.toLowerCase().replace(' ', '-');
      resources.createResource(file.toCharArray(), "../webapp/WEB-INF/" + filename + "-mvc-context.xml");
      
      // Retrieve the WEB-INF/web.xml file to be edited.
      
      FileResource<?> webXML = resources.getResource("../webapp/WEB-INF/web.xml");
      Node webapp = parser.parse(webXML.getResourceInputStream());
      
      // Define a Dispatcher servlet, named after the project.
      Node servlet = new Node("servlet", webapp);
      String servName = projectName.replace(' ', (char) 0);
      Node servletName = new Node("servlet-name", servlet);
      servletName.text(servName);
      Node servletClass = new Node("servlet-class", servlet);
      servletClass.text("org.springframework.web.servlet.DispatcherServlet");
      Node initParam = new Node("init-param", servlet);
      Node paramName = new Node("param-name", initParam);
      paramName.text("contextConfigLocation");
      Node paramValue = new Node("param-value", initParam);
      paramValue.text("/WEB-INF/" + filename + ".xml");
      Node loadOnStartup = new Node("load-on-startup", servlet);
      loadOnStartup.text(1);
      
      Node servletMapping = new Node("servlet-mapping", webapp);
      Node servletNameRepeat = new Node("servlet-name", servletMapping);
      servletNameRepeat.text(projectName.replace(' ', (char) 0));
      Node url = new Node("url-pattern", servletMapping);
      url.text('/');
      
      file = parser.toXMLString(webapp);
      resources.createResource(file.toCharArray(), "../webapp/WEB-INF/web.xml");
  }
  
  /**
   * The 'mvc-from-entity' command creates a web controller from an entity.
   * When the application is deployed on the selected container, these controllers will provide a web view of the selected entity.
   * @throws FileNotFoundException 
   */
/*  @Command("mvc-from-entity")
  public void generateFromEntity(PipeOut out, 
          @Option(required = false) JavaResource[] targets,
          @Option(flagOnly = true, name = "overwrite") boolean overwrite)
          throws FileNotFoundException
  {
      JavaSourceFacet java = this.project.getFacet(JavaSourceFacet.class);
      ResourceFacet resources = this.project.getFacet(ResourceFacet.class);
      MetadataFacet meta = this.project.getFacet(MetadataFacet.class);
      
      // Retrieve the base package that will be scanned for controllers.
      String projectName = meta.getProjectName();
      String filePath = "../webapp/WEB-INF/" + projectName.replace(' ', '-') + "-mvc-context.xml";
      Resource<?> mvcContext = resources.getResource(filePath);
      Node beans = parser.parse(mvcContext.getResourceInputStream());
      Node contextScan = beans.getSingle("context:component-scan");
      String mvcPackage = contextScan.getAttribute("base-package");

      // If no target is specified, use the file that is currently open.
      if(targets == null || (targets.length < 1) && (currentResource instanceof JavaResource)) {
          targets = new JavaResource[] {(JavaResource) currentResource};
      }
      
      // Reduced the passed list of targets to a list of @Entity targets.
      List<JavaResource> javaTargets = selectTargets(out, targets);
      
      // If there are no entities passed, return an error message to the user.
      if(javaTargets.isEmpty()) {
          ShellMessages.error(out, "Must specify a domain @Entity on which to operate.");
          return;
      }
      
      // For each @Entity that is detected, create a Spring MVC controller.
      for(JavaResource jr : javaTargets) {
          JavaClass entity = (JavaClass) ((JavaResource) jr).getJavaSource();
          // Call to a function which creates the Spring MVC controller from the given entity.
          List<Resource<?>> controller = generateController(entity, mvcPackage);
          if(!controller.isEmpty()) {
              java.saveJavaSource((JavaSource<?>) controller.get(0));
              ShellMessages.success(out, "Generated Spring MVC Controller for [" + entity.getQualifiedName() + "]");
          }
      }
      
  }
  
  public List<JavaResource> selectTargets(PipeOut out, Resource<?>[] targets)
          throws FileNotFoundException
  {
      List<JavaResource> results = new ArrayList<JavaResource>();
      
      if(targets == null) {
          targets = new Resource<?>[] {};
      }
      
      for(Resource<?> r : targets) {
          if(r instanceof JavaResource) {
              JavaSource<?> entity = ((JavaResource) r).getJavaSource();
              if(entity instanceof JavaClass) {
                  if(entity.hasAnnotation(Entity.class)) {
                      results.add((JavaResource) r);
                  }
                  else {
                      ShellMessages.info(out, "Skipped non-@Entity Java resource [" + entity.getQualifiedName() + "]");
                  }
              }
              else {
                  ShellMessages.info(out, "Skipped non-@Entity Java resource [" + entity.getQualifiedName() + "]");
              }
          }
      }
      
      return results;
  }
  
  public List<Resource<?>> generateController(JavaClass entity, String mvcPackage)
  {
      if(this.springControllerTemplate == null) {
          springControllerTemplate = compiler.compile(SPRING_CONTROLLER_TEMPLATE);
      }
      
      List<Resource<?>> controller = new ArrayList<Resource<?>>();
      
      Map<Object, Object> context = new HashMap<Object, Object>();
      context.put("entity", entity);
      context.put("mvcPackage", mvcPackage);
      
      JavaClass entityController = JavaParser.parse(JavaClass.class, this.springControllerTemplate.render(context));
      controller.add((Resource<?>) entityController);      
      
      return controller;
  }
  */
}