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

package org.jboss.forge.spec.spring.security;

import junit.framework.Assert;

import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.test.AbstractShellTest;
import org.junit.Test;

import java.util.List;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class SpringSecurityPluginTest extends AbstractShellTest
{
	private Project prepSecurityTest() throws Exception {
    	getShell().setOutputStream(System.out);
        Project project = initializeJavaProject();
        queueInputLines("HIBERNATE", "JBOSS_AS7", "", "", "");
        getShell().execute("persistence setup");
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

    private void checkUsers(Node security) {
        Node userService = security.getSingle("user-service");
        Assert.assertNotNull(userService);
        List<Node> users = userService.getChildren();
        Assert.assertNotNull(users);
        Assert.assertEquals(2, users.size());

        Node user = users.get(0);
        Assert.assertNotNull(user);
        Assert.assertEquals("admin", user.getAttribute("name"));
        Assert.assertEquals("adminPass", user.getAttribute("password"));
        Assert.assertEquals("ROLE_ADMIN", user.getAttribute("authorities"));

        user = users.get(1);
        Assert.assertNotNull(user);
        Assert.assertEquals("Tejas", user.getAttribute("name"));
        Assert.assertEquals("password", user.getAttribute("password"));
        Assert.assertEquals("ROLE_DEVELOPER", user.getAttribute("authorities"));
    }

    @Test
    public void testSecurityInMemory() throws Exception
    {
        Project project = prepSecurityTest();
        queueInputLines("", "Y", "1", "", "");
        getShell().execute("security setup");

        queueInputLines("Tejas", "password", "ROLE_DEVELOPER");
        getShell().execute("security add-user");
        queueInputLines("Ryan", "password", "ROLE_DEVELOPER");
        getShell().execute("security add-user");
        queueInputLines("Ryan");
        getShell().execute("security remove-user");

        SpringSecurityFacet spring = project.getFacet(SpringSecurityFacet.class);

        Node security = XMLParser.parse(spring.getSecurityContextFile("").getResourceInputStream());

        checkHttpNode(security);

        Node userService = security.getSingle("user-service");
        Assert.assertNotNull(userService);
        Assert.assertEquals("userService", userService.getAttribute("id"));

        checkAuthenticationManager(security);

        checkUsers(security);
    }

    @Test
    public void testSecurityJDBC() throws Exception
    {
    	Project project = prepSecurityTest();
        queueInputLines("", "Y", "2", "testDataSource");
        getShell().execute("security setup");
        
        SpringSecurityFacet spring = project.getFacet(SpringSecurityFacet.class);
        
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
        queueInputLines("", "Y", "3", "ldap://forgeplugintest.com:389/dc=forge,dc=com");
        getShell().execute("security setup");
        
        SpringSecurityFacet spring = project.getFacet(SpringSecurityFacet.class);
        
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

    @Test
    public void testSecurityJSF() throws Exception
    {
        Project project = prepSecurityTest();
        queueInputLines("", "", "", "");
        getShell().execute("scaffold setup");
        queueInputLines("1", "", "");
        getShell().execute("security setup");

        SpringSecurityFacet spring = project.getFacet(SpringSecurityFacet.class);
        Node security = XMLParser.parse(spring.getSecurityContextFile("").getResourceInputStream());

        checkHttpNodeJSF(security);


        Node userService = security.getSingle("user-service");
        Assert.assertNotNull(userService);
        Assert.assertEquals("userService", userService.getAttribute("id"));

        checkAuthenticationManager(security);
    }

    private void checkHttpNodeJSF(Node security) {
        Assert.assertNotNull(security.getAttribute("xmlns"));
        Assert.assertNotNull(security.getAttribute("xmlns:beans"));
        Assert.assertNotNull(security.getAttribute("xsi:schemaLocation"));

        Node http = security.getSingle("http");
        Assert.assertNotNull(http);
        Assert.assertEquals("true", http.getAttribute("auto-config"));

        Node formLogin = http.getSingle("form-login");
        Assert.assertNotNull(formLogin);
        Assert.assertEquals("true", formLogin.getAttribute("always-use-default-target"));
        Node interceptURL = http.getChildren().get(0);
        Assert.assertEquals("/**/create*", interceptURL.getAttribute("pattern"));
        Assert.assertEquals("ROLE_ADMIN", interceptURL.getAttribute("access"));
        interceptURL = http.getChildren().get(1);
        Assert.assertEquals("/**/edit*", interceptURL.getAttribute("pattern"));
        Assert.assertEquals("ROLE_ADMIN", interceptURL.getAttribute("access"));

        Node rememberMe = http.getSingle("remember-me");
        Assert.assertNotNull(rememberMe);
    }

}
