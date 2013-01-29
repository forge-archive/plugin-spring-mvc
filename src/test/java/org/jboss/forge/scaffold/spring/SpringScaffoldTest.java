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
        Assert.assertTrue(contents.contains("<div id=\"navigation\">"));
        Assert.assertTrue(contents.contains("<div id=\"content\">"));
        Assert.assertTrue(contents.contains("<div id=\"footer-wrapper"));
    }

    @Test
    public void testScaffoldSetupWithScaffoldTypeandTargetDir() throws Exception
    {
        Project project = initializeJavaProject();
        queueInputLines("HIBERNATE", "JBOSS_AS7", "", "", "");
        getShell().execute("persistence setup");
        queueInputLines("", "", "2", "", "", "");
        getShell().execute("scaffold setup --scaffoldType spring --targetDir admin");
        Assert.assertTrue(project.hasFacet(SpringScaffold.class));
    }

    @Test(expected = PluginExecutionException.class)
    public void testCannotGenerateFromEntityUntilScaffoldInstalled() throws Exception
    {
        initializeJavaProject();

        queueInputLines("", "", "");
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

        queueInputLines("", "", "", "", "");
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
        metawidget.append("<!-- Generated by Forge -->\n\n");

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<form:form commandName=\"customer\">\n\n");
        metawidget.append("\t<form:hidden path=\"id\"/>\n");
        metawidget.append("\t<form:hidden path=\"version\"/>\n\n");
        metawidget.append("\t<table>")
                .append(CRLF);
        metawidget.append("\t\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<th class=\"label\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t</th>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t<td class=\"component\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<div>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t\t\t<form:input path=\"firstName\"/>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t\t\t<div class=\"error\">")
        	.append(CRLF);
        metawidget.append("\t\t\t\t\t\t\t<form:errors path=\"firstName\"/>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t\t\t</div>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t\t</div>")
    		.append(CRLF);
        metawidget.append("\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<td class=\"required\"/>")
                .append(CRLF);        
        metawidget.append("\t\t\t</tr>")
        	.append(CRLF);
        
        metawidget.append("\t\t\t<tr>")
        	.append(CRLF);
		metawidget.append("\t\t\t\t<th class=\"label\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
			.append(CRLF);
		metawidget.append("\t\t\t\t</th>")
			.append(CRLF);
		metawidget.append("\t\t\t\t<td class=\"component\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t\t<div>")
			.append(CRLF);
		metawidget.append("\t\t\t\t\t\t<form:input path=\"lastName\"/>")
			.append(CRLF);
		metawidget.append("\t\t\t\t\t\t<div class=\"error\">")
			.append(CRLF);
		metawidget.append("\t\t\t\t\t\t\t<form:errors path=\"lastName\"/>")
			.append(CRLF);
		metawidget.append("\t\t\t\t\t\t</div>")
			.append(CRLF);
		metawidget.append("\t\t\t\t\t</div>")
			.append(CRLF);
		metawidget.append("\t\t\t\t</td>")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<td class=\"required\"/>")
		        .append(CRLF);        
		metawidget.append("\t\t\t</tr>")
			.append(CRLF);
        metawidget.append( "\t\t</tbody>")
                .append(CRLF);
        metawidget.append("\t</table>\n\n\t<span class=\"buttons\">\n");
        metawidget.append("\t\t<input type=\"submit\" value=\"Save\" class=\"btn btn-primary\"/>\n");
        metawidget.append("\t\t<a class=\"btn btn-primary\" href=\"<c:url value=\"/customers\"/>\">Cancel</a>\n");
        metawidget.append("\t</span>\n</form:form>\n");


        Assert.assertEquals(metawidget.toString(), contents.toString());

        // Update

        FileResource<?> update = web.getWebResource("WEB-INF/views/Customer/editCustomer.jsp");
        Assert.assertTrue(update.exists());
        contents = Streams.toString(update.getResourceInputStream());
        metawidget = new StringBuilder();
        metawidget.append("<!-- Generated by Forge -->\n\n");

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        
        metawidget.append("<form:form commandName=\"customer\" name=\"edit\" onsubmit=\"onSubmit()\">\n\n");
        metawidget.append("\t<form:hidden path=\"id\"/>\n");
        metawidget.append("\t<form:hidden path=\"version\"/>\n\n");
        metawidget.append("\t<table>")
                .append(CRLF);
        metawidget.append("\t\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<th class=\"label\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t</th>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t<td class=\"component\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<form:input path=\"firstName\"/>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<td class=\"required\"/>")
                .append(CRLF);        
        metawidget.append("\t\t\t</tr>")
        	.append(CRLF);        
        metawidget.append("\t\t\t<tr>")
        	.append(CRLF);
		metawidget.append("\t\t\t\t<th class=\"label\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
			.append(CRLF);
		metawidget.append("\t\t\t\t</th>")
			.append(CRLF);
		metawidget.append("\t\t\t\t<td class=\"component\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t\t<form:input path=\"lastName\"/>")
			.append(CRLF);
		metawidget.append("\t\t\t\t</td>")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<td class=\"required\"/>")
		        .append(CRLF);        
		metawidget.append("\t\t\t</tr>")
			.append(CRLF);
        metawidget.append( "\t\t</tbody>")
                .append(CRLF);
        metawidget.append("\t</table>\n\n\t<span class=\"buttons\">\n");
        metawidget.append("\t\t<input type=\"submit\" value=\"Save\" class=\"btn btn-primary\" onclick=\"document.pressed=this.value\"/>\n");
        metawidget.append("\t\t<a class=\"btn btn-primary\" href=\"<c:url value=\"/customers/${customer.id}\"/>\">Cancel</a>\n");
        metawidget.append("\t\t<input type=\"submit\" value=\"Delete\" class=\"btn btn-primary\" onclick=\"document.pressed=this.value\"/>\n");
        metawidget.append("\t</span>\n</form:form>\n\n");
        
        metawidget.append("<script type=\"text/javascript\">\n");
        metawidget.append("\tfunction onSubmit()\n\t{\n");
        metawidget.append("\t\tif (document.pressed == 'Save')\n\t\t{\n");
        metawidget.append("\t\t\tdocument.edit.action = \"${customer.id}\";\n\t\t}\n");
        metawidget.append("\t\telse if (document.pressed == 'Delete')\n\t\t{\n");
        metawidget.append("\t\t\tdocument.edit.action = \"${customer.id}/delete\";\n\t\t}\n\n");
        metawidget.append("\t\treturn true;\n\t}\n");
        metawidget.append("</script>");   
        
        Assert.assertEquals(metawidget.toString(), contents.toString());

        // Search

        FileResource<?> search = web.getWebResource("WEB-INF/views/Customer/customers.jsp");
        Assert.assertTrue(update.exists());
        contents = Streams.toString(search.getResourceInputStream());
        metawidget = new StringBuilder();
        metawidget.append("<!-- Generated by Forge -->\n\n");

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<span class=\"search\">\n");
        metawidget.append("\t<form:form commandName=\"search\" method=\"POST\">\n\n");
        metawidget.append("\t\t<form:hidden path=\"id\"/>\n");
        metawidget.append("\t\t<form:hidden path=\"version\"/>\n\n");
        metawidget.append("\t\t<table>")
                .append(CRLF);
        metawidget.append("\t<tbody>")
		        .append(CRLF);
		metawidget.append("\t\t<tr>")
		        .append(CRLF);
		metawidget.append("\t\t\t<th class=\"label\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
			.append(CRLF);
		metawidget.append("\t\t\t</th>")
			.append(CRLF);
		metawidget.append("\t\t\t<td class=\"component\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<form:input path=\"firstName\"/>")
			.append(CRLF);
		metawidget.append("\t\t\t</td>")
		        .append(CRLF);
		metawidget.append("\t\t\t<td class=\"required\"/>")
		        .append(CRLF);        
		metawidget.append("\t\t</tr>")
			.append(CRLF);
		
		metawidget.append("\t\t<tr>")
			.append(CRLF);
		metawidget.append("\t\t\t<th class=\"label\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
			.append(CRLF);
		metawidget.append("\t\t\t</th>")
			.append(CRLF);
		metawidget.append("\t\t\t<td class=\"component\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<form:input path=\"lastName\"/>")
			.append(CRLF);
		metawidget.append("\t\t\t</td>")
		        .append(CRLF);
		metawidget.append("\t\t\t<td class=\"required\"/>")
		        .append(CRLF);        
		metawidget.append("\t\t</tr>")
			.append(CRLF);
		metawidget.append( "\t</tbody>")
		        .append(CRLF);
		metawidget.append("</table>\n\t\n\t\t<span class=\"buttons\">\n");
        metawidget.append("\t\t\t<input type=\"submit\" value=\"Search\" class=\"btn btn-primary\" name=\"search\"/>\n\t\t\t");
        metawidget.append("<input type=\"submit\" value=\"Create New\" class=\"btn btn-primary\" name=\"create\"/>\n\t\t");
        metawidget.append("</span>\n\t");
        metawidget.append("</form:form>\n</span>\n\n<table class=\"data-table\">\n\t<thead>\n\t\t<tr>\n\t\t\t");
        metawidget.append("<th>First Name</th>")
                .append(CRLF);
        metawidget.append("\t\t\t<th>Last Name</th>\n\t\t</tr>\n\t</thead>\n\t<tbody>\n\t\t<c:forEach items=\"${customers}\" ");
        metawidget.append("var=\"Customer\">\n\t\t\t<tr>\n\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<a href=\"<c:url value=\"/customers/${Customer.id}\"/>\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<c:out value=\"${Customer.firstName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</a>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<a href=\"<c:url value=\"/customers/${Customer.id}\"/>\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<c:out value=\"${Customer.lastName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</a>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</td>\n\t\t\t</tr>\n\t\t</c:forEach>\n\t</tbody>\n</table>\n\n<span class=\"paginator\">\n");
        metawidget.append("\t<c:if test=\"${current > 1}\">\n\t\t<a class=\"btn btn-primary\" href=\"<c:url value=\"/customers?first=${(current-2)");
        metawidget.append("*max}&max=${max}\"/>\">Previous</a>\n\t</c:if>\n\n\t<span>${first} to ${last} (of ${count})</span>\n\n\t");
        metawidget.append("<c:if test=\"${count > current*max}\">\n\t\t<a class=\"btn btn-primary\" href=\"<c:url value=\"/customers?first=");
        metawidget.append("${current*max}&max=${max}\"/>\">Next</a>\n\t</c:if>\n</span>\n");

        Assert.assertEquals(metawidget.toString(), contents.toString());

        // View

        FileResource<?> view = web.getWebResource("WEB-INF/views/Customer/viewCustomer.jsp");
        Assert.assertTrue(view.exists());
        contents = Streams.toString(view.getResourceInputStream());
        metawidget.append("<!-- Generated by Forge -->\n\n");

        metawidget = new StringBuilder();
        metawidget.append("<!-- Generated by Forge -->\n\n");

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<form:form commandName=\"customer\">\n\n");
        metawidget.append("\t<form:hidden path=\"id\"/>\n");
        metawidget.append("\t<form:hidden path=\"version\"/>\n\n");
        metawidget.append("\t<input type=\"hidden\" value=\"${customer.id}\"/>")
        	.append(CRLF);
        metawidget.append("\t<input type=\"hidden\" value=\"${customer.version}\"/>")
    		.append(CRLF);
        metawidget.append("\t<c:out value=\"${customer.firstName}\">")
        	.append(CRLF);
        metawidget.append("\t\t<input type=\"hidden\" value=\"${customer.firstName}\"/>")
        	.append(CRLF);
        metawidget.append("\t</c:out>")
        	.append(CRLF);
        metawidget.append("\t<c:out value=\"${customer.lastName}\">")
	    	.append(CRLF);
	    metawidget.append("\t\t<input type=\"hidden\" value=\"${customer.lastName}\"/>")
	    	.append(CRLF);
	    metawidget.append("\t</c:out>\n\n");     

        metawidget.append("</form:form>\n\n<span class=\"buttons\">\n\t");
        metawidget.append("<a class=\"btn btn-primary\" href=\"<c:url value=\"/customers\"/>\">View All</a>\n\t");
        metawidget.append("<a class=\"btn btn-primary\" href=\"<c:url value=\"/customers/${customer.id}?edit=true\"/>\">Edit</a>\n\t");
        metawidget.append("<a class=\"btn btn-primary\" href=\"<c:url value=\"/customers/create\"/>\">");
        metawidget.append("Create New</a>\n</span>\n");
        
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

        queueInputLines("", "", "", "", "");
        getShell().execute("scaffold from-entity");

        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        // Create

        FileResource<?> create = web.getWebResource("WEB-INF/views/CustomerPerson/createCustomerPerson.jsp");
        Assert.assertTrue(create.exists());
        String contents = Streams.toString(create.getResourceInputStream());
        StringBuilder metawidget = new StringBuilder();
        metawidget.append("<!-- Generated by Forge -->\n\n");

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<form:form commandName=\"customerPerson\">\n\n");
        metawidget.append("\t<form:hidden path=\"id\"/>\n");
        metawidget.append("\t<form:hidden path=\"version\"/>\n\n");
        metawidget.append("\t<table>")
                .append(CRLF);
        metawidget.append("\t\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<th class=\"label\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t</th>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t<td class=\"component\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<form:input path=\"firstName\"/>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<td class=\"required\"/>")
                .append(CRLF);        
        metawidget.append("\t\t\t</tr>")
        	.append(CRLF);        
        metawidget.append("\t\t\t<tr>")
        	.append(CRLF);
		metawidget.append("\t\t\t\t<th class=\"label\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
			.append(CRLF);
		metawidget.append("\t\t\t\t</th>")
			.append(CRLF);
		metawidget.append("\t\t\t\t<td class=\"component\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t\t\t<form:input path=\"lastName\"/>")
			.append(CRLF);
		metawidget.append("\t\t\t\t</td>")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<td class=\"required\"/>")
		        .append(CRLF);        
		metawidget.append("\t\t\t</tr>")
			.append(CRLF);
        metawidget.append( "\t\t</tbody>")
                .append(CRLF);
        metawidget.append("\t</table>\n\n\t<span class=\"buttons\">\n");
        metawidget.append("\t\t<input type=\"submit\" value=\"Save\" class=\"btn btn-primary\"/>\n");
        metawidget.append("\t\t<a class=\"btn btn-primary\" href=\"<c:url value=\"/customerpeople\"/>\">Cancel</a>\n");
        metawidget.append("\t</span>\n</form:form>\n");

        Assert.assertEquals(metawidget.toString(), contents.toString());

        // Update

        FileResource<?> update = web.getWebResource("WEB-INF/views/CustomerPerson/editCustomerPerson.jsp");
        Assert.assertTrue(update.exists());
        contents = Streams.toString(update.getResourceInputStream());
        metawidget = new StringBuilder();
        metawidget.append("<!-- Generated by Forge -->\n\n");

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        
        metawidget.append("<form:form commandName=\"customerPerson\" name=\"edit\" onsubmit=\"onSubmit()\">\n\n");
        metawidget.append("\t<form:hidden path=\"id\"/>\n");
        metawidget.append("\t<form:hidden path=\"version\"/>\n\n");
        metawidget.append("\t<table>")
                .append(CRLF);
        metawidget.append("\t\t<tbody>")
                .append(CRLF);
        metawidget.append("\t\t\t<tr>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<th class=\"label\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t</th>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t<td class=\"component\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<form:input path=\"firstName\"/>")
        	.append(CRLF);
        metawidget.append("\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<td class=\"required\"/>")
                .append(CRLF);        
        metawidget.append("\t\t\t</tr>")
        	.append(CRLF);
        
        metawidget.append("\t\t\t<tr>")
        	.append(CRLF);
		metawidget.append("\t\t\t\t<th class=\"label\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
			.append(CRLF);
		metawidget.append("\t\t\t\t</th>")
			.append(CRLF);
		metawidget.append("\t\t\t\t<td class=\"component\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t\t<form:input path=\"lastName\"/>")
			.append(CRLF);
		metawidget.append("\t\t\t\t</td>")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<td class=\"required\"/>")
		        .append(CRLF);        
		metawidget.append("\t\t\t</tr>")
			.append(CRLF);
        metawidget.append( "\t\t</tbody>")
                .append(CRLF);
        metawidget.append("\t</table>\n\n\t<span class=\"buttons\">\n");
        metawidget.append("\t\t<input type=\"submit\" value=\"Save\" class=\"btn btn-primary\" onclick=\"document.pressed=this.value\"/>\n");
        metawidget.append("\t\t<a class=\"btn btn-primary\" href=\"<c:url value=\"/customerpeople/${customerPerson.id}\"/>\">Cancel</a>\n");
        metawidget.append("\t\t<input type=\"submit\" value=\"Delete\" class=\"btn btn-primary\" onclick=\"document.pressed=this.value\"/>\n");
        metawidget.append("\t</span>\n</form:form>\n\n");
        
        metawidget.append("<script type=\"text/javascript\">\n");
        metawidget.append("\tfunction onSubmit()\n\t{\n");
        metawidget.append("\t\tif (document.pressed == 'Save')\n\t\t{\n");
        metawidget.append("\t\t\tdocument.edit.action = \"${customerPerson.id}\";\n\t\t}\n");
        metawidget.append("\t\telse if (document.pressed == 'Delete')\n\t\t{\n");
        metawidget.append("\t\t\tdocument.edit.action = \"${customerPerson.id}/delete\";\n\t\t}\n\n");
        metawidget.append("\t\treturn true;\n\t}\n");
        metawidget.append("</script>");   
        

        Assert.assertEquals(metawidget.toString(), contents.toString());

        // Search

        FileResource<?> search = web.getWebResource("WEB-INF/views/CustomerPerson/customerpeople.jsp");
        Assert.assertTrue(update.exists());
        contents = Streams.toString(search.getResourceInputStream());
        metawidget = new StringBuilder();
        metawidget.append("<!-- Generated by Forge -->\n\n");

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<span class=\"search\">\n");
        metawidget.append("\t<form:form commandName=\"search\" method=\"POST\">\n\n");
        metawidget.append("\t\t<form:hidden path=\"id\"/>\n");
        metawidget.append("\t\t<form:hidden path=\"version\"/>\n\n");
        metawidget.append("\t\t<table>")
                .append(CRLF);
        metawidget.append("\t<tbody>")
		        .append(CRLF);
		metawidget.append("\t\t<tr>")
		        .append(CRLF);
		metawidget.append("\t\t\t<th class=\"label\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<form:label path=\"firstName\">First Name:</form:label>")
			.append(CRLF);
		metawidget.append("\t\t\t</th>")
			.append(CRLF);
		metawidget.append("\t\t\t<td class=\"component\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<form:input path=\"firstName\"/>")
			.append(CRLF);
		metawidget.append("\t\t\t</td>")
		        .append(CRLF);
		metawidget.append("\t\t\t<td class=\"required\"/>")
		        .append(CRLF);        
		metawidget.append("\t\t</tr>")
			.append(CRLF);
		
		metawidget.append("\t\t<tr>")
			.append(CRLF);
		metawidget.append("\t\t\t<th class=\"label\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<form:label path=\"lastName\">Last Name:</form:label>")
			.append(CRLF);
		metawidget.append("\t\t\t</th>")
			.append(CRLF);
		metawidget.append("\t\t\t<td class=\"component\">")
		        .append(CRLF);
		metawidget.append("\t\t\t\t<form:input path=\"lastName\"/>")
			.append(CRLF);
		metawidget.append("\t\t\t</td>")
		        .append(CRLF);
		metawidget.append("\t\t\t<td class=\"required\"/>")
		        .append(CRLF);        
		metawidget.append("\t\t</tr>")
			.append(CRLF);
		metawidget.append( "\t</tbody>")
		        .append(CRLF);
		metawidget.append("</table>\n\t\n\t\t<span class=\"buttons\">\n");
        metawidget.append("\t\t\t<input type=\"submit\" value=\"Search\" class=\"btn btn-primary\" name=\"search\"/>\n\t\t\t");
        metawidget.append("<input type=\"submit\" value=\"Create New\" class=\"btn btn-primary\" name=\"create\"/>\n\t\t");
        metawidget.append("</span>\n\t");
        metawidget.append("</form:form>\n</span>\n\n<table class=\"data-table\">\n\t<thead>\n\t\t<tr>\n\t\t\t");
        metawidget.append("<th>First Name</th>")
                .append(CRLF);
        metawidget.append("\t\t\t<th>Last Name</th>\n\t\t</tr>\n\t</thead>\n\t<tbody>\n\t\t<c:forEach items=\"${customerpeople}\" ");
        metawidget.append("var=\"CustomerPerson\">\n\t\t\t<tr>\n\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<a href=\"<c:url value=\"/customerpeople/${CustomerPerson.id}\"/>\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<c:out value=\"${CustomerPerson.firstName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</a>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t<td>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t<a href=\"<c:url value=\"/customerpeople/${CustomerPerson.id}\"/>\">")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t\t<c:out value=\"${CustomerPerson.lastName}\"/>")
                .append(CRLF);
        metawidget.append("\t\t\t\t\t</a>")
                .append(CRLF);
        metawidget.append("\t\t\t\t</td>\n\t\t\t</tr>\n\t\t</c:forEach>\n\t</tbody>\n</table>\n\n<span class=\"paginator\">\n");
        metawidget.append("\t<c:if test=\"${current > 1}\">\n\t\t<a class=\"btn btn-primary\" href=\"<c:url value=\"/customerpeople?first=${(current-2)");
        metawidget.append("*max}&max=${max}\"/>\">Previous</a>\n\t</c:if>\n\n\t<span>${first} to ${last} (of ${count})</span>\n\n\t");
        metawidget.append("<c:if test=\"${count > current*max}\">\n\t\t<a class=\"btn btn-primary\" href=\"<c:url value=\"/customerpeople?first=");
        metawidget.append("${current*max}&max=${max}\"/>\">Next</a>\n\t</c:if>\n</span>\n");

        Assert.assertEquals(metawidget.toString(), contents.toString());

        // View

        FileResource<?> view = web.getWebResource("WEB-INF/views/CustomerPerson/viewCustomerPerson.jsp");
        Assert.assertTrue(view.exists());
        contents = Streams.toString(view.getResourceInputStream());
        

        metawidget = new StringBuilder();
        metawidget.append("<!-- Generated by Forge -->\n\n");

        metawidget.append("<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n");
        metawidget.append("<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>\n\n");
        metawidget.append("<form:form commandName=\"customerPerson\">\n\n");
        metawidget.append("\t<form:hidden path=\"id\"/>\n");
        metawidget.append("\t<form:hidden path=\"version\"/>\n\n");
        metawidget.append("\t<input type=\"hidden\" value=\"${customerPerson.id}\"/>")
        	.append(CRLF);
        metawidget.append("\t<input type=\"hidden\" value=\"${customerPerson.version}\"/>")
    		.append(CRLF);
        metawidget.append("\t<c:out value=\"${customerPerson.firstName}\">")
        	.append(CRLF);
        metawidget.append("\t\t<input type=\"hidden\" value=\"${customerPerson.firstName}\"/>")
        	.append(CRLF);
        metawidget.append("\t</c:out>")
        	.append(CRLF);
        metawidget.append("\t<c:out value=\"${customerPerson.lastName}\">")
	    	.append(CRLF);
	    metawidget.append("\t\t<input type=\"hidden\" value=\"${customerPerson.lastName}\"/>")
	    	.append(CRLF);
	    metawidget.append("\t</c:out>\n\n");     

        metawidget.append("</form:form>\n\n<span class=\"buttons\">\n\t");
        metawidget.append("<a class=\"btn btn-primary\" href=\"<c:url value=\"/customerpeople\"/>\">View All</a>\n\t");
        metawidget.append("<a class=\"btn btn-primary\" href=\"<c:url value=\"/customerpeople/${customerPerson.id}?edit=true\"/>\">Edit</a>\n\t");
        metawidget.append("<a class=\"btn btn-primary\" href=\"<c:url value=\"/customerpeople/create\"/>\">");
        metawidget.append("Create New</a>\n</span>\n");
        
        Assert.assertEquals(metawidget.toString(), contents.toString());
    }
}
