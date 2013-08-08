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
package org.jboss.forge.spec.spring.security.impl;

import java.io.FileNotFoundException;
import java.util.*;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Facet;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.*;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.project.services.FacetFactory;
import org.jboss.forge.scaffold.ScaffoldProvider;
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
import org.jboss.forge.spec.spring.security.SpringSecurityFacet;
import org.jboss.seam.render.TemplateCompiler;
import org.jboss.seam.render.spi.TemplateResolver;
import org.jboss.seam.render.template.CompiledTemplateResource;
import org.jboss.seam.render.template.resolver.ClassLoaderTemplateResolver;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.FilterDef;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.WebAppDescriptor;
import org.metawidget.util.CollectionUtils;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

@Alias("security")
@RequiresFacet({PersistenceFacet.class,
        WebResourceFacet.class})
public class SpringSecurityPlugin implements Plugin {
    @Inject
    private Project project;

    @Inject
    private Event<InstallFacets> request;

    @Inject
    private TemplateCompiler compiler;

    @Inject
    private ShellPrompt prompt;

    @Inject
    private FacetFactory factory;

    private static final String XMLNS_PREFIX = "xmlns:";

    private TemplateResolver<ClassLoader> resolver;

    @Inject
    public SpringSecurityPlugin(final TemplateCompiler compiler) {
        this.compiler = compiler;
        this.resolver = new ClassLoaderTemplateResolver(SpringSecurityPlugin.class.getClassLoader());

        if (this.compiler != null) {
            this.compiler.getTemplateResolverFactory().addResolver(this.resolver);
        }
    }

    @SetupCommand
    public void setup(@Option(required = false, name = "targetDir", description = "Target Directory") String targetDir) throws FileNotFoundException {
        if (!project.hasFacet(SpringSecurityFacet.class)) {
            request.fire(new InstallFacets(SpringSecurityFacet.class));
        }

        SpringSecurityFacet spring = project.getFacet(SpringSecurityFacet.class);
        MetadataFacet meta = project.getFacet(MetadataFacet.class);

        List<ScaffoldProvider> providers = getScaffoldTypes();
        ScaffoldProvider selectedProvider = null;
        if (providers != null && providers.size() > 1){
            selectedProvider = providers.get(this.prompt.promptChoice("Which type of Scaffolding are you using?", providers));
        } else if (providers != null && providers.size() == 1){
            selectedProvider = providers.get(0);
        } else{
            boolean continuing = this.prompt.promptBoolean("There is no scaffolding plugin installed are sure you want to continue installing spring security?", false);
            if (!continuing){
                return;
            }
        }


        if (spring.install()) {
            System.out.println("Sucessfully installed spring security");
        }

        if (targetDir == null) {
            targetDir = new String();
        }

        targetDir = processTargetDir(targetDir);
        spring.setTargetDir(targetDir);

        String securityContext;
        if (targetDir.isEmpty()) {
            securityContext = "/WEB-INF/"
                    + meta.getProjectName().replace(' ', '-').toLowerCase()
                    + "-security-context.xml";
        } else {
            if (!targetDir.endsWith("/")) {
                targetDir += "/";
            }
            securityContext = "/WEB-INF/" + targetDir + meta.getProjectName().replace(' ', '-').toLowerCase()
                    + "-security-context.xml";
        }
        generateSecurity(securityContext, selectedProvider);
        updateWebXML(securityContext);
    }

    @Command("add-user")
    public void addUser(final PipeOut out) {
        SpringSecurityFacet spring = project.getFacet(SpringSecurityFacet.class);
        MetadataFacet meta = project.getFacet(MetadataFacet.class);
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        Node beans = XMLParser.parse(spring.getSecurityContextFile(spring.getTargetDir()).getResourceInputStream());
        if (!hasChildNamed(beans, "user-service")) {
            ShellMessages.error(out, "No Embedded Database installed, when setting up spring security please select embedded security to be able to add users");
        } else {
            String username = this.prompt.prompt("Enter Username for new user");
            String password = this.prompt.promptSecret("Enter Password for new user");
            String role = this.prompt.prompt("What role does this user have?", "ROLE_ADMIN");
            Node userService = beans.getSingle("user-service");
            userService.createChild("user").attribute("name", username).attribute("password", password).attribute("authorities", role);
            String securityContext = "/WEB-INF/" + spring.getTargetDir() + meta.getProjectName().replace(' ', '-').toLowerCase()
                    + "-security-context.xml";
            web.createWebResource(XMLParser.toXMLString(beans), securityContext);
        }

    }

    @Command("remove-user")
    public void removeUser(final PipeOut out) {
        SpringSecurityFacet spring = project.getFacet(SpringSecurityFacet.class);
        MetadataFacet meta = project.getFacet(MetadataFacet.class);
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        Node beans = XMLParser.parse(spring.getSecurityContextFile(spring.getTargetDir()).getResourceInputStream());
        if (!hasChildNamed(beans, "user-service")) {
            ShellMessages.error(out, "No Embedded Database installed, when setting up spring security please select embedded security to be able to add users");
        } else {
            String username = this.prompt.prompt("Enter Username of the user to delete");
            Node userService = beans.getSingle("user-service");
            Node toDelete = null;
            for(Node children: userService.getChildren()){
                if(children.getName().equals("user") && children.getAttribute("name").equals(username)){
                    toDelete = children;
                    break;
                }
            }
            userService.removeChild(toDelete);
            String securityContext = "/WEB-INF/" + spring.getTargetDir() + meta.getProjectName().replace(' ', '-').toLowerCase()
                    + "-security-context.xml";
            web.createWebResource(XMLParser.toXMLString(beans), securityContext);
        }

    }


    private void generateSecurity(String securityContext, ScaffoldProvider selectedProvider) throws FileNotFoundException {

        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        Node beans;

        if (!web.getWebResource(securityContext).exists()) {
            beans = new Node("beans:beans");
            beans.attribute("xsi:schemaLocation", "http://www.springframework.org/schema/beans\nhttp://www.springframework.org/schema/security\n"
                    + "http//www.springframework.org/schema/security/spring-security-3.0.xsd");
        } else {
            beans = XMLParser.parse(web.getWebResource(securityContext).getResourceInputStream());
        }

        beans = addXMLSchemaSecurity(beans, false);
        if (!hasChildNamed(beans, "http")) {
            Node http = new Node("http", beans);
            http.attribute("auto-config", "true");
            http.createChild("intercept-url").attribute("pattern", "/**/create*")
                    .attribute("access", "ROLE_ADMIN");
            http.createChild("intercept-url").attribute("pattern", "/**/edit*")
                    .attribute("access", "ROLE_ADMIN");
            http.createChild("remember-me");
            if (selectedProvider != null && selectedProvider.getClass().getCanonicalName().contains("Faces")){
                setupSecurityForJSF(http, beans);
            }
        }
        List<String> possibleAuthenciationTechniques = new ArrayList<String>();
        possibleAuthenciationTechniques.add("Embedded");
        if (project.hasFacet(PersistenceFacet.class)) {
            possibleAuthenciationTechniques.add("JDBC");
        }
        possibleAuthenciationTechniques.add("LDAP");
        int authenciationMethod = this.prompt.promptChoice("Type of User Authenciation", possibleAuthenciationTechniques);
        switch (authenciationMethod) {
            case 0:
                if (!hasChildNamed(beans, "user-service")) {
                    Node userService = new Node("user-service", beans);
                    userService.attribute("id", "userService");
                    String username = this.prompt.prompt("Admin User Name?", "admin");
                    String password = this.prompt.promptSecret("Admin Password?", "adminPass");
                    userService.createChild("user").attribute("name", username).attribute("password", password).attribute("authorities", "ROLE_ADMIN");
                }
                break;
            case 1:
                if (!hasChildNamed(beans, "jdbc-user-service")) {
                    Node userService = new Node("jdbc-user-service", beans);
                    userService.attribute("id", "userService");
                    String dataSourceBean = this.prompt.prompt("JDBC Data Source Reference Bean?", "dataSource");
                    userService.attribute("data-source-ref", dataSourceBean);
                }
                break;
            case 2:
                if (!hasChildNamed(beans, "ldap-user-service")) {
                    Node userService = new Node("ldap-user-service", beans);
                    userService.attribute("id", "userService");
                    userService.attribute("user-search-filter", "(uid={0})");
                    userService.attribute("group-search-filter", "member={0}");
                }
                if (!hasChildNamed(beans, "ldap-server")) {
                    Node ldapServer = new Node("ldap-server", beans);
                    String urlOrLDIF = this.prompt.prompt("Enter url to remote LDAP server or ldif file on classpath");
                    if (urlOrLDIF.endsWith("ldif")) {
                        ldapServer.attribute("ldif", urlOrLDIF);
                    } else {
                        ldapServer.attribute("url", urlOrLDIF);
                    }
                }
                break;
            default:
                break;
        }
        if (!hasChildNamed(beans, "authentication-manager")) {
            Node authentiation = new Node("authentication-manager", beans);
            authentiation.createChild("authentication-provider").attribute("user-service-ref", "userService");
        }
        web.createWebResource(XMLParser.toXMLString(beans), securityContext);

    }

    private Node addXMLSchemaSecurity(Node beans, boolean b) {
        beans.attribute(XMLNS_PREFIX + "beans", "http://www.springframework.org/schema/beans");
        beans.attribute("xmlns", "http://www.springframework.org/schema/security");
        beans.attribute(XMLNS_PREFIX + "xsi", "http://www.w3.org/2001/XMLSchema-instance");

        String schemaLocation = beans.getAttribute("");
        schemaLocation = (schemaLocation == null) ? new String() : schemaLocation;

        if (!schemaLocation.contains("http://www.springframework.org/schema/beans " +
                "http://www.springframework.org/schema/beans/spring-beans.xsd")) {
            schemaLocation += " http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd";
        }

        if (!schemaLocation.contains("http://www.springframework.org/schema/security " +
                "http://www.springframework.org/schema/security/spring-security-3.0.xsd")) {
            schemaLocation += " http://www.springframework.org/schema/security http://www.springframework.org/schema/security/spring-security.xsd";
        }
        beans.attribute("xsi:schemaLocation", schemaLocation);

        return beans;
    }

    private boolean hasChildNamed(Node beans, String string) {
        for (Node child : beans.getChildren()) {
            if (child.getName() != null && child.getName().equals(string)) {
                return true;
            }
        }
        return false;
    }

    protected void updateWebXML(String targetDir) throws FileNotFoundException {
        ServletFacet servlet = project.getFacet(ServletFacet.class);

        WebAppDescriptor webXML = servlet.getConfig();
        MetadataFacet meta = project.getFacet(MetadataFacet.class);
        if (webXML == null) {
            Node webapp = new Node("webapp");
            WebResourceFacet web = project.getFacet(WebResourceFacet.class);
            web.createWebResource(XMLParser.toXMLString(webapp), "WEB-INF/web.xml");
            webXML = servlet.getConfig();
        }

        // If the application already has a name, prompt the user to change it to the project name

        if (webXML.getDisplayName() != null) {
            if (this.prompt.promptBoolean("Change the application's display name to " + meta.getProjectName() + "?")) {
                webXML = webXML.displayName(meta.getProjectName());
            }
        } else {
            webXML = webXML.displayName(meta.getProjectName());
        }

        // Add the application context file to web.xml's <context-param>

        if (webXML.getContextParam("contextConfigLocation") == null) {
            webXML = webXML.contextParam("contextConfigLocation", targetDir);
        } else {
            String contextConfigLocation = webXML.getContextParam("contextConfigLocation");

            if (!contextConfigLocation.contains(targetDir)) {
                contextConfigLocation += ", " + targetDir;
                webXML = webXML.contextParam("contextConfigLocation", contextConfigLocation);
            }
        }
        if (!webXML.getListeners().contains("org.springframework.web.context.ContextLoaderListener"))
        {
            webXML = webXML.listener("org.springframework.web.context.ContextLoaderListener");
        }


        //Add security filter if asked for one
        webXML = addSecurity(targetDir, webXML);
        // Add to context param if not there
        if (targetDir.startsWith("/")) {
            targetDir = targetDir.substring(1);
        }

        servlet.saveConfig(webXML);
    }

    private void setupSecurityForJSF(Node http, Node beans) throws FileNotFoundException {
        http.createChild("form-login").attribute("always-use-default-target", "true");
    }

    private WebAppDescriptor addSecurity(String targetDir,
                                         WebAppDescriptor webXML) {
        String security = new String();
        if (targetDir.contains("-security-context.xml")) {
            for (FilterDef filter : webXML.getFilters()) {
                if (filter.getFilterClass().contains("org.springframework.web.filter.DelegatingFilterProxy")) {
                    security = filter.getFilterClass();
                    break;
                }
            }
        }
        if (security.isEmpty() && targetDir.contains("-security-context.xml")) {
            webXML = webXML.filter("springSecurityFilterChain", "org.springframework.web.filter.DelegatingFilterProxy", new String[]{"/*"});
        }
        return webXML;
    }

    private String processTargetDir(String targetDir) {
        targetDir = (targetDir.startsWith("/")) ? targetDir.substring(1) : targetDir;
        targetDir = (targetDir.endsWith("/")) ? targetDir.substring(0, targetDir.length() - 1) : targetDir;

        return targetDir;
    }

    @SuppressWarnings("unchecked")
    private List<ScaffoldProvider> getScaffoldTypes()
    {
        ScaffoldProvider scaffoldImpl = null;

        Collection<Facet> facets = project.getFacets();
        List<ScaffoldProvider> detectedScaffolds = new ArrayList<ScaffoldProvider>();
        for (Facet facet : facets)
        {
            if (facet instanceof ScaffoldProvider)
            {
                detectedScaffolds.add((ScaffoldProvider) facet);
            }
        }

        return  detectedScaffolds;
    }
}
