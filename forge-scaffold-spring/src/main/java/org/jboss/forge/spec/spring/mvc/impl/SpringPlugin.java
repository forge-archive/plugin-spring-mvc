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

   private TemplateResolver<ClassLoader> resolver;

   private String APPLICATION_CONTEXT_TEMPLATE = "org/jboss/forge/applicationContext.xl";
   private String MVC_CONTEXT_TEMPLATE = "org/jboss/forge/mvc-context.xl";
   private String WEB_XML_TEMPLATE = "org/jboss/forge/web.xl";

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
   public void setup(PipeOut out)
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
       }
   }

   @Command("mvc-from-template")
   public void generateMVCFromTemplate(@Option(required=false, defaultValue="Y", description="Overwrite existing files?", name="overwrite") boolean overwrite,
                   @Option(required=false, name="MVC Package") String mvcPackage,
                   @Option(required=false, name="DAO Package") String repoPackage,
                   @Option(required=false, name="Target Directory") String targetDir)
   {
       Map<Object, Object> context = CollectionUtils.newHashMap();
       MetadataFacet meta = project.getFacet(MetadataFacet.class);

       if (mvcPackage == null)
       {
           mvcPackage = meta.getTopLevelPackage() + ".mvc.root";
       }
       if(repoPackage == null)
       {
           repoPackage = meta.getTopLevelPackage() + ".repo";
       }
       if (targetDir == null)
       {
           targetDir = new String();
       }

       context.put("mvcPackage", mvcPackage);
       context.put("repoPackage", repoPackage);
       context.put("targetDir", targetDir);

       generateContextFiles(overwrite, context);
   }

   @Command("mvc")
   public void updateMVC( @Option(required=false, name="MVC Package") String mvcPackage,
                   @Option(required=false, name="DAO Package") String repoPackage,
                   @Option(required=false, name="Target Directory") String targetDir)
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

       if (repoPackage == null)
       {
           repoPackage = meta.getTopLevelPackage() + ".repo";
       }

       updateWebXML(targetDir);
   }

   protected void generateContextFiles(boolean overwrite, Map<Object, Object> context)
   {
       MetadataFacet meta = project.getFacet(MetadataFacet.class);
       PersistenceFacet persistence = project.getFacet(PersistenceFacet.class);
       ResourceFacet resources = project.getFacet(ResourceFacet.class);
       WebResourceFacet web = project.getFacet(WebResourceFacet.class);

       String filename = "WEB-INF/" + meta.getProjectName().replace(' ', '-') + "-mvc-context.xml";
       loadTemplates();

       context.put("projectName", meta.getProjectName());
       context.put("persistenceUnit", persistence.getConfig().listUnits().get(0).getName());

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

   protected void updateWebXML(String targetDir)
   {
       MetadataFacet meta = project.getFacet(MetadataFacet.class);
       ServletFacet servlet = project.getFacet(ServletFacet.class);
       SpringFacet spring = project.getFacet(SpringFacet.class);

       WebAppDescriptor webXML = servlet.getConfig();

       // If the application already has a name, prompt the user to change it to the project name

       if (webXML.getDisplayName() != null)
       {
           if (this.prompt.promptBoolean("Change the application's display name to " + meta.getProjectName() + "?"))
           {
               webXML.displayName(meta.getProjectName());
           }
       }
       else
       {
           webXML.displayName(meta.getProjectName());
       }

       // Add the application context file to web.xml's <context-param>

       if (webXML.getContextParam("contextConfigLocation") == null)
       {
           webXML.contextParam("contextConfigLocation", "classpath:/" + spring.getContextFileLocation());
       }
       else
       {
           String contextConfigLocation = webXML.getContextParam("contextConfigLocation");

           if (!contextConfigLocation.contains(spring.getContextFileLocation()))
           {
               contextConfigLocation += ", classpath:/" + spring.getContextFileLocation();
               webXML.contextParam("contextConfigLocation", contextConfigLocation);
           }
       }

       if (!webXML.getListeners().contains("org.springframework.web.context.ContextLoaderListener"))
       {
           webXML.listener("org.springframework.web.context.ContextLoaderListener");
       }

       webXML = addPersistenceContextRef(webXML);
       webXML = addOpenEntityManagerInViewFilter(webXML);
       servlet.saveConfig(webXML);

       if (targetDir.equals("/") || targetDir.isEmpty())
       {
           spring.addRootServlet();
       }
       else
       {
           spring.addServlet(targetDir);
       }
   }

   private String processTargetDir(String targetDir)
   {
       targetDir = (targetDir.startsWith("/")) ? targetDir.substring(1) : targetDir;
       targetDir = (targetDir.endsWith("/")) ? targetDir.substring(0, targetDir.length()-1) : targetDir;

       return targetDir;
   }

   private WebAppDescriptor addPersistenceContextRef(WebAppDescriptor webXML)
   {
       Node webapp = XMLParser.parse(webXML.toString());

       if (webapp.get("persistence-context-ref") == null)
       {
           PersistenceFacet persistence = project.getFacet(PersistenceFacet.class);
           ServletFacet servlet = project.getFacet(ServletFacet.class);
           WebResourceFacet web = project.getFacet(WebResourceFacet.class);

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
