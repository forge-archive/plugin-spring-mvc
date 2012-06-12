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

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.PackagingFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.spec.spring.mvc.SpringFacet;
import org.jboss.seam.render.TemplateCompiler;
import org.jboss.seam.render.spi.TemplateResolver;
import org.jboss.seam.render.template.CompiledTemplateResource;
import org.jboss.seam.render.template.resolver.ClassLoaderTemplateResolver;

/**
 *  @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

@Alias("spring")
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

   @Command
   public void mvc()
   {
       if (prompt.promptBoolean("Do you also want to generate Spring application context files from default templates?"))
       {
           generateContextFiles();
           return;
       }
       
   }

   protected void generateContextFiles()
   {
       loadTemplates();

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
}
