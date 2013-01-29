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
package org.jboss.forge.spec.spring.mvc.impl;

import java.util.Map;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.PackagingFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.scaffold.util.ScaffoldUtil;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.spec.javaee.PersistenceFacet;
import org.jboss.forge.spec.javaee.ServletFacet;
import org.jboss.forge.spec.spring.mvc.SpringFacet;
import org.jboss.seam.render.TemplateCompiler;
import org.jboss.seam.render.spi.TemplateResolver;
import org.jboss.seam.render.template.CompiledTemplateResource;
import org.jboss.seam.render.template.resolver.ClassLoaderTemplateResolver;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.FilterDef;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.FilterMappingDef;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.WebAppDescriptor;
import org.metawidget.util.CollectionUtils;

/**
 *  @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

@Alias("spring")
@RequiresFacet({SpringFacet.class,
                PersistenceFacet.class,
                WebResourceFacet.class})
public class SpringPlugin implements Plugin
{
   @Inject
   private Project project;

   @Inject
   private Event<InstallFacets> request;

   @Inject
   private TemplateCompiler compiler;

   @Inject
   private ShellPrompt prompt;

   private static final String XMLNS_PREFIX = "xmlns:";

   private TemplateResolver<ClassLoader> resolver;

   private String APPLICATION_CONTEXT_TEMPLATE = "scaffold/spring/applicationContext.xl";
   private String MVC_CONTEXT_TEMPLATE = "scaffold/spring/mvc-context.xl";
   private String WEB_XML_TEMPLATE = "scaffold/spring/web.xl";

   private CompiledTemplateResource applicationContextTemplate;
   private CompiledTemplateResource mvcContextTemplate;
   private CompiledTemplateResource webXmlTemplate;

   @Inject
   public SpringPlugin(final TemplateCompiler compiler)
   {
       this.compiler = compiler;
       this.resolver = new ClassLoaderTemplateResolver(SpringPlugin.class.getClassLoader());

       if (this.compiler != null)
       {
           this.compiler.getTemplateResolverFactory().addResolver(this.resolver);
       }
   }

   @SetupCommand
   public void setup(PipeOut out, @Option(required=false, name="location", defaultValue="META-INF/spring/applicationContext.xml",
                       description="Location of the application context XML file.") String location)
   {
       if (!project.hasFacet(SpringFacet.class))
       {
           request.fire(new InstallFacets(SpringFacet.class));
       }

       PackagingFacet packaging = project.getFacet(PackagingFacet.class);

       if (packaging.getPackagingType() != PackagingType.WAR)
       {
           if (this.prompt.promptBoolean("Facet [forge.maven.WebResourceFacet] requires packaging type(s) [war], but is currently [" +
                   packaging.getPackagingType().toString() + "]. Update packaging? (Note: this could deactivate other plugins in your project.)"))
           {
               packaging.setPackagingType(PackagingType.WAR);
               this.request.fire(new InstallFacets(WebResourceFacet.class));
           }
       }

       if (project.hasFacet(SpringFacet.class))
       {
           ShellMessages.success(out, "Spring MVC dependencies are installed.");
           SpringFacet spring = project.getFacet(SpringFacet.class);

           location = (location.startsWith("/")) ? location.substring(1) : location;

           if (spring.setContextFileLocation(location))
           {
               ShellMessages.success(out, "Application context file located at: src/main/resources/" + location);
           }
           else
           {
               ShellMessages.error(out, "Could not change application context location, no file found at src/main/resources/" + location);
           }
       }
   }

   @Command("context-location")
   public void setApplicationContextLocation(@Option(required=true, name="location",
                   description="Location of Application Context XML file") String location, final PipeOut out)
   {
      SpringFacet spring = project.getFacet(SpringFacet.class);

      if (spring.setContextFileLocation(location))
      {
          ShellMessages.success(out, "Application context file location changed to: src/main/resources/" + location);
      }
      else
      {
          ShellMessages.error(out, "No file found at: src/main/resources/" + location);
      }
   }

   @Command("persistence")
   public void generateApplicationContext(@Option(required=false, name="--daoPackage", description="DAO/Repository Package") String repoPackage)
   {
       MetadataFacet meta = project.getFacet(MetadataFacet.class);
       ResourceFacet resources = project.getFacet(ResourceFacet.class);
       SpringFacet spring = project.getFacet(SpringFacet.class);

       Node beans;

       if (!resources.getResource(spring.getContextFileLocation()).exists())
       {
           beans = new Node("beans");
       }
       else
       {
           beans = XMLParser.parse(resources.getResource(spring.getContextFileLocation()).getResourceInputStream());
       }

       addXMLSchema(beans, true);

       if (repoPackage == null)
       {
           repoPackage = meta.getTopLevelPackage() + ".repo";
       }

       if (!hasContextComponentScan(beans, repoPackage))
       {
           addContextComponentScan(beans, repoPackage);
       }

       if (!hasChild(beans, "entityManager"))
       {
           Node entityManager = new Node("bean", beans);
           entityManager.attribute("id", "entityManager");
           entityManager.attribute("class", "org.springframework.orm.jpa.support.SharedEntityManagerBean");
           entityManager.createChild("property").attribute("name", "entityManagerFactory")
                       .attribute("ref", "entityManagerFactory");
       }

       if (beans.getSingle("tx:annotation-driven") == null)
       {
           beans.createChild("tx:annotation-driven");
       }

       if (beans.getSingle("tx:jta-transaction-manager") == null)
       {
           beans.createChild("tx:jta-transaction-manager");
       }

       if (!hasChild(beans, "entityManagerFactory"))
       {
           PersistenceFacet persistence = project.getFacet(PersistenceFacet.class);
           String unitName = persistence.getConfig().listUnits().get(0).getName();

           beans.createChild("jee:jndi-lookup").attribute("expected-type", "javax.persistence.EntityManagerFactory")
                       .attribute("id", "entityManagerFactory")
                       .attribute("jndi-name", "java:jboss/" + unitName + "/persistence");
       }

       resources.createResource(XMLParser.toXMLString(beans).toCharArray(), spring.getContextFileLocation());
   }

   @Command("mvc-from-template")
   public void generateMVCFromTemplate(@Option(required=false, defaultValue="true", name="overwrite") boolean overwrite,
                   @Option(required=false, name="mvcPackage", description="MVC Controller Package") String mvcPackage,
                   @Option(required=false, name="mvcContext", description="MVC Context File Location") String mvcContext,
                   @Option(required=false, name="daoPackage", description="DAO/Repository Package") String repoPackage,
                   @Option(required=false, name="targetDir", description="Target Directory") String targetDir)
   {
       Map<Object, Object> context = CollectionUtils.newHashMap();
       MetadataFacet meta = project.getFacet(MetadataFacet.class);

       if (targetDir == null)
       {
           targetDir = new String();
       }

       targetDir = processTargetDir(targetDir);

       if (mvcPackage == null)
       {
           mvcPackage = meta.getTopLevelPackage() + ".mvc";
           mvcPackage += (targetDir.isEmpty()) ? ".root" : "." + targetDir.replace('/', '.');
       }

       if (mvcContext == null)
       {
           if (targetDir.isEmpty() || targetDir.equals("/"))
           {
               mvcContext = "/WEB-INF/" + meta.getProjectName().replace(' ', '-').toLowerCase() + "-mvc-context.xml";              
           }
           else
           {
               mvcContext = "/WEB-INF/" + targetDir.replace('/', '-').toLowerCase() + "-mvc-context.xml";
           }
       }

       if(repoPackage == null)
       {
           repoPackage = meta.getTopLevelPackage() + ".repo";
       }

       context.put("mvcPackage", mvcPackage);
       context.put("repoPackage", repoPackage);
       context.put("targetDir", targetDir);
       context.put("mvc-context-file", mvcContext);

       generateContextFiles(overwrite, context);
   }

   @Command("mvc")
   public void updateMVC( @Option(required=false, name="mvcPackage", description="MVC Controller Package") String mvcPackage,
                   @Option(required=false, name="mvcContext", description="MVC Context File Location") String mvcContext,
                   @Option(required=false, name="targetDir", description="Target Directory") String targetDir)
   {
       MetadataFacet meta = project.getFacet(MetadataFacet.class);

       if (targetDir == null)
       {
           targetDir = new String();
       }

       targetDir = processTargetDir(targetDir);

       if (mvcPackage == null)
       {
           mvcPackage = meta.getTopLevelPackage() + ".mvc";
           mvcPackage += (targetDir.isEmpty()) ? ".root" : targetDir.replace('/', '.');
       }

       if (mvcContext == null)
       {
           if (targetDir.isEmpty())
           {
               mvcContext = "/WEB-INF/" + meta.getProjectName().replace(' ', '-').toLowerCase() + "-mvc-context.xml";
           }
           else
           {
               mvcContext = "/WEB-INF/" + targetDir.replace('/', '-').toLowerCase() + "-mvc-context.xml";
           }
       }

       updateWebXML(targetDir, mvcContext);
       generateMVCContext(mvcContext, mvcPackage);
   }

   protected void generateContextFiles(boolean overwrite, Map<Object, Object> context)
   {
       MetadataFacet meta = project.getFacet(MetadataFacet.class);
       PersistenceFacet persistence = project.getFacet(PersistenceFacet.class);
       ResourceFacet resources = project.getFacet(ResourceFacet.class);

       WebResourceFacet web = project.getFacet(WebResourceFacet.class);

       String filename = context.get("mvc-context-file").toString();
       loadTemplates();

       context.put("projectName", meta.getProjectName());
       context.put("persistenceUnit", persistence.getConfig().listUnits().get(0).getName());
       context.put("mvcContextFile", filename);

       ScaffoldUtil.createOrOverwrite(this.prompt, resources.getResource("META-INF/spring/applicationContext.xml"),
               applicationContextTemplate.render(context), overwrite);
       ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/web.xml"), webXmlTemplate.render(context), overwrite);
       ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource(filename), mvcContextTemplate.render(context), overwrite);
   }

   protected void loadTemplates()
   {
       if (applicationContextTemplate == null)
       {
           applicationContextTemplate = compiler.compile(APPLICATION_CONTEXT_TEMPLATE);
       }

       if (mvcContextTemplate == null)
       {
           mvcContextTemplate = compiler.compile(MVC_CONTEXT_TEMPLATE);
       }

       if (webXmlTemplate == null)
       {
           webXmlTemplate = compiler.compile(WEB_XML_TEMPLATE);
       }
   }

   protected void generateMVCContext(String mvcContextFilename, String mvcPackage)
   {
       WebResourceFacet web = project.getFacet(WebResourceFacet.class);

       Node beans;

       if (!web.getWebResource(mvcContextFilename).exists())
       {
           beans = new Node("beans");
       }
       else
       {
           beans = XMLParser.parse(web.getWebResource(mvcContextFilename).getResourceInputStream());
       }

       beans = addXMLSchema(beans, false);
       beans = addContextComponentScan(beans, mvcPackage);

       // Add view resolver, default to Apache Tiles2

       if (!hasChild(beans, "viewResolver"))
       {
           Node viewResolver = new Node("bean", beans);
           viewResolver.attribute("id", "viewResolver");
           viewResolver.attribute("class", "org.springframework.web.servlet.view.tiles2.TilesViewResolver");

           viewResolver.createChild("property").attribute("name", "viewClass")
                       .attribute("value", "org.springframework.web.servlet.view.tiles2.TilesView");

           if (!hasChild(beans, "tilesConfigurer"))
           {
               Node tilesConfigurer = new Node("bean", beans);
               tilesConfigurer.attribute("id", "tilesConfigurer");
               tilesConfigurer.attribute("class", "org.springframework.web.servlet.view.tiles2.TilesConfigurer");

               Node definitions = new Node("property", tilesConfigurer);
               definitions.attribute("name", "definitions");

               Node list = new Node("list", definitions);
               list.createChild("value").text("/WEB-INF/**/layouts.xml");
               list.createChild("value").text("/WEB-INF/**/views.xml");
           }
       }

       if (!hasChild(beans, "errorViewResolver"))
       {
           Node errorViewResolver = new Node("bean", beans);
           errorViewResolver.attribute("id", "errorViewResolver");
           errorViewResolver.attribute("class", "org.springframework.web.servlet.handler.SimpleMappingExceptionHandler");

           Node exceptionMappings = new Node("property", errorViewResolver);
           exceptionMappings.createChild("props").createChild("prop").text("error").attribute("key", "java.lang.Exception");
       }

       if (beans.getSingle("mvc:annotation-driven") == null)
       {
           beans.createChild("mvc:annotation-driven");
       }

       if (beans.getSingle("mvc:resources") == null)
       {
           beans.createChild("mvc:resources").attribute("location", "/").attribute("mapping", "/static/**");
       }

       // Only add <mvc:default-servlet-handler/> to servlets mapped to '/'

       if (beans.getSingle("mvc:default-servlet-handler") == null)
       {
           beans.createChild("mvc:default-servlet-handler");
       }

       web.createWebResource(XMLParser.toXMLString(beans), mvcContextFilename);
   }

   protected void updateWebXML(String mvcContext, String targetDir)
   {
       MetadataFacet meta = project.getFacet(MetadataFacet.class);
       ServletFacet servlet = project.getFacet(ServletFacet.class);
       SpringFacet spring = project.getFacet(SpringFacet.class);

       WebAppDescriptor webXML = servlet.getConfig();

       if (webXML == null)
       {
           Node webapp = new Node("webapp");
           WebResourceFacet web = project.getFacet(WebResourceFacet.class);
           web.createWebResource(XMLParser.toXMLString(webapp), "WEB-INF/web.xml");
           webXML = servlet.getConfig();
       }

       // If the application already has a name, prompt the user to change it to the project name

       if (webXML.getDisplayName() != null)
       {
           if (this.prompt.promptBoolean("Change the application's display name to " + meta.getProjectName() + "?"))
           {
               webXML = webXML.displayName(meta.getProjectName());
           }
       }
       else
       {
           webXML= webXML.displayName(meta.getProjectName());
       }

       // Add the application context file to web.xml's <context-param>

       if (webXML.getContextParam("contextConfigLocation") == null)
       {
           webXML = webXML.contextParam("contextConfigLocation", "classpath:/" + spring.getContextFileLocation());
       }
       else
       {
           String contextConfigLocation = webXML.getContextParam("contextConfigLocation");

           if (!contextConfigLocation.contains(spring.getContextFileLocation()))
           {
               contextConfigLocation += ", classpath:/" + spring.getContextFileLocation();
               webXML = webXML.contextParam("contextConfigLocation", contextConfigLocation);
           }
       }

       // Add a Spring ContextLoaderListener

       if (!webXML.getListeners().contains("org.springframework.web.context.ContextLoaderListener"))
       {
           webXML = webXML.listener("org.springframework.web.context.ContextLoaderListener");
       }

       // Add a reference to the application's persistence unit as well as an OpenEntityManagerInView bean

       webXML = addPersistenceContextRef(webXML);
       webXML = addOpenEntityManagerInViewFilter(webXML);
       servlet.saveConfig(webXML);

       // Add a servlet corresponding to the specified targetDir

       if (targetDir.equals("/") || targetDir.isEmpty())
       {
           spring.addRootServlet(mvcContext);
       }
       else
       {
           spring.addServlet(targetDir, mvcContext);
       }
   }

   private Node addContextComponentScan(Node beans, String basePackage)
   {
       if (hasContextComponentScan(beans, basePackage))
       {
           return beans;
       }

       Node scan = new Node("context:component-scan", beans);
       scan.attribute("base-package", basePackage);

       return beans;
   }

   private boolean hasContextComponentScan(Node beans, String basePackage)
   {
       for (Node scan : beans.get("context:component-scan"))
       {
           if (scan.getAttribute("base-package").equals(basePackage))
           {
               return true;
           }
       }

       return false;
   }

   private boolean hasChild(Node beans, String id)
   {
       for (Node child : beans.getChildren())
       {
           if (child.getAttribute("id") != null && child.getAttribute("id").equals(id))
           {
               return true;
           }
       }

       return false;
   }

   private String processTargetDir(String targetDir)
   {
       targetDir = (targetDir.startsWith("/")) ? targetDir.substring(1) : targetDir;
       targetDir = (targetDir.endsWith("/")) ? targetDir.substring(0, targetDir.length()-1) : targetDir;

       return targetDir;
   }

   private Node addXMLSchema(Node beans, boolean applicationContext)
   {
       beans.attribute("xmlns", "http://www.springframework.org/schema/beans");
       beans.attribute(XMLNS_PREFIX + "context", "http://www.springframework.org/schema/context");
       beans.attribute(XMLNS_PREFIX + "xsi", "http://www.w3.org/2001/XMLSchema-instance");

       String schemaLocation = beans.getAttribute("xsi:schemaLocation");
       schemaLocation = (schemaLocation == null) ? new String() : schemaLocation;

       if (!schemaLocation.contains("http://www.springframework.org/schema/beans " +
       		"http://www.springframework.org/schema/beans/spring-beans.xsd"))
       {
           schemaLocation += " http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\n";
       }

       if (!schemaLocation.contains("http://www.springframework.org/schema/context " +
       		"http://www.springframework.org/schema/context/spring-context.xsd"))
       {
           schemaLocation += " http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd\n";
       }

       if (!applicationContext)
       {
           beans.attribute(XMLNS_PREFIX + "mvc", "http://www.springframework.org/schema/mvc");

           if (!schemaLocation.contains("http://www.springframework.org/schema/mvc " +
           		"http://www.springframework.org/schema/mvc/spring-mvc.xsd"))
           {
               schemaLocation += " http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd\n";
           }
       }
       else
       {
           beans.attribute(XMLNS_PREFIX + "jee", "http://www.springframework.org/schema/jee");
           beans.attribute(XMLNS_PREFIX + "tx", "http://www.springframework.org/schema/tx");

           if (!schemaLocation.contains("http://www.springframework.org/schema/jee " +
                   "http://www.springframework.org/schema/jee/spring-jee.xsd"))
           {
               schemaLocation += " http://www.springframework.org/schema/jee http://www.springframework.org/schema/jee/spring-jee.xsd\n";
           }

           if (!schemaLocation.contains("http://www.springframework.org/schema/tx " +
                   "http://www.springframework.org/schema/tx/spring-tx.xsd"))
           {
               schemaLocation += " http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx.xsd\n";
           }
       }

       beans.attribute("xsi:schemaLocation", schemaLocation);

       return beans;
   }

   private WebAppDescriptor addPersistenceContextRef(WebAppDescriptor webXML)
   {
       ServletFacet servlet = project.getFacet(ServletFacet.class);
       WebResourceFacet web = project.getFacet(WebResourceFacet.class);

       servlet.saveConfig(webXML);
       Node webapp = XMLParser.parse(web.getWebResource("WEB-INF/web.xml").getResourceInputStream());

       if (webapp.getSingle("persistence-context-ref") == null)
       {
           PersistenceFacet persistence = project.getFacet(PersistenceFacet.class);

           Node persistenceContextRef = new Node("persistence-context-ref", webapp);
           String unitName = persistence.getConfig().listUnits().get(0).getName();
           persistenceContextRef.createChild("persistence-context-ref-name").text("persistence/" + unitName + "/entityManager");
           persistenceContextRef.createChild("persistence-unit-name").text(unitName);

           web.createWebResource(XMLParser.toXMLString(webapp), "WEB-INF/web.xml");
           return servlet.getConfig();
       }

       return webXML;
   }

   private WebAppDescriptor addOpenEntityManagerInViewFilter(WebAppDescriptor webXML)
   {
       for (FilterDef filter : webXML.getFilters())
       {
           if (filter.getClass().getName().equals("org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter"))
           {
               for (FilterMappingDef mapping : filter.getMappings())
               {
                   if (mapping.getUrlPatterns().contains("/*"))
                   {
                       return webXML;
                   }
               }

               filter.mapping().urlPattern("/*");
           }
       }

       String[] urlPatterns = {"/*"};
       webXML.filter("org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter", urlPatterns);

       return webXML;
   }
}
