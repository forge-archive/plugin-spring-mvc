package org.jboss.forge.spec.spring.mvc.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;

import javax.inject.Inject;

import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.spec.javaee.ServletFacet;
import org.jboss.forge.spec.javaee.util.ServletUtil;
import org.jboss.forge.spec.spring.mvc.SpringFacet;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.ServletDef;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.ServletMappingDef;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.WebAppDescriptor;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

@Alias("forge.spec.spring")
@RequiresFacet({ DependencyFacet.class, ServletFacet.class })
public class SpringFacetImpl extends BaseFacet implements SpringFacet
{
    public static final Dependency JAVAEE6 = DependencyBuilder.create("org.jboss.spec:jboss-javaee-6.0").setScopeType(ScopeType.IMPORT)
            .setPackagingType(PackagingType.BASIC);

    private final DependencyInstaller installer;

    private ServletMappingHelper servletMappingHelper = new ServletMappingHelper();

    private static final String SPRING_VERSION = "3.1.1.RELEASE";

    private static final Dependency SPRING_ASM = DependencyBuilder.create("org.springframework:spring-asm:${spring.version}");

    private static final Dependency SPRING_BEANS = DependencyBuilder.create("org.springframework:spring-beans:${spring.version}");

    private static final Dependency SPRING_CONTEXT = DependencyBuilder.create("org.springframework:spring-context:${spring.version}");

    private static final Dependency SPRING_CONTEXT_SUPPORT = DependencyBuilder.create("org.springframework:spring-context-support:${spring.version}");

    private static final Dependency SPRING_CORE = DependencyBuilder.create("org.springframework:spring-core:${spring.version}");

    private static final Dependency SPRING_EXPRESSION = DependencyBuilder.create("org.springframework:spring-expression:${spring.version}");

    private static final Dependency SPRING_ORM = DependencyBuilder.create("org.springframework:spring-orm:${spring.version}");

    private static final Dependency SPRING_TX = DependencyBuilder.create("org.springframework:spring-tx:${spring.version}");

    private static final Dependency SPRING_WEB = DependencyBuilder.create("org.springframework:spring-web:${spring.version}");

    private static final Dependency SPRING_WEB_MVC = DependencyBuilder.create("org.springframework:spring-webmvc:${spring.version}");

    @Inject
    public SpringFacetImpl(final DependencyInstaller installer)
    {
        this.installer = installer;
    }

    @Override
    public boolean isInstalled()
    {
        String version = project.getFacet(ServletFacet.class).getConfig().getVersion();

        if (!version.trim().startsWith("3"))
        {
            return false;
        }

        DependencyFacet deps = project.getFacet(DependencyFacet.class);

        for (Dependency requirement : getRequiredDependencies())
        {
            if(!deps.hasEffectiveDependency(requirement))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean install()
    {
        for (Dependency requirement : getRequiredDependencies())
        {
            if (!this.installer.isInstalled(project, requirement))
            {
                DependencyFacet deps = project.getFacet(DependencyFacet.class);

                if (!deps.hasEffectiveDependency(requirement) && !deps.hasDirectManagedDependency(JAVAEE6))
                {
                    this.installer.installManaged(project, JAVAEE6);
                }

                if (requirement.getGroupId().equals("org.springframework"))
                {
                    deps.setProperty("spring.version", SPRING_VERSION);
                }

                this.installer.install(project, requirement);
            }
        }

        return true;
    }

    protected List<Dependency> getRequiredDependencies()
    {
        return Arrays.asList(JAVAEE6, SPRING_ASM, SPRING_BEANS, SPRING_CONTEXT, SPRING_CONTEXT_SUPPORT,
                SPRING_CORE, SPRING_EXPRESSION, SPRING_ORM, SPRING_TX, SPRING_WEB, SPRING_WEB_MVC);        
    }

    /*
     * Facet Methods
     */

    @Override
    public FileResource<?> getContextFile()
    {
        ResourceFacet resources = project.getFacet(ResourceFacet.class);

        return resources.getResource("META-INF/spring/applicationContext.xml");
    }

    @Override
    public FileResource<?> getMvcContextFile(String targetDir)
    {
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        if (targetDir.equals("/") || targetDir.isEmpty())
        {
            MetadataFacet meta = project.getFacet(MetadataFacet.class);

            return web.getWebResource("WEB-INF/" + meta.getProjectName().replace(' ', '-').toLowerCase() + "-mvc-context.xml");
        }
        else
        {
            while (targetDir.startsWith("/"))
            {
                targetDir = targetDir.substring(1);
            }

            while (targetDir.endsWith("/"))
            {
                targetDir = targetDir.substring(0, targetDir.length()-1);
            }

            String filename = "WEB-INF/" + targetDir.replace('/', '-').toLowerCase() + "-mvc-context.xml";

            return web.getWebResource(filename);
        }
    }

    @Override
    public List<String> getSpringServletMappings()
    {
        ServletFacet facet = project.getFacet(ServletFacet.class);
        WebAppDescriptor webXml = facet.getConfig();
        return getExplicitSpringServletMappings(webXml);
    }

    private List<String> getExplicitSpringServletMappings(final WebAppDescriptor webXml)
    {
        List<ServletDef> servlets = webXml.getServlets();
        List<String> results = new ArrayList<String>();

        for (ServletDef servlet : servlets)
        {
            if (servlet.getClass().getName().startsWith("org.springframework.web.servlet"))
            {
                List<ServletMappingDef> mappings = servlet.getMappings();

                for (ServletMappingDef mapping : mappings)
                {
                    results.addAll(mapping.getUrlPatterns());
                }
            }
        }

        return results;
    }

    // Adds the servlet mapping to the first Spring servlet that is found - implementation could be improved.

    @Override
    public void setSpringMapping(String mapping)
    {
        ServletFacet facet = project.getFacet(ServletFacet.class);
        InputStream webXml = facet.getConfigFile().getResourceInputStream();
        InputStream newWebXml = servletMappingHelper.addSpringServletMapping(webXml, mapping);

        if (webXml != newWebXml)
        {
            facet.getConfigFile().setContents(newWebXml);
        }
    }

    @Override
    public List<String> getWebPaths(final Resource<?> r)
    {
        if (r != null)
        {
            WebResourceFacet web = project.getFacet(WebResourceFacet.class);

            List<DirectoryResource> webRootDirectories = web.getWebRootDirectories();

            for (DirectoryResource d : webRootDirectories)
            {
                if (r.getFullyQualifiedName().startsWith(d.getFullyQualifiedName()))
                {
                    String path = r.getFullyQualifiedName().substring(d.getFullyQualifiedName().length());
                    return getWebPaths(path);
                }
            }
        }

        return new ArrayList<String>();
    }

    @Override
    public List<String> getWebPaths(final String path)
    {
        List<String> results = new ArrayList<String>();

        if (getResourceForWebPath(path) == null)
        {
            List<String> mappings = getSpringServletMappings();

            for (String mapping : mappings)
            {
                String viewId = buildSpringViewId(mapping, path);

                if (!results.contains(viewId))
                {
                    results.add(viewId);
                }
            }
        }

        return results;
    }

    @Override
    public Resource<?> getResourceForWebPath(String path)
    {
        if (path != null)
        {
            WebResourceFacet web = project.getFacet(WebResourceFacet.class);
            List<DirectoryResource> webRootDirectories = web.getWebRootDirectories();

            boolean matches = false;

            for (String mapping : getSpringServletMappings())
            {
                Matcher matcher = ServletUtil.mappingToRegex(mapping).matcher(path);

                if (matcher.matches())
                {
                    path = matcher.group(1);
                    matches = true;
                    break;
                }

                while (path.startsWith("/"))
                {
                    path = path.substring(1);
                }

                if (!matches)
                {
                    return null;
                }

                List<String> strings = Arrays.asList(path.split("/"));

                for (DirectoryResource d : webRootDirectories)
                {
                    Queue<String> queue = new LinkedList<String>();
                    queue.addAll(strings);

                    Resource<?> temp = d;

                    while (queue.size() > 1)
                    {
                        Resource<?> child = temp.getChild(queue.remove());

                        if (child != null && child.exists())
                        {
                            temp = child;
                        }
                        else
                        {
                            break;
                        }

                        if (queue.isEmpty())
                        {
                            return child;
                        }
                    }

                    if (temp != null)
                    {
                        String name = queue.remove();
                        Resource<?> child = null;

                        if (name.endsWith(".jsp"))
                        {
                            child = temp.getChild(name);
                        }
                        else
                        {
                            child = temp.getChild(name + ".jsp");
                        }

                        if ((child != null) && child.exists())
                        {
                            return child;
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public boolean hasServlet(String servletName)
    {
        ServletFacet serv = project.getFacet(ServletFacet.class);
        WebAppDescriptor webXml = serv.getConfig();

        for (ServletDef servlet : webXml.getServlets())
        {
            if (servlet.getName().equals(servletName))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Build a Spring view ID for the given resource path, assumes Spring servlet mappings begin with either '/' or '.'
     */

    private String buildSpringViewId(String mapping, String path)
    {
        if (mapping.startsWith("/"))
        {
            return mapping + path;
        }
        else
        {
            return path + mapping;
        }
    }

    public static class ServletMappingHelper
    {
        public static final String SPRING_SERVLET_PACKAGE = "org.springframework.web.servlet";

        public static final String SPRING_DISPATCHER_SERVLET = "org.springframework.web.servlet.DispatcherServlet";

        public InputStream addSpringServletMapping(final InputStream webXmlStream, final String mapping)
        {
            Node root = XMLParser.parse(webXmlStream);
            Node servlet = getOrCreateSpringServlet(root);
            createMappingIfNotExists(root, servlet, mapping);

            return XMLParser.toXMLInputStream(root);
        }

        public Node getOrCreateSpringServlet(Node root)
        {
            List<Node> servlets = root.get("servlet");

            for (Node servlet : servlets)
            {
                if (servlet.getSingle("servlet-class").getText().startsWith(SPRING_SERVLET_PACKAGE))
                {
                    return servlet;
                }
            }

            Node servlet = root.createChild("servlet");
            servlet.createChild("servlet-name").text("Spring Servlet");
            servlet.createChild("servlet-class").text(SPRING_DISPATCHER_SERVLET);
            servlet.createChild("load-on-startup").text("1");

            return servlet;
        }

        private boolean createMappingIfNotExists(Node root, Node servlet, String mapping)
        {
            List<Node> servletMappings = root.get("servlet-mapping");
            Node servletMappingNode = null;

            String servletName = servlet.getSingle("servlet-name").getText();

            for (Node servletMapping : servletMappings)
            {
                if (servletName.equals(servletMapping.getSingle("servlet-name").getText()))
                {
                    servletMappingNode = servletMapping;
                    List<Node> urlPatterns = servletMapping.get("url-pattern");

                    for (Node urlPattern : urlPatterns)
                    {
                        if (mapping.equals(urlPattern.getText()))
                        {
                            // Servlet mapping already exists, do not create one.

                            return false;
                        }
                    }
                }
            }

            // Mapping does not exist, create it and add the URL pattern.

            if (servletMappingNode == null)
            {
                servletMappingNode = root.createChild("servlet-mapping");
                servletMappingNode.createChild("servlet-name").text(servletName);
            }

            servletMappingNode.createChild("url-pattern").text(mapping);

            return true;
        }
    }
}