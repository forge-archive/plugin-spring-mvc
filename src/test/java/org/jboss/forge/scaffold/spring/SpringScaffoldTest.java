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
import org.jboss.forge.shell.exceptions.PluginExecutionException;
import org.jboss.forge.shell.util.Streams;
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
        Assert.assertEquals(4, beans.getChildren().size());
        Assert.assertNotNull(beans.get("context:component-scan"));
        Assert.assertEquals(meta.getTopLevelPackage() + ".repo", beans.get("context:component-scan").get(0).getAttribute("base-package"));

        FileResource<?> webXML = web.getWebResource("WEB-INF/web.xml");
        Assert.assertTrue(webXML.exists());

        Node webapp = XMLParser.parse(webXML.getResourceInputStream());
        Assert.assertTrue(webapp.getChildren().size() == 6);
        // Comment out as error page location has been commented out for debugging.
        // Assert.assertEquals("WEB-INF/views/error.jsp", webapp.get("error-page").get(0).get("location").get(0).getText());
        Assert.assertEquals("contextConfigLocation", webapp.get("context-param").get(0)
                .get("param-name").get(0).getText());
        Assert.assertEquals("classpath:/META-INF/spring/applicationContext.xml", webapp.get("context-param").get(0)
                .get("param-value").get(0).getText());

        FileResource<?> mvcContext = web.getWebResource("WEB-INF/" + meta.getProjectName().toLowerCase().replace(' ', '-') + "-mvc-context.xml");
        Assert.assertTrue(mvcContext.exists());

        beans = XMLParser.parse(mvcContext.getResourceInputStream());
        Assert.assertEquals(meta.getTopLevelPackage() + ".mvc", beans.get("context:component-scan").get(0).getAttribute("base-package"));
        Assert.assertFalse(beans.get("mvc:annotation-driven").isEmpty());
        Assert.assertFalse(beans.get("mvc:default-servlet-handler").isEmpty());
        // Adjust for commenting out of error page view resolver, should be 2.
        Assert.assertEquals(1, beans.get("bean").size());
    }

    @Test
    public void testScaffoldSetupWithScaffoldTypeandTargetDir() throws Exception
    {
        Project project = setupScaffoldProject();
        queueInputLines("", "", "2", "", "", "");
        getShell().execute("scaffold setup --scaffoldType spring --targetDir WEB-INF/views");
        Assert.assertTrue(project.hasFacet(SpringScaffold.class));
    }

    @Test(expected = PluginExecutionException.class)
    public void testCannotGenerateFromEntityUntilScaffoldInstalled() throws Exception
    {
        initializeJavaProject();

        getShell().execute("spring setup");

        queueInputLines("");
        getShell().execute("persistence setup --provider HIBERNATE --container JBOSS_AS7");

        getShell().execute("spring persistence");

        queueInputLines("");
        getShell().execute("entity --named Customer");

        queueInputLines("", "");
        getShell().execute("scaffold from-entity --scaffoldType spring");
    }

    @Test
    public void testGenerateFromEntity() throws Exception
    {
        Project project = setupScaffoldProject();

        queueInputLines("");
        getShell().execute("entity --named Customer");
        getShell().execute("field string --named firstName");
        getShell().execute("field string --named lastName");

        queueInputLines("", "");
        getShell().execute("scaffold from-entity --targetDir WEB-INF/views --scaffoldType spring");

        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        // View

        FileResource<?> view = web.getWebResource("WEB-INF/views/viewCustomer.jsp");
        Assert.assertTrue(view.exists());
        String contents = Streams.toString(view.getResourceInputStream());

        StringBuilder metawidget = new StringBuilder();
        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");
        metawidget.append("<html>\n\n");
        metawidget.append("\t<head>\n\t\t<title>View Customer</title>\n\t</head>\n\n");
        metawidget.append("\t<form:form commandName=\"customer\">\n\r\n");
        metawidget.append("\t<table>\r\n\t<tbody>\r\n\t\t<tr>\r\n\t\t\t<th/>\r\n\t\t\t");
        metawidget.append("<td>\r\n\t\t\t\t<form:hidden path=\"id\"/>\r\n\t\t\t</td>\r\n\t\t\t<td>*</td>\r\n");
        metawidget.append("\t\t</tr>\r\n\t\t<tr>\r\n\t\t\t<th/>\r\n\t\t\t");
        metawidget.append("<td>\r\n\t\t\t\t<form:hidden path=\"version\"/>\r\n\t\t\t</td>\r\n\t\t\t<td/>\r\n");
        metawidget.append("\t\t</tr>\r\n\t\t<tr>\r\n\t\t\t<th>\r\n\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>\r\n");
        metawidget.append("\t\t\t</th>\r\n\t\t\t<td>\r\n\t\t\t\t<c:out value=\"${customer.firstName}\"/>\r\n\t\t\t</td>\r\n\t\t\t<td/>\r\n");
        metawidget.append("\t\t</tr>\r\n\t\t<tr>\r\n\t\t\t<th>\r\n\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>\r\n");
        metawidget.append("\t\t\t</th>\r\n\t\t\t<td>\r\n\t\t\t\t<c:out value=\"${customer.lastName}\"/>\r\n\t\t\t</td>\r\n\t\t\t<td/>\r\n");
        metawidget.append("\t\t</tr>\r\n\t</tbody>\r\n\t</table>\n");
        metawidget.append("\t</form:form>\n\t\n\t<div class=\"links\">\n\t\n\t</div>\n\n</html>");
        
        Assert.assertEquals(metawidget.toString(), contents.toString());

        // Create

        FileResource<?> create = web.getWebResource("WEB-INF/views/createCustomer.jsp");
        Assert.assertTrue(create.exists());
        contents = Streams.toString(create.getResourceInputStream());

        metawidget = new StringBuilder();
        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");
        metawidget.append("<html>\n\n");
        metawidget.append("\t<head>\n\t\t<title>Create a new Customer</title>\n\t</head>\n\t\n");
        metawidget.append("\t<form:form commandName=\"customer\">\n\t\n");
        metawidget.append("\t\t<table>\r\n\t\t\t<tbody>\r\n\t\t\t\t<tr>\r\n\t\t\t\t\t<th/>\r\n\t\t\t\t\t");
        metawidget.append("<td>\r\n\t\t\t\t\t\t<form:hidden path=\"id\"/>\r\n\t\t\t\t\t</td>\r\n\t\t\t\t\t<td>*</td>\r\n");
        metawidget.append("\t\t\t\t</tr>\r\n\t\t\t\t<tr>\r\n\t\t\t\t\t<th/>\r\n\t\t\t\t\t");
        metawidget.append("<td>\r\n\t\t\t\t\t\t<form:hidden path=\"version\"/>\r\n\t\t\t\t\t</td>\r\n\t\t\t\t\t<td/>\r\n");
        metawidget.append("\t\t\t\t</tr>\r\n\t\t\t\t<tr>\r\n\t\t\t\t\t<th>\r\n\t\t\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>\r\n");
        metawidget.append("\t\t\t\t\t</th>\r\n\t\t\t\t\t<td>\r\n\t\t\t\t\t\t<form:input path=\"firstName\"/>\r\n\t\t\t\t\t</td>\r\n\t\t\t\t\t<td/>\r\n");
        metawidget.append("\t\t\t\t</tr>\r\n\t\t\t\t<tr>\r\n\t\t\t\t\t<th>\r\n\t\t\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>\r\n");
        metawidget.append("\t\t\t\t\t</th>\r\n\t\t\t\t\t<td>\r\n\t\t\t\t\t\t<form:input path=\"lastName\"/>\r\n\t\t\t\t\t</td>\r\n\t\t\t\t\t<td/>\r\n");
        metawidget.append("\t\t\t\t</tr>\r\n\t\t\t</tbody>\r\n\t\t</table>\n\n\t\t<input type=\"submit\" value=\"Create New Customer\"/>\t\t\n");
        metawidget.append("\t</form:form>\n\t\n</html>");

        Assert.assertEquals(metawidget.toString(), contents.toString());

        // Search

/*        FileResource<?> search = web.getWebResource("WEB-INF/views/searchCustomer.jsp");
        Assert.assertTrue(search.exists());
        contents = Streams.toString(search.getResourceInputStream());*/        
    }
}
