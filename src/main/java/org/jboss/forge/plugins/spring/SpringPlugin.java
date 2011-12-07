package org.jboss.forge.plugins.spring;

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

import static org.jvnet.inflector.Noun.pluralOf;

import javax.inject.Inject;
import javax.persistence.Entity;

import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.DefaultCommand;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.spec.javaee.PersistenceFacet;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.impl.JavaClassImpl;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.PackagingFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.project.services.ProjectFactory;
import org.jboss.forge.project.Project;
import org.jboss.seam.render.TemplateCompiler;
import org.jboss.seam.render.spi.TemplateResolver;
import org.jboss.seam.render.template.CompiledTemplateResource;
import org.jboss.seam.render.template.resolver.ClassLoaderTemplateResolver;
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

  // Use a ProjectFactory to retrieve a WebResourceFacet.
  @Inject
  private ProjectFactory factory;
  
  // Members for the 'mvc-from-entity' command.
  private static final String SPRING_CONTROLLER_TEMPLATE = "org/jboss/forge/plugins/spring/mvc/SpringControllerTemplate.jv";
  private static final String DAO_INTERFACE_TEMPLATE = "org/jboss/forge/plugins/spring/repo/DaoInterfaceTemplate.jv";
  private static final String DAO_IMPLEMENTATION_TEMPLATE = "org/jboss/forge/plugins/spring/repo/DaoImplementationTemplate.jv";
  private static final String CREATE_TEMPLATE = "org/jboss/forge/plugins/spring/mvc/create.jsp";
  private static final String LIST_TEMPLATE = "org/jboss/forge/plugins/spring/mvc/list.jsp";
  private static final String VIEW_TEMPLATE = "org/jboss/forge/plugins/spring/mvc/create.jsp";

  private TemplateCompiler compiler;
  private TemplateResolver<ClassLoader> resolver;
  
  private CompiledTemplateResource springControllerTemplate;
  private CompiledTemplateResource daoInterfaceTemplate;
  private CompiledTemplateResource daoImplentationTemplate;
  private CompiledTemplateResource createTemplate;
  private CompiledTemplateResource listTemplate;
  private CompiledTemplateResource viewTemplate;
  
  // MVEL 2.0 Test Templates
  private static final String PROPERTIES_TEMPLATE = "org/jboss/forge/plugins/spring/mvc/properties.jsp";
  private CompiledTemplateResource propertiesTemplate;
  
  @Inject
  public SpringPlugin(TemplateCompiler compiler)
  {
      this.compiler = compiler;
      
      this.resolver = new ClassLoaderTemplateResolver(SpringPlugin.class.getClassLoader());
      compiler.getTemplateResolverFactory().addResolver(resolver);
  }
  
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
  public void setupProject(PipeOut out)
  {  
    // Use the DependencyFacet interface to add each Spring dependency to the POM.
    DependencyFacet deps = project.getFacet(DependencyFacet.class);
    
    // Use the PackagingFacet interface to get, and potentially modify, the project's packaging type.
    PackagingFacet packaging = project.getFacet(PackagingFacet.class);
    
    // If the project was not created as a WAR, change the packaging type to 'WAR' and install a WebResourceFacet.   
    if(packaging.getPackagingType() != PackagingType.WAR) {
        packaging.setPackagingType(PackagingType.WAR);
        factory.installSingleFacet(project, WebResourceFacet.class);
    }

    // Use the Forge DependencyBuilder to add Maven dependencies to the POM. 
    String springVersion = "3.1.0.RC1";
    deps.setProperty("spring.version", springVersion);
    deps.setProperty("forge.api.version", "1.0.0.Beta3");
    
    // Add the Spring ASM dependency.
    DependencyBuilder springAsm = DependencyBuilder.create("org.springframework:spring-asm:${spring.version}");
    deps.addDependency(springAsm);

    // Add the Spring beans dependency.
    DependencyBuilder springBeans = DependencyBuilder.create("org.springframework:spring-beans:${spring.version}");
    deps.addDependency(springBeans);

    // Add the Spring context dependency.
    DependencyBuilder springContext = DependencyBuilder.create("org.springframework:spring-context:${spring.version}");
    deps.addDependency(springContext);

    // Add the support for the Spring context dependency.
    DependencyBuilder springContextSupport = DependencyBuilder.create("org.springframework:spring-context-support:${spring.version}");
    deps.addDependency(springContextSupport); 

    // Add the support for the Spring core.
    DependencyBuilder springCore = DependencyBuilder.create("org.springframework:spring-core:${spring.version}");
    deps.addDependency(springCore); 

     // Add the support for the Spring expression dependency.
    DependencyBuilder springExpression = DependencyBuilder.create("org.springframework:spring-expression:${spring.version}");
    deps.addDependency(springExpression);

    // Add the support for the Spring web dependency.
    DependencyBuilder springWeb = DependencyBuilder.create("org.springframework:spring-web:${spring.version}");
    deps.addDependency(springWeb);

     // Add the support for the Spring MVC dependency.
    DependencyBuilder springMVC = DependencyBuilder.create("org.springframework:spring-webmvc:${spring.version}");
    deps.addDependency(springMVC);
    
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
      // Use a ResourceFacet object to write to a new XML file.
      ResourceFacet resources = project.getFacet(ResourceFacet.class);
      
      // Use a WebResourceFacet object to write a new web.xml file.    
      WebResourceFacet web = project.getFacet(WebResourceFacet.class);

      // Use a MetadataFacet object to retrieve the project's name.
      MetadataFacet meta = project.getFacet(MetadataFacet.class);
      String projectName = meta.getProjectName();
      
      /*
       * First, check to see that a PersistenceFacet has been installed, otherwise, 'persistence setup' may not have been executed.
       */
      
      if(!project.hasFacet(PersistenceFacet.class)) {
          out.println("No PersistenceFacet installed, have you executed 'persistence setup' yet?");
          return;
      }
      
      /*
       * Use a PersistenceFacet object to retrieve the project's persistence configuration.
       * This persistence configuration can then be used to retrieve the appropriate persistence unit name (for a JNDI lookup).
       */
      
      PersistenceFacet jpa = project.getFacet(PersistenceFacet.class);
      PersistenceDescriptor config = jpa.getConfig();
      String unitName = config.listUnits().get(0).getName();
      
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
      persistenceContextRefName.text("persistence/" + unitName + "/entityManager");
      Node persistenceUnitName = new Node("persistence-unit-name", persistenceContextRef);
      persistenceUnitName.text(unitName);
      
      // Save the updated web.xml file.
      file = XMLParser.toXMLString(webapp);
      web.createWebResource(file.toCharArray(), "WEB-INF/web.xml");
  }
  
  /**
   * The 'web-mvc' command configures Spring MVC in the application context.
   * This command will be necessary to deploy the application, once we have created MVC controllers.
   */
  @SuppressWarnings("unused")
@Command("web-mvc")
  public void springMVC(PipeOut out, @Option(required=true, name="package", description="Package containing Spring controllers")
                          final String mvcPackage)
  {
      // Use a ResourceFacet object to retrieve and update XML context files.
      ResourceFacet resources = this.project.getFacet(ResourceFacet.class);
      
      // Use a WebResourceFacet object to retrieve and update the MVC context XML and web.xml files.
      WebResourceFacet web = project.getFacet(WebResourceFacet.class);
      
      // Use a MetadataFacet object to retrieve the project's name.
      MetadataFacet meta = this.project.getFacet(MetadataFacet.class);
      String projectName = meta.getProjectName();
      
      /*
       * Ensure that the META-INF/applicationContext.xml file exists.
       * If it does not exist, tell the user that they may need to execute 'spring persistence'.
       */
      if(!web.getWebResource("WEB-INF/web.xml").exists()) {
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

      // Add the schema files for the <context> and <mvc> namespaces to the mvc-context.xml file.
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
      
      // Write the mvc-context.xml file: 'src/main/webapp/WEB-INF/{lowercase.project.name}-mvc-context.xml'.
      String file = XMLParser.toXMLString(beans);
      String filename = projectName.toLowerCase().replace(' ', '-');
      web.createWebResource(file.toCharArray(), "WEB-INF/" + filename + "-mvc-context.xml");
      
      // Update the applicationContext.xml file to scan for DAO implementations.
      FileResource<?> applicationContext = resources.getResource("META-INF/applicationContext.xml");
      beans = XMLParser.parse(applicationContext.getResourceInputStream());
      beans.attribute("xmlns:context", "http://www.springframework.org/schema/context");
      
      // Use a <context:component-scan> to create beans for all DAO interface implementations, annotated as @Repository
      Node componentScan = new Node("context:component-scan", beans);
      componentScan.attribute("base-package", meta.getTopLevelPackage() + ".repo");
      
      // Include the spring-context schema file, so that the <context> namespace can be used in web.xml.
      schemaLoc = beans.getAttribute("xsi:schemaLocation");
      schemaLoc += " http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd";
      beans.attribute("xsi:schemaLocation", schemaLoc);
      
      // Save the updated applicationContext.xml file to 'src/main/resources/META-INF/applicationContext.xml'.
      file = XMLParser.toXMLString(beans);
      resources.createResource(file.toCharArray(), "META-INF/applicationContext.xml");
      
      // Retrieve the WEB-INF/web.xml file to be edited.      
      FileResource<?> webXML = web.getWebResource("WEB-INF/web.xml");
      Node webapp = XMLParser.parse(webXML.getResourceInputStream());
      
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
      
      // Map the servlet to the '/' URL.
      Node servletMapping = new Node("servlet-mapping", webapp);
      Node servletNameRepeat = new Node("servlet-name", servletMapping);
      servletNameRepeat.text(projectName.replace(' ', (char) 0));
      Node url = new Node("url-pattern", servletMapping);
      url.text('/');
      
      // Save the updated web.xml file to 'src/main/webapp/WEB-INF/web.xml'.
      file = XMLParser.toXMLString(webapp);
      web.createWebResource(file.toCharArray(), "WEB-INF/web.xml");
  }
  
  /**
   * The 'mvc-from-entity' command creates a web controller from an entity.
   * When the application is deployed on the selected container, these controllers will provide a web view of the selected entity.
   * @throws FileNotFoundException 
   */
  @Command("mvc-from-entity")
  public void generateFromEntity(PipeOut out, 
          @Option(required = false) JavaResource[] targets,
          @Option(flagOnly = true, name = "overwrite") boolean overwrite)
          throws FileNotFoundException
  {
      JavaSourceFacet java = this.project.getFacet(JavaSourceFacet.class);
      MetadataFacet meta = this.project.getFacet(MetadataFacet.class);
      WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
      
      // Retrieve the base package that will be scanned for controllers.
      String projectName = meta.getProjectName();
      String filePath = "WEB-INF/" + projectName.replace(' ', '-') + "-mvc-context.xml";
      Resource<?> mvcContext = web.getWebResource(filePath);
      Node beans = XMLParser.parse(mvcContext.getResourceInputStream());
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
      
      // Compile the templates for DAO and controller generation as well as for web views.
      loadTemplates();
      
      // For each @Entity that is detected, create a Spring MVC controller.
      for(JavaResource jr : javaTargets) {
          JavaClass entity = (JavaClass) ((JavaResource) jr).getJavaSource();
          
          // Call to a function which creates a DAO interface and implementation for the given entity.
          String daoPackage = meta.getTopLevelPackage() + ".repo";
          generateDao(entity, daoPackage);
          
          // Call to a function which creates the Spring MVC controller from the given entity.
          JavaClass controller = generateController(entity, mvcPackage, daoPackage);
          Resource<?> resource = java.getJavaResource(controller);
          
          if(!resource.exists()) {
              java.saveJavaSource(controller);
              ShellMessages.success(out, "Generated Spring MVC Controller for [" + entity.getQualifiedName() + "]");
          }
      }
      
  }
  
  /**
   * Helper function for the 'mvc-from-entity' command, takes in an array of target Resource objects,
   * and returns a list of JavaResource objects which are annotated with @Entity, and should have 
   * a controller and DAO objects created for them.
   * @throws FileNotFoundException
   */  
  public List<JavaResource> selectTargets(PipeOut out, Resource<?>[] targets)
          throws FileNotFoundException
  {
      List<JavaResource> results = new ArrayList<JavaResource>();
      
      if(targets == null) {
          targets = new Resource<?>[] {};
      }
      
      /*
       * Loop through the resource list, 'targets', passed to the function, and check for any
       * JavaResources (i.e. classes) that have the @Entity annotation.
       */
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

  /**
   * Helper function for the 'mvc-from-entity' command.  Compiles the .jsp and .java templates which are used to generate
   * controllers, data-access objects, and web views for the entity/entities which the command is executed on.
   */
  public void loadTemplates()
  {
      // Compile the DAO interface Java template.
      if(this.daoInterfaceTemplate == null) {
          daoInterfaceTemplate = compiler.compile(DAO_INTERFACE_TEMPLATE);
      }
      
      // Compile the DAO interface implementation Java template.
      if(this.daoImplentationTemplate == null) {
          daoImplentationTemplate = compiler.compile(DAO_IMPLEMENTATION_TEMPLATE);
      }
      
      // Compile the Spring MVC controller Java template.
      if(this.springControllerTemplate == null) {
          springControllerTemplate = compiler.compile(SPRING_CONTROLLER_TEMPLATE);
      }
      
      // Compile the JSP template for the create view.
      if(this.createTemplate == null) {
          createTemplate = compiler.compile(CREATE_TEMPLATE);
      }
      
      // Compile the JSP template for the list view.
      if(this.listTemplate == null) {
          listTemplate = compiler.compile(LIST_TEMPLATE);
      }
      
      // Compile the JSP template for the single entity view.
      if(this.viewTemplate == null) {
          viewTemplate = compiler.compile(VIEW_TEMPLATE);
      }
      
      // Compile the MVEL 2.0 test template
      if(this.propertiesTemplate == null) {
          propertiesTemplate = compiler.compile(PROPERTIES_TEMPLATE);
      }
      
      return;
  }
  
  /**
   * Helper function for the 'mvc-from-entity' command.  Given a JavaClass, annotated with @Entity, and a
   * target package, this function generates a DAO interface and its implementation from template.  The resulting
   * interface and its implementation can be found in 'daoPacakge'.
   * 
   * NOTE: The templates for the DAO objects can be found in src/main/resources/org/jboss/forge/plugins/spring/repo
   *@throws FileNotFoundException
   */
  public void generateDao(JavaClass entity, String daoPackage) throws FileNotFoundException
  {
      JavaSourceFacet java = this.project.getFacet(JavaSourceFacet.class);
      
      // Pass the entity itself and the target package to the templates via a HashMap object.
      Map<Object, Object> context = new HashMap<Object, Object>();
      context.put("entity", entity);
      context.put("daoPackage", daoPackage);
      
      // Create the DAO interface and its implementation from the specified templates.
      JavaInterface daoInterface = JavaParser.parse(JavaInterface.class, this.daoInterfaceTemplate.render(context));
      JavaClassImpl daoImpl = JavaParser.parse(JavaClassImpl.class, this.daoImplentationTemplate.render(context));

      // Save the created interface and class implementation, so they can be referenced by the controller.
      java.saveJavaSource(daoInterface);
      java.saveJavaSource(daoImpl);
      
      return;
  }
  
  /**
   * Helper function for the 'mvc-from-entity' command.  Given a JavaClass, annotated with
   * @Entity, a target package, mvcPackage, and a package containing the entity's DAO, daoPackage,
   * this function creates a Spring MVC controller from template.  The resulting controller can be
   * found in 'mvcPackage'.
   * 
   * NOTE: The template for the controller can be found in src/main/resources/org/jboss/forge/plugins/spring/mvc
   */
  public JavaClass generateController(JavaClass entity, String mvcPackage, String daoPackage)
  {      
      // Pass the entity, the target package, and the entity's DAO package to the template via a HashMap.
      Map<Object, Object> context = new HashMap<Object, Object>();
      context.put("entity", entity);
      context.put("mvcPackage", mvcPackage);
      context.put("daoPackage", daoPackage);
      context.put("entityPlural", pluralOf(entity.getName().toLowerCase()));
      String ccEntity = entity.getName().substring(0, 1).toLowerCase() + entity.getName().substring(1);
      context.put("ccEntity", ccEntity);
      
      // Create a Spring MVC controller for the entity from the template.
      JavaClass entityController = JavaParser.parse(JavaClass.class, this.springControllerTemplate.render(context));
      
      // Return the controller to the function for the 'mvc-from'entity' command.
      return entityController;
  }
  
}