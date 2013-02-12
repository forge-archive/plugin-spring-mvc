/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and individual contributors
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

package org.jboss.forge.spec.spring.mvc;

import junit.framework.Assert;

import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.spec.javaee.ServletFacet;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.WebAppDescriptor;
import org.junit.Test;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class SpringPluginTest extends AbstractShellTest
{
    @Test
    public void testSpringSetup() throws Exception
    {
        getShell().setOutputStream(System.out);
        Project project = initializeJavaProject();
        queueInputLines("HIBERNATE", "JBOSS_AS7", "", "", "");
        getShell().execute("persistence setup");
        queueInputLines("Y", "");
        getShell().execute("spring setup");
        Assert.assertTrue(project.hasFacet(SpringFacet.class));
    }
    
    

    @Test
    public void testSetContextFileLocation() throws Exception
    {
        getShell().setOutputStream(System.out);
        Project project = initializeJavaProject();
        queueInputLines("HIBERNATE", "JBOSS_AS7", "", "", "");
        getShell().execute("persistence setup");
        queueInputLines("Y", "");
        getShell().execute("spring setup");

        ResourceFacet resources = project.getFacet(ResourceFacet.class);
        SpringFacet spring = project.getFacet(SpringFacet.class);
        resources.createResource("".toCharArray(), "META-INF/test-business-context.xml");

        getShell().execute("spring context-location --location META-INF/test-business-context.xml");

        Assert.assertEquals("META-INF/test-business-context.xml", spring.getContextFileLocation());
    }

    @Test
    public void testMVCFromTemplate() throws Exception
    {
        Project project = initializeJavaProject();
        queueInputLines("HIBERNATE", "JBOSS_AS7", "", "", "");
        getShell().execute("persistence setup");
        queueInputLines("", "");
        getShell().execute("spring setup");

        queueInputLines("", "", "");
        getShell().execute("spring mvc-from-template");
        Assert.assertTrue(project.getProjectRoot().getChild("src/main/webapp/WEB-INF/web.xml").exists());
        Assert.assertTrue(project.getProjectRoot().getChild("src/main/resources/META-INF/spring/applicationContext.xml").exists());
        Assert.assertTrue(project.getProjectRoot().getChild("src/main/webapp/WEB-INF/test-mvc-context.xml").exists());
    }

    @Test
    public void testMVC() throws Exception
    {
        Project project = initializeJavaProject();
        queueInputLines("HIBERNATE", "JBOSS_AS7", "", "", "");
        getShell().execute("persistence setup");
        queueInputLines("", "");
        getShell().execute("spring setup");

        getShell().execute("spring mvc --mvcContext /WEB-INF/servlet-context.xml --targetDir /admin --mvcPackage test.mvc.package");

        MetadataFacet meta = project.getFacet(MetadataFacet.class);
        ServletFacet servlet = project.getFacet(ServletFacet.class);
        SpringFacet spring = project.getFacet(SpringFacet.class);
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        WebAppDescriptor webXML = servlet.getConfig();

        Assert.assertNotNull(webXML);
        Assert.assertTrue(webXML.getContextParam("contextConfigLocation").contains("classpath:/" + spring.getContextFileLocation()));
        Assert.assertTrue(webXML.getListeners().contains("org.springframework.web.context.ContextLoaderListener"));

        Node webapp = XMLParser.parse(servlet.getConfigFile().getResourceInputStream());

        Assert.assertTrue(webapp.getSingle("display-name").getText().equals(meta.getProjectName()));
        Assert.assertNotNull(webapp.getSingle("persistence-context-ref"));

        Node dispatcherServlet = webapp.getSingle("servlet");
        Assert.assertTrue(dispatcherServlet.getSingle("servlet-name").getText().equals("admin"));
        Assert.assertTrue(dispatcherServlet.getSingle("servlet-class").getText().equals("org.springframework.web.servlet.DispatcherServlet"));

        Node param = dispatcherServlet.getSingle("init-param").getSingle("param-value");
        Assert.assertTrue(param.getText().equals("/WEB-INF/servlet-context.xml"));

        Node servletMapping = webapp.getSingle("servlet-mapping");
        Assert.assertTrue(servletMapping.getSingle("url-pattern").getText().equals("/admin/*"));
        Assert.assertTrue(servletMapping.getSingle("servlet-name").getText().equals("admin"));

        Node beans = XMLParser.parse(web.getWebResource("WEB-INF/servlet-context.xml").getResourceInputStream());

        Assert.assertNotNull(beans.getAttribute("xmlns"));
        Assert.assertNotNull(beans.getAttribute("xmlns:context"));
        Assert.assertNotNull(beans.getAttribute("xmlns:mvc"));
        Assert.assertNotNull(beans.getAttribute("xsi:schemaLocation"));

        Node mvcScan = beans.getSingle("context:component-scan");
        Assert.assertNotNull(mvcScan);
        Assert.assertEquals("test.mvc.package", mvcScan.getAttribute("base-package"));
        Assert.assertNotNull(beans.getSingle("mvc:annotation-driven"));
        Assert.assertNotNull(beans.getSingle("mvc:resources"));
        // Should this element be added for non-root servlets?
        //Assert.assertNotNull(beans.getSingle("mvc:default-servlet-handler"));

        boolean viewResolver = false;

        for (Node bean : beans.get("bean"))
        {
            if (bean.getAttribute("id") != null && bean.getAttribute("id").equals("viewResolver"))
            {
                viewResolver = true;
            }
        }

        Assert.assertTrue(viewResolver);
    }

    @Test
    public void testGenerateApplicationContext() throws Exception
    {
        Project project = initializeJavaProject();
        queueInputLines("HIBERNATE", "JBOSS_AS7", "", "", "");
        getShell().execute("persistence setup");
        queueInputLines("", "", "");
        getShell().execute("spring setup");

        getShell().execute("spring persistence");

        ResourceFacet resources = project.getFacet(ResourceFacet.class);
        SpringFacet spring = project.getFacet(SpringFacet.class);

        Node beans = XMLParser.parse(resources.getResource(spring.getContextFileLocation()).getResourceInputStream());

        Assert.assertNotNull(beans.getAttribute("xmlns"));
        Assert.assertNotNull(beans.getAttribute("xmlns:context"));
        Assert.assertNotNull(beans.getAttribute("xmlns:jee"));
        Assert.assertNotNull(beans.getAttribute("xmlns:tx"));
        Assert.assertNotNull(beans.getAttribute("xsi:schemaLocation"));

        Node scan = beans.getSingle("context:component-scan");
        Assert.assertEquals("com.test.repo", scan.getAttribute("base-package"));

        Assert.assertNotNull(beans.getSingle("tx:annotation-driven"));
        Assert.assertNotNull(beans.getSingle("tx:jta-transaction-manager"));

        Node entityManager = beans.getSingle("bean");
        Assert.assertEquals("entityManager", entityManager.getAttribute("id"));
        Assert.assertEquals("org.springframework.orm.jpa.support.SharedEntityManagerBean", entityManager.getAttribute("class"));

        Node entityManagerFactory = beans.getSingle("jee:jndi-lookup");
        Assert.assertEquals("entityManagerFactory", entityManagerFactory.getAttribute("id"));
        Assert.assertEquals("javax.persistence.EntityManagerFactory", entityManagerFactory.getAttribute("expected-type"));
        Assert.assertEquals("java:jboss/forge-default/persistence", entityManagerFactory.getAttribute("jndi-name"));
    }
    
    
    private Project prepSecurityTest() throws Exception {
    	getShell().setOutputStream(System.out);
        Project project = initializeJavaProject();
        queueInputLines("HIBERNATE", "JBOSS_AS7", "", "", "");
        getShell().execute("persistence setup");
        queueInputLines("", "");
        getShell().execute("spring setup");

        getShell().execute("spring mvc --mvcContext /WEB-INF/servlet-context.xml --targetDir /admin --mvcPackage test.mvc.package");

        MetadataFacet meta = project.getFacet(MetadataFacet.class);
        ServletFacet servlet = project.getFacet(ServletFacet.class);
        SpringFacet spring = project.getFacet(SpringFacet.class);
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        WebAppDescriptor webXML = servlet.getConfig();

        Assert.assertNotNull(webXML);
        Assert.assertTrue(webXML.getContextParam("contextConfigLocation").contains("classpath:/" + spring.getContextFileLocation()));
        Assert.assertTrue(webXML.getListeners().contains("org.springframework.web.context.ContextLoaderListener"));

        Node webapp = XMLParser.parse(servlet.getConfigFile().getResourceInputStream());

        Assert.assertTrue(webapp.getSingle("display-name").getText().equals(meta.getProjectName()));
        Assert.assertNotNull(webapp.getSingle("persistence-context-ref"));

        Node dispatcherServlet = webapp.getSingle("servlet");
        Assert.assertTrue(dispatcherServlet.getSingle("servlet-name").getText().equals("admin"));
        Assert.assertTrue(dispatcherServlet.getSingle("servlet-class").getText().equals("org.springframework.web.servlet.DispatcherServlet"));

        Node param = dispatcherServlet.getSingle("init-param").getSingle("param-value");
        Assert.assertTrue(param.getText().equals("/WEB-INF/servlet-context.xml"));

        Node servletMapping = webapp.getSingle("servlet-mapping");
        Assert.assertTrue(servletMapping.getSingle("url-pattern").getText().equals("/admin/*"));
        Assert.assertTrue(servletMapping.getSingle("servlet-name").getText().equals("admin"));

        Node beans = XMLParser.parse(web.getWebResource("WEB-INF/servlet-context.xml").getResourceInputStream());

        Assert.assertNotNull(beans.getAttribute("xmlns"));
        Assert.assertNotNull(beans.getAttribute("xmlns:context"));
        Assert.assertNotNull(beans.getAttribute("xmlns:mvc"));
        Assert.assertNotNull(beans.getAttribute("xsi:schemaLocation"));

        Node mvcScan = beans.getSingle("context:component-scan");
        Assert.assertNotNull(mvcScan);
        Assert.assertEquals("test.mvc.package", mvcScan.getAttribute("base-package"));
        Assert.assertNotNull(beans.getSingle("mvc:annotation-driven"));
        Assert.assertNotNull(beans.getSingle("mvc:resources"));
        // Should this element be added for non-root servlets?
        //Assert.assertNotNull(beans.getSingle("mvc:default-servlet-handler"));

        boolean viewResolver = false;

        for (Node bean : beans.get("bean"))
        {
            if (bean.getAttribute("id") != null && bean.getAttribute("id").equals("viewResolver"))
            {
                viewResolver = true;
            }
        }

        Assert.assertTrue(viewResolver);
        return project;
    }
    
	private void checkAuthenticationManager(Node security) {
		Node authenticationManager = security.getSingle("authentication-manager");
        Assert.assertNotNull(authenticationManager);
        
        Node authenticationProvider = authenticationManager.getSingle("authentication-provider");
        Assert.assertNotNull(authenticationProvider);
        Assert.assertEquals("userService", authenticationProvider.getAttribute("user-service-ref"));
	}       
    
	private void checkHttpNode(Node security) {
		Assert.assertNotNull(security.getAttribute("xmlns"));
        Assert.assertNotNull(security.getAttribute("xmlns:beans"));
        Assert.assertNotNull(security.getAttribute("xsi:schemaLocation"));

        Node http = security.getSingle("http");
        Assert.assertNotNull(http);
        Assert.assertEquals("true", http.getAttribute("auto-config"));
        
        Node interceptURL = http.getChildren().get(0);
        Assert.assertEquals("/**/create*", interceptURL.getAttribute("pattern"));
        Assert.assertEquals("ROLE_ADMIN", interceptURL.getAttribute("access"));
        interceptURL = http.getChildren().get(1);
        Assert.assertEquals("/**/edit*", interceptURL.getAttribute("pattern"));
        Assert.assertEquals("ROLE_ADMIN", interceptURL.getAttribute("access"));
        
        Node rememberMe = http.getSingle("remember-me");
        Assert.assertNotNull(rememberMe);
	}
    
    @Test
    public void testSecurityInMemory() throws Exception
    {
        Project project = prepSecurityTest();
        queueInputLines("1", "", "");
        getShell().execute("spring security");

        SpringFacet spring = project.getFacet(SpringFacet.class);

        Node security = XMLParser.parse(spring.getSecurityContextFile("").getResourceInputStream());
        
        checkHttpNode(security);
        
        Node userService = security.getSingle("user-service");
        Assert.assertNotNull(userService);
        Assert.assertEquals("userService", userService.getAttribute("id"));
        
        Node user = userService.getSingle("user");
        Assert.assertNotNull(user);
        Assert.assertEquals("admin", user.getAttribute("name"));
        Assert.assertEquals("adminPass", user.getAttribute("password"));
        Assert.assertEquals("ROLE_ADMIN", user.getAttribute("authorities"));

        checkAuthenticationManager(security);               
    }
    
    @Test
    public void testSecurityJDBC() throws Exception
    {
    	Project project = prepSecurityTest();
        queueInputLines("2", "testDataSource");
        getShell().execute("spring security");
        
        SpringFacet spring = project.getFacet(SpringFacet.class);
        
        Node security = XMLParser.parse(spring.getSecurityContextFile("").getResourceInputStream());
        
        checkHttpNode(security);
        
        Node jdbcUserService = security.getSingle("jdbc-user-service");
        Assert.assertNotNull(jdbcUserService);
        Assert.assertEquals("userService", jdbcUserService.getAttribute("id"));
        Assert.assertEquals("testDataSource", jdbcUserService.getAttribute("data-source-ref"));

        checkAuthenticationManager(security);
    }
    
    @Test
    public void testSecurityLDAP() throws Exception
    {
    	Project project = prepSecurityTest();
        queueInputLines("3", "ldap://forgeplugintest.com:389/dc=forge,dc=com");
        getShell().execute("spring security");
        
        SpringFacet spring = project.getFacet(SpringFacet.class);
        
        Node security = XMLParser.parse(spring.getSecurityContextFile("").getResourceInputStream());
        
        checkHttpNode(security);
        
        Node userService = security.getSingle("ldap-user-service");
        Assert.assertNotNull(userService);
        Assert.assertEquals("userService", userService.getAttribute("id"));
        Assert.assertEquals("(uid={0})", userService.getAttribute("user-search-filter"));
        Assert.assertEquals("member={0}", userService.getAttribute("group-search-filter"));
        
        Node server = security.getSingle("ldap-server");
        Assert.assertNotNull(server);
        Assert.assertEquals("ldap://forgeplugintest.com:389/dc=forge,dc=com", server.getAttribute("url"));

        checkAuthenticationManager(security);
    }
}
