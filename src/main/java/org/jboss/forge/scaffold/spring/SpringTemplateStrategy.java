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

package org.jboss.forge.scaffold.spring;

import java.util.Arrays;

import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.scaffold.TemplateStrategy;
import org.jboss.forge.shell.util.ResourceUtil;
import org.jboss.forge.shell.util.Streams;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class SpringTemplateStrategy implements TemplateStrategy {

    //
    // Private statics
    //

    private static final String FORGE_SCAFFOLD_TEMPLATE = "/resources/scaffold/page.xhtml";

    //
    // Private members
    //

    private final Project project;
    
    //
    // Constructor
    //

    public SpringTemplateStrategy(Project project)
    {
        this.project = project;
    }

    //
    // Public methods
    //

    @Override
    public boolean compatibleWith(Resource<?> template)
    {
        String contents = Streams.toString(template.getResourceInputStream());

        for(String section : Arrays.asList("main")) {
            
            // Does this regex apply for Spring/JSP as well as JSF?

            if (!contents.matches(".*:\\s*insert\\s+name\\s*=\\s*\"" + section + "\".*"))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getReferencePath(Resource<?> template)
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

        for(DirectoryResource dir : web.getWebRootDirectories()) {
            if(ResourceUtil.isChildOf(dir, template))
            {
                String relativePath = template.getFullyQualifiedName().substring(dir.getFullyQualifiedName().length());
                return relativePath;
            }
        }

        throw new IllegalArgumentException("Not a valid template resource for this scaffold.");
    }

    @Override
    public Resource<?> getDefaultTemplate()
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
        return web.getWebResource(FORGE_SCAFFOLD_TEMPLATE);
    }

}
