/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.scaffold.spring.AbstractSpringScaffoldTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

@RunWith(Arquillian.class)
public class SpringScaffoldTest extends AbstractSpringScaffoldTest
{
    @Test
    public void testSetupScaffold() throws Exception
    {
        Project project = setupScaffoldProject();

        MetadataFacet meta = project.getFacet(MetadataFacet.class);
        ResourceFacet resources = project.getFacet(ResourceFacet.class);
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        Assert.assertTrue(project.hasFacet(SpringScaffold.class));

        // Add tests for index.jsp and error.jsp later, when implementation is complete.

        FileResource<?> applicationContext = resources.getResource("META-INF/spring/applicationContext.xml");
        Assert.assertTrue(applicationContext.exists());

        Node beans = XMLParser.parse(applicationContext.getResourceInputStream());
        Assert.assertTrue(beans.getChildren().size() == 1);
        Assert.assertNotNull(beans.get("context:component-scan"));
        Assert.assertEquals(meta.getTopLevelPackage() + ".repo", beans.get("context:component-scan").get(0).getAttribute("base-package"));

        FileResource<?> webXML = web.getWebResource("WEB-INF/web.xml");
        Assert.assertTrue(webXML.exists());

        Node webapp = XMLParser.parse(webXML.getResourceInputStream());
        Assert.assertTrue(webapp.getChildren().size() == 5);
        Assert.assertEquals("WEB-INF/views/error.jsp", webapp.get("error-page").get(0).get("location").get(0).getText());
        Assert.assertEquals("classpath:/META-INF/spring/applicationContext.xml", webapp.get("display").get(0)
                .get("context-param").get(0).get("param-name").get(0).get("param-value").get(0).getText());

        FileResource<?> mvcContext = web.getWebResource("WEB-INF/" + meta.getProjectName().toLowerCase().replace(' ', '-') + "-mvc-context.xml");
        Assert.assertTrue(mvcContext.exists());

        beans = XMLParser.parse(mvcContext.getResourceInputStream());
        Assert.assertEquals(meta.getTopLevelPackage() + ".mvc", beans.get("context:component-scan").get(0).getAttribute("base-package"));
        Assert.assertFalse(beans.get("mvc:annotation-driven").isEmpty());
        Assert.assertFalse(beans.get("mvc:default-servlet-handler").isEmpty());
        Assert.assertEquals(2, beans.get("bean").size());
    }

}
