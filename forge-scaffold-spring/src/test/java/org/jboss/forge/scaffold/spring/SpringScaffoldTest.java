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
import org.jboss.forge.project.facets.JavaSourceFacet;
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
    private final static String CRLF = "\r\n";

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
        Assert.assertEquals(5, beans.getChildren().size());
        Assert.assertNotNull(beans.get("context:component-scan"));
        Assert.assertEquals(meta.getTopLevelPackage() + ".repo", beans.get("context:component-scan").get(0).getAttribute("base-package"));

        FileResource<?> webXML = web.getWebResource("WEB-INF/web.xml");
        Assert.assertTrue(webXML.exists());

        Node webapp = XMLParser.parse(webXML.getResourceInputStream());
        Assert.assertEquals(9, webapp.getChildren().size());
        Assert.assertEquals("/error", webapp.get("error-page").get(0).get("location").get(0).getText());
        Assert.assertEquals("contextConfigLocation", webapp.get("context-param").get(0)
                .get("param-name").get(0).getText());
        Assert.assertEquals("classpath:/META-INF/spring/applicationContext.xml", webapp.get("context-param").get(0)
                .get("param-value").get(0).getText());

        FileResource<?> mvcContext = web.getWebResource("WEB-INF/" + meta.getProjectName().toLowerCase().replace(' ', '-') + "-mvc-context.xml");
        Assert.assertTrue(mvcContext.exists());

        beans = XMLParser.parse(mvcContext.getResourceInputStream());
        Assert.assertEquals(meta.getTopLevelPackage() + ".mvc.root", beans.get("context:component-scan").get(0).getAttribute("base-package"));
        Assert.assertFalse(beans.get("mvc:annotation-driven").isEmpty());
        Assert.assertFalse(beans.get("mvc:default-servlet-handler").isEmpty());
        Assert.assertEquals(3, beans.get("bean").size());

        FileResource<?> page = web.getWebResource("WEB-INF/layouts/pageTemplate.jsp");
        Assert.assertTrue(page.exists());
        String contents = Streams.toString(page.getResourceInputStream());
        Assert.assertTrue(contents.contains("<div id=\"wrapper\">"));
        Assert.assertTrue(contents.contains("<div id=\"navigation\">"));
        Assert.assertTrue(contents.contains("<div id=\"content\">"));
        Assert.assertTrue(contents.contains("<div id=\"content\">"));
        Assert.assertTrue(contents.contains("<div id=\"footer\">"));
    }

    @Test
    public void testScaffoldSetupWithScaffoldTypeandTargetDir() throws Exception
    {
        Project project = initializeJavaProject();
        queueInputLines("HIBERNATE", "JBOSS_AS7", "", "");
        getShell().execute("persistence setup");
        queueInputLines("", "", "2", "", "", "");
        getShell().execute("scaffold setup --scaffoldType spring --targetDir admin");
        Assert.assertTrue(project.hasFacet(SpringScaffold.class));
    }

    @Test(expected = PluginExecutionException.class)
    public void testCannotGenerateFromEntityUntilScaffoldInstalled() throws Exception
    {
        initializeJavaProject();

        queueInputLines("", "");
        getShell().execute("persistence setup --provider HIBERNATE --container JBOSS_AS7");

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

        queueInputLines("", "", "", "");
        getShell().execute("scaffold from-entity");

        MetadataFacet meta = project.getFacet(MetadataFacet.class);
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        Assert.assertTrue(web.getWebResource("WEB-INF/views/views.xml").exists());
        Assert.assertTrue(web.getWebResource("WEB-INF/layouts/layouts.xml").exists());

        // Create

        FileResource<?> create = web.getWebResource("WEB-INF/views/Customer/createCustomer.jsp");
        Assert.assertTrue(create.exists());
        String contents = Streams.toString(create.getResourceInputStream());
        StringBuilder metawidget = new StringBuilder();

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<div class=\"section\">\n");
        metawidget.append("\t<form:form commandName=\"customer\">\n\n");
        metawidget.append("\t\t<table>")
                .append(CRLF);
        metawidget.append("\t\t\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"id\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"version\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append( "\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:input path=\"firstName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:input path=\"lastName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append( "\t\t\t</tbody>")
                .append(CRLF);
        metawidget.append("\t\t</table>\n\n\t\t<div class=\"buttons\">\n\t\t\t<table>\n\t\t\t\t<tbody>\n\t\t\t\t\t");
        metawidget.append("<tr>\n\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t<input type=\"submit\" value=\"Save\" class=\"button\"/>\n\t\t\t\t\t\t");
        metawidget.append("</td>\n\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t<a class=\"button\" href=\"<c:url value=\"/customers\"/>\">Cancel</a>\n");
        metawidget.append("\t\t\t\t\t\t</td>\n\t\t\t\t\t</tr>\n\t\t\t\t</tbody>\n\t\t\t</table>\n\t\t</div>\n\t</form:form>\n</div>\n");

        Assert.assertEquals(metawidget.toString(), contents.toString());

        // Update

        FileResource<?> update = web.getWebResource("WEB-INF/views/Customer/updateCustomer.jsp");
        Assert.assertTrue(update.exists());
        contents = Streams.toString(update.getResourceInputStream());
        metawidget = new StringBuilder();

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<div class=\"section\">\n");
        metawidget.append("\t<form:form commandName=\"customer\" action=\"${customer.id}\">\n\n");
        metawidget.append("\t\t<table>")
                .append(CRLF);
        metawidget.append("\t\t\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"id\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"version\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append( "\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:input path=\"firstName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:input path=\"lastName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append( "\t\t\t</tbody>")
                .append(CRLF);
        metawidget.append("\t\t</table>\n\n\t\t<input type=\"submit\" value=\"Save\" class=\"button\"/>\n\n\t</form:form>\n\n\t");
        metawidget.append("<div class=\"buttons\">\n\t\t<table>\n\t\t\t<tbody>\n\t\t\t\t");
        metawidget.append("<tr>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<a class=\"button\" href=\"<c:url value=\"/customers\"/>\">Cancel</a>\n\t\t\t\t");
        metawidget.append("\t</td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<form:form commandName=\"customer\" action=\"${customer.id}/delete\"");
        metawidget.append(" method=\"POST\">\n\t\t\t\t\t\t\t<input type=\"submit\" value=\"Delete\" class=\"button\"/>\n");
        metawidget.append("\t\t\t\t\t\t</form:form>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</tbody>\n\t\t</table>\n\t</div>\n</div>\n");

        Assert.assertEquals(metawidget.toString(), contents.toString());

        // Search

        FileResource<?> search = web.getWebResource("WEB-INF/views/Customer/customers.jsp");
        Assert.assertTrue(update.exists());
        contents = Streams.toString(search.getResourceInputStream());
        metawidget = new StringBuilder();

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<div class=\"section\">\n");
        metawidget.append("\t<form:form commandName=\"search\" method=\"GET\">\n");
        metawidget.append("\t\t<table class=\"search\">")
                .append(CRLF);
        metawidget.append("\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:hidden path=\"id\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:hidden path=\"version\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t</th>")
                .append(CRLF);
        metawidget.append( "\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:input path=\"firstName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t</th>")
                .append(CRLF);
        metawidget.append("\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:input path=\"lastName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t</tr>")
                .append(CRLF);
        metawidget.append( "\t</tbody>")
                .append(CRLF);
        metawidget.append("</table>\n\n\t\t<div class=\"buttons\">\n\t\t\t<table>\n\t\t\t\t<tbody>\n\t\t\t\t\t");
        metawidget.append("<tr>\n\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t<input type=\"submit\" value=\"Search\" class=\"button\"/>\n\t\t\t\t\t");
        metawidget.append("\t</td>\n\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t<a class=\"button\" href=\"<c:url value=\"/customers/create\"/>\">");
        metawidget.append("Create New</a>\n\t\t\t\t\t\t</td>\n\t\t\t\t\t</tr>\n\t\t\t\t</tbody>\n\t\t\t</table>\n\t\t</div>\n\t");
        metawidget.append("</form:form>\n\n\t<table class=\"data-table\">\n\t\t<thead>\n\t\t\t<tr>\n\t\t\t\t");
        metawidget.append("<th>First Name</th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<th>Last Name</th>\n\t\t\t</tr>\n\t\t</thead>\n\t\t<tbody>\n\t\t\t<c:forEach items=\"${customers}\" ");
        metawidget.append("var=\"Customer\">\n\t\t\t\t<tr>\n\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<a href=\"<c:url value=\"/customers/${Customer.id}\"/>\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t\t<c:out value=\"${Customer.firstName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t</a>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<a href=\"<c:url value=\"/customers/${Customer.id}\"/>\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t\t<c:out value=\"${Customer.lastName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t</a>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</c:forEach>\n\t\t</tbody>\n\t</table>\n\t<span class=\"paginator\">\n");
        metawidget.append("\t\t<c:if test=\"${current > 1}\">\n\t\t\t<a class=\"button\" href=\"<c:url value=\"/customers?first=${(current-2)");
        metawidget.append("*max}&max=${max}\"/>\">Previous</a>\n\t\t</c:if>\n\t\t<span>${first} to ${last} (of ${count})</span>\n\t\t");
        metawidget.append("<c:if test=\"${count > current*max}\">\n\t\t\t<a class=\"button\" href=\"<c:url value=\"/customers?first=");
        metawidget.append("${current*max}&max=${max}\"/>\">Next</a>\n\t\t</c:if>\n\t</span>\n</div>\n");

        Assert.assertEquals(metawidget.toString(), contents.toString());

        // View

        FileResource<?> view = web.getWebResource("WEB-INF/views/Customer/viewCustomer.jsp");
        Assert.assertTrue(view.exists());
        contents = Streams.toString(view.getResourceInputStream());

        metawidget = new StringBuilder();
        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<div class=\"section\">\n");
        metawidget.append("\t<form:form commandName=\"customer\">\n\n");
        metawidget.append("\t\t<table>")
                .append(CRLF);
        metawidget.append("\t\t\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"id\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"version\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append( "\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<c:out value=\"${customer.firstName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append( "\t\t\t\t\t\t<c:out value=\"${customer.lastName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t</tbody>")
                .append(CRLF);
        metawidget.append("\t\t</table>\n\n");
        metawidget.append("\t</form:form>\n\n\t<div class=\"buttons\">\n\t\t<table>\n\t\t\t<tr>\n\t\t\t\t<td>\n\t\t\t\t\t");
        metawidget.append("<a class=\"button\" href=\"<c:url value=\"/customers\"/>\">View All</a>\n\t\t\t\t</td>\n\t\t\t\t<td>\n\t\t\t\t\t");
        metawidget.append("<a class=\"button\" href=\"<c:url value=\"/customers/${customer.id}?edit=true\"/>\">Edit</a>\n\t\t\t\t");
        metawidget.append("</td>\n\t\t\t\t<td>\n\t\t\t\t\t<a class=\"button\" href=\"<c:url value=\"/customers/create\"/>\">");
        metawidget.append("Create New</a>\n\t\t\t\t</td>\n\t\t\t</tr>\n\t\t</table>\n\t</div>\n</div>\n");
        
        Assert.assertEquals(metawidget.toString(), contents.toString());

        Assert.assertTrue(java.getJavaResource(meta.getTopLevelPackage() + ".mvc.root.CustomerController").exists());
        Assert.assertTrue(java.getJavaResource(meta.getTopLevelPackage() + ".repo.CustomerDao").exists());
        Assert.assertTrue(java.getJavaResource(meta.getTopLevelPackage() + ".repo.CustomerDaoImpl").exists());

        Assert.assertTrue(java.getJavaResource(meta.getTopLevelPackage() + ".mvc.root.IndexController").exists());
    }

    @Test
    public void testGenerateFromEntityCamelCase() throws Exception
    {
        StringBuilder expectedContent;

        Project project = setupScaffoldProject();

        queueInputLines("");
        getShell().execute("entity --named CustomerPerson");
        getShell().execute("field string --named firstName");
        getShell().execute("field string --named lastName");

        queueInputLines("", "", "", "");
        getShell().execute("scaffold from-entity");

        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        // Create

        FileResource<?> create = web.getWebResource("WEB-INF/views/CustomerPerson/createCustomerPerson.jsp");
        Assert.assertTrue(create.exists());
        String contents = Streams.toString(create.getResourceInputStream());
        StringBuilder metawidget = new StringBuilder();

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<div class=\"section\">\n");
        metawidget.append("\t<form:form commandName=\"customerPerson\">\n\n");
        metawidget.append("\t\t<table>")
                .append(CRLF);
        metawidget.append("\t\t\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"id\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"version\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append( "\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:input path=\"firstName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:input path=\"lastName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append( "\t\t\t</tbody>")
                .append(CRLF);
        metawidget.append("\t\t</table>\n\n\t\t<div class=\"buttons\">\n\t\t\t<table>\n\t\t\t\t<tbody>\n\t\t\t\t\t");
        metawidget.append("<tr>\n\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t<input type=\"submit\" value=\"Save\" class=\"button\"/>\n\t\t\t\t\t\t");
        metawidget.append("</td>\n\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t<a class=\"button\" href=\"<c:url value=\"/customerpeople\"/>\">Cancel</a>\n");
        metawidget.append("\t\t\t\t\t\t</td>\n\t\t\t\t\t</tr>\n\t\t\t\t</tbody>\n\t\t\t</table>\n\t\t</div>\n\t</form:form>\n</div>\n");

        Assert.assertEquals(metawidget.toString(), contents.toString());

        // Update

        FileResource<?> update = web.getWebResource("WEB-INF/views/CustomerPerson/updateCustomerPerson.jsp");
        Assert.assertTrue(update.exists());
        contents = Streams.toString(update.getResourceInputStream());
        metawidget = new StringBuilder();

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<div class=\"section\">\n");
        metawidget.append("\t<form:form commandName=\"customerPerson\" action=\"${customerPerson.id}\">\n\n");
        metawidget.append("\t\t<table>")
                .append(CRLF);
        metawidget.append("\t\t\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"id\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"version\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append( "\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:input path=\"firstName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:input path=\"lastName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append( "\t\t\t</tbody>")
                .append(CRLF);
        metawidget.append("\t\t</table>\n\n\t\t<input type=\"submit\" value=\"Save\" class=\"button\"/>\n\n\t</form:form>\n\n\t");
        metawidget.append("<div class=\"buttons\">\n\t\t<table>\n\t\t\t<tbody>\n\t\t\t\t");
        metawidget.append("<tr>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<a class=\"button\" href=\"<c:url value=\"/customerpeople\"/>\">Cancel</a>\n\t\t\t\t");
        metawidget.append("\t</td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<form:form commandName=\"customerPerson\" action=\"${customerPerson.id}/delete\"");
        metawidget.append(" method=\"POST\">\n\t\t\t\t\t\t\t<input type=\"submit\" value=\"Delete\" class=\"button\"/>\n");
        metawidget.append("\t\t\t\t\t\t</form:form>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</tbody>\n\t\t</table>\n\t</div>\n</div>\n");

        Assert.assertEquals(metawidget.toString(), contents.toString());

        // Search

        FileResource<?> search = web.getWebResource("WEB-INF/views/CustomerPerson/customerpeople.jsp");
        Assert.assertTrue(update.exists());
        contents = Streams.toString(search.getResourceInputStream());
        metawidget = new StringBuilder();

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<div class=\"section\">\n");
        metawidget.append("\t<form:form commandName=\"search\" method=\"GET\">\n");
        metawidget.append("\t\t<table class=\"search\">")
                .append(CRLF);
        metawidget.append("\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:hidden path=\"id\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:hidden path=\"version\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t</th>")
                .append(CRLF);
        metawidget.append( "\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:input path=\"firstName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t</th>")
                .append(CRLF);
        metawidget.append("\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<form:input path=\"lastName\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t</tr>")
                .append(CRLF);
        metawidget.append( "\t</tbody>")
                .append(CRLF);
        metawidget.append("</table>\n\n\t\t<div class=\"buttons\">\n\t\t\t<table>\n\t\t\t\t<tbody>\n\t\t\t\t\t");
        metawidget.append("<tr>\n\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t<input type=\"submit\" value=\"Search\" class=\"button\"/>\n\t\t\t\t\t");
        metawidget.append("\t</td>\n\t\t\t\t\t\t<td>\n\t\t\t\t\t\t\t<a class=\"button\" href=\"<c:url value=\"/customerpeople/create\"/>\">");
        metawidget.append("Create New</a>\n\t\t\t\t\t\t</td>\n\t\t\t\t\t</tr>\n\t\t\t\t</tbody>\n\t\t\t</table>\n\t\t</div>\n\t");
        metawidget.append("</form:form>\n\n\t<table class=\"data-table\">\n\t\t<thead>\n\t\t\t<tr>\n\t\t\t\t");
        metawidget.append("<th>First Name</th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<th>Last Name</th>\n\t\t\t</tr>\n\t\t</thead>\n\t\t<tbody>\n\t\t\t<c:forEach items=\"${customerpeople}\" ");
        metawidget.append("var=\"CustomerPerson\">\n\t\t\t\t<tr>\n\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<a href=\"<c:url value=\"/customerpeople/${CustomerPerson.id}\"/>\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t\t<c:out value=\"${CustomerPerson.firstName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t</a>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<a href=\"<c:url value=\"/customerpeople/${CustomerPerson.id}\"/>\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t\t<c:out value=\"${CustomerPerson.lastName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t</a>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</c:forEach>\n\t\t</tbody>\n\t</table>\n\t<span class=\"paginator\">\n");
        metawidget.append("\t\t<c:if test=\"${current > 1}\">\n\t\t\t<a class=\"button\" href=\"<c:url value=\"/customerpeople?first=${(current-2)");
        metawidget.append("*max}&max=${max}\"/>\">Previous</a>\n\t\t</c:if>\n\t\t<span>${first} to ${last} (of ${count})</span>\n\t\t");
        metawidget.append("<c:if test=\"${count > current*max}\">\n\t\t\t<a class=\"button\" href=\"<c:url value=\"/customerpeople?first=");
        metawidget.append("${current*max}&max=${max}\"/>\">Next</a>\n\t\t</c:if>\n\t</span>\n</div>\n");

        Assert.assertEquals(metawidget.toString(), contents.toString());

        // View

        FileResource<?> view = web.getWebResource("WEB-INF/views/CustomerPerson/viewCustomerPerson.jsp");
        Assert.assertTrue(view.exists());
        contents = Streams.toString(view.getResourceInputStream());

        metawidget = new StringBuilder();
        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<div class=\"section\">\n");
        metawidget.append("\t<form:form commandName=\"customerPerson\">\n\n");
        metawidget.append("\t\t<table>")
                .append(CRLF);
        metawidget.append("\t\t\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"id\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:hidden path=\"version\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append( "\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<c:out value=\"${customerPerson.firstName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</th>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append( "\t\t\t\t\t\t<c:out value=\"${customerPerson.lastName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<td/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</tr>")
                .append(CRLF);
        metawidget.append("\t\t\t</tbody>")
                .append(CRLF);
        metawidget.append("\t\t</table>\n\n");
        metawidget.append("\t</form:form>\n\n\t<div class=\"buttons\">\n\t\t<table>\n\t\t\t<tr>\n\t\t\t\t<td>\n\t\t\t\t\t");
        metawidget.append("<a class=\"button\" href=\"<c:url value=\"/customerpeople\"/>\">View All</a>\n\t\t\t\t</td>\n\t\t\t\t<td>\n\t\t\t\t\t");
        metawidget.append("<a class=\"button\" href=\"<c:url value=\"/customerpeople/${customerPerson.id}?edit=true\"/>\">Edit</a>\n\t\t\t\t");
        metawidget.append("</td>\n\t\t\t\t<td>\n\t\t\t\t\t<a class=\"button\" href=\"<c:url value=\"/customerpeople/create\"/>\">");
        metawidget.append("Create New</a>\n\t\t\t\t</td>\n\t\t\t</tr>\n\t\t</table>\n\t</div>\n</div>\n");
        
        Assert.assertEquals(metawidget.toString(), contents.toString());
    }
}
