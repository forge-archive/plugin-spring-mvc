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

import static org.jvnet.inflector.Noun.pluralOf;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.scaffold.AccessStrategy;
import org.jboss.forge.scaffold.ScaffoldProvider;
import org.jboss.forge.scaffold.TemplateStrategy;
import org.jboss.forge.scaffold.spring.metawidget.config.ForgeConfigReader;
import org.jboss.forge.scaffold.spring.metawidget.widgetbuilder.HtmlAnchor;
import org.jboss.forge.scaffold.util.ScaffoldUtil;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.util.Streams;
import org.jboss.forge.spec.javaee.PersistenceFacet;
import org.jboss.seam.render.TemplateCompiler;
import org.jboss.seam.render.spi.TemplateResolver;
import org.jboss.seam.render.template.CompiledTemplateResource;
import org.jboss.seam.render.template.resolver.ClassLoaderTemplateResolver;
import org.metawidget.statically.StaticUtils.IndentedWriter;
import org.metawidget.statically.javacode.StaticJavaMetawidget;
import org.metawidget.statically.html.widgetbuilder.HtmlTag;
import org.metawidget.statically.spring.StaticSpringMetawidget;
import org.metawidget.util.CollectionUtils;
import org.metawidget.util.XmlUtils;
import org.metawidget.util.simple.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Facet to generate a UI using the Spring JSP taglib.
 * <p>
 * This facet utilizes <a href="http://metawidget.org">Metawidget</a> internally. This enables the use of the Metawidget
 * SPI (pluggable WidgetBuilders, Layouts etc) for customizing the generated User Interface. For more information on
 * writing Metawidget plugins, see <a href="http://metawidget.org/documentation.php">the Metawidget documentation</a>.
 * <p>
 * This Facet does <em>not</em> require Metawidget to be in the final project.
 * 
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

@Alias("spring")
@RequiresFacet({ WebResourceFacet.class,
            PersistenceFacet.class})
public class SpringScaffold extends BaseFacet implements ScaffoldProvider {
    
    //
    // Private statics
    //

    private static String XMLNS_PREFIX = "xmlns:";

    private static final String SPRING_CONTROLLER_TEMPLATE = "scaffold/spring/SpringControllerTemplate.jv";
    private static final String DAO_INTERFACE_TEMPLATE = "scaffold/spring/DaoInterfaceTemplate.jv";
    private static final String DAO_IMPLEMENTATION_TEMPLATE = "scaffold/spring/DaoImplementationTemplate.jv";
    private static final String VIEW_TEMPLATE = "scaffold/spring/view.jsp";
    private static final String CREATE_TEMPLATE = "scaffold/spring/create.jsp";
    private static final String SEARCH_TEMPLATE = "scaffold/spring/search.jsp";
    private static final String NAVIGATION_TEMPLATE = "scaffold/spring/page.jsp";
    
    private static final String ERROR_TEMPLATE = "scaffold/spring/error.jsp";
    private static final String INDEX_TEMPLATE = "scaffold/spring/index.jsp";

    //
    // Protected members (nothing is private, to help sub-classing)
    //

    protected CompiledTemplateResource backingBeanTemplate;
    protected int backingBeanTemplateQbeMetawidgetIndent;

    protected CompiledTemplateResource springControllerTemplate;
    protected CompiledTemplateResource daoInterfaceTemplate;
    protected CompiledTemplateResource daoImplementationTemplate;
    protected CompiledTemplateResource viewTemplate;
    protected Map<String, String> viewTemplateNamespaces;
    protected int viewTemplateEntityMetawidgetIndent;

    protected CompiledTemplateResource createTemplate;
    protected Map<String, String> createTemplateNamespaces;
    protected int createTemplateEntityMetawidgetIndent;

    protected CompiledTemplateResource searchTemplate;
    protected Map<String, String> searchTemplateNamespaces;
    protected int searchTemplateSearchMetawidgetIndent;
    protected int searchTemplateBeanMetawidgetIndent;

    protected CompiledTemplateResource navigationTemplate;
    protected int navigationTemplateIndent;

    protected CompiledTemplateResource errorTemplate;
    protected CompiledTemplateResource indexTemplate;   
    private TemplateResolver<ClassLoader> resolver;
    
    private ShellPrompt prompt;
    private TemplateCompiler compiler;
    private Event<InstallFacets> install;
    private StaticSpringMetawidget entityMetawidget;
    private StaticSpringMetawidget searchMetawidget;
    private StaticSpringMetawidget beanMetawidget;
    private StaticJavaMetawidget qbeMetawidget;
    
    //
    // Constructor
    //
    
    @Inject
    public SpringScaffold(final ShellPrompt prompt,
                    final TemplateCompiler compiler,
                    final Event<InstallFacets> install)
    {
        this.prompt = prompt;
        this.compiler = compiler;
        this.install = install;
        
        this.resolver = new ClassLoaderTemplateResolver(SpringScaffold.class.getClassLoader());
        
        if(this.compiler != null)
        {
            this.compiler.getTemplateResolverFactory().addResolver(this. resolver);
        }
    }
    
    //
    // Public methods
    //

    @Override
    public List<Resource<?>> setup(Resource<?> template, boolean overwrite)
    {
        List<Resource<?>> resources = generateIndex(template, overwrite);
/*        resources.add(updateApplicationContext());*/
        resources.add(setupMVCContext());
        resources.add(updateWebXML());

        return resources;
    }

    /**
     * Overridden to setup the Metawidgets.
     * <p>
     * Metawidgets must be configured per project <em>and per Forge invocation</em>. It is not sufficient to simply
     * configure them in <code>setup</code> because the user may restart Forge and not run <code>scaffold setup</code> a
     * second time.
     */    
    
    @Override
    public void setProject(Project project)
    {
        super.setProject(project);
        
        ForgeConfigReader configReader = new ForgeConfigReader(project);
        
        this.entityMetawidget = new StaticSpringMetawidget();
        this.entityMetawidget.setConfigReader(configReader);
        this.entityMetawidget.setConfig("scaffold/spring/metawidget-entity.xml");
        
        this.searchMetawidget = new StaticSpringMetawidget();
        this.searchMetawidget.setConfigReader(configReader);
        this.searchMetawidget.setConfig("scaffold/spring/metawidget-search.xml");
        
        this.beanMetawidget = new StaticSpringMetawidget();
        this.beanMetawidget.setConfigReader(configReader);
        this.beanMetawidget.setConfig("scaffold/spring/metawidget-bean.xml");
        
        this.qbeMetawidget = new StaticJavaMetawidget();
        this.qbeMetawidget.setConfigReader(configReader);
        this.qbeMetawidget.setConfig("scaffold/spring/metawidget-qbe.xml");
    }

    @Override
    public List<Resource<?>> generateFromEntity(Resource<?> template, JavaClass entity, boolean overwrite)
    {

        // Save the current thread's ContextClassLoader, so that it can be restored later

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        // Track the list of resources generated

        List<Resource<?>> result = new ArrayList<Resource<?>>();

        try {

            // Force the current thread to use the ScaffoldProvider's ContextClassLoader

            Thread.currentThread().setContextClassLoader(SpringScaffold.class.getClassLoader());

            try
            {
                JavaSourceFacet java = this.project.getFacet(JavaSourceFacet.class);
                WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
                MetadataFacet meta = this.project.getFacet(MetadataFacet.class);

                loadTemplates();
                Map<Object, Object> context = CollectionUtils.newHashMap();
                context.put("entity", entity);
                String ccEntity = StringUtils.decapitalize(entity.getName());
                context.put("ccEntity", ccEntity);
                String daoPackage = meta.getTopLevelPackage() + ".repo";
                context.put("daoPackage", daoPackage);

                // Prepare qbeMetawidget

                this.qbeMetawidget.setPath(entity.getQualifiedName());
                StringWriter writer = new StringWriter();
                this.qbeMetawidget.write(writer, backingBeanTemplateQbeMetawidgetIndent);

                context.put("qbeMetawidget", writer.toString().trim());
                context.put("qbeMetawidgetImports",
                        CollectionUtils.toString(this.qbeMetawidget.getImports(), ";\r\n", true, false));

                // Set context for view generation

                context = getTemplateContext(template);
                context.put("entity", entity);
                context.put("entityName", StringUtils.uncamelCase(entity.getName()));
                context.put("ccEntity", ccEntity);
                context.put("daoPackage", daoPackage);

                // Prepare entity metawidget

                this.entityMetawidget.putAttribute("value", ccEntity);
                this.entityMetawidget.setPath(entity.getQualifiedName());
                this.entityMetawidget.setReadOnly(false);
    
                // Generate create
    
                writeEntityMetawidget(context, this.createTemplateEntityMetawidgetIndent, this.createTemplateNamespaces);
    
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views/create" + entity.getName() + ".jsp"),
                        this.createTemplate.render(context), overwrite));
    
                // Generate view
    
                this.entityMetawidget.setReadOnly(true);
                writeEntityMetawidget(context, this.viewTemplateEntityMetawidgetIndent, this.viewTemplateNamespaces);
    
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views/view" + entity.getName() + ".jsp"),
                        this.viewTemplate.render(context), overwrite));
    
                // Generate search - how does it differ between JSF and Spring?
    
                // Generate navigation
    
                result.add(generateNavigation(overwrite));
    
                JavaInterface daoInterface = JavaParser.parse(JavaInterface.class, this.daoInterfaceTemplate.render(context));
                JavaClass daoImplementation = JavaParser.parse(JavaClass.class, this.daoImplementationTemplate.render(context));
    
                // Save the created interface and class implementation, so they can be referenced by the controller.
    
                java.saveJavaSource(daoInterface);
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(daoInterface), daoInterface.toString(), overwrite));
    
                java.saveJavaSource(daoImplementation);
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(daoImplementation), daoImplementation.toString(), overwrite));
                
                String mvcPackage = meta.getTopLevelPackage() + ".mvc";
                context.put("mvcPackage",  mvcPackage);
                context.put("entityPlural", pluralOf(entity.getName().toLowerCase()));
                context.put("entityPluralCap", pluralOf(entity.getName()));
    
                // Create a Spring MVC controller for the passed entity, using SpringControllerTemplate.jv
    
                JavaClass entityController = JavaParser.parse(JavaClass.class, this.springControllerTemplate.render(context));
                java.saveJavaSource(entityController);
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(entityController), entityController.toString(), overwrite));
                
            } catch (Exception e)
            {
                throw new RuntimeException("Error generating Spring scaffolding: " + entity.getName(), e);
            }
        } finally {

            // Restore the original ContextClassLoader

            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")    
    public boolean install()
    {

        if(!(this.project.hasFacet(WebResourceFacet.class) && this.project.hasFacet(PersistenceFacet.class)))
        {
            this.install.fire(new InstallFacets(WebResourceFacet.class, PersistenceFacet.class));
        }
        
        return true;
    }

    @Override
    public boolean isInstalled()
    {
        return true;
    }

    @Override
    public List<Resource<?>> generateIndex(Resource<?> template, boolean overwrite)
    {
        List<Resource<?>> result = new ArrayList<Resource<?>>();
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

        loadTemplates();

//        generateTemplates(overwrite);
        HashMap<Object, Object> context = getTemplateContext(template);

        // Basic pages

/*        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("index.jsp"),
                getClass().getResourceAsStream("/scaffold/spring/index.jsp"), overwrite));*/

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views/index.jsp"),
                this.indexTemplate.render(context), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views/error.jsp"),
                this.errorTemplate.render(context), overwrite));

        // Static resources

        return result;
    }

    @Override
    public List<Resource<?>> generateTemplates(final boolean overwrite)
    {
        List<Resource<?>> result = new ArrayList<Resource<?>>();

        try
        {
            WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

            result.add(ScaffoldUtil.createOrOverwrite(this.prompt,
                    web.getWebResource("/resources/scaffold/paginator.xhtml"),
                    getClass().getResourceAsStream("/resources/scaffold/paginator.xhtml"),
                    overwrite));

            result.add(generateNavigation(overwrite));
        } catch (Exception e)
        {
            throw new RuntimeException("Error generating default templates.", e);
        }

        return result;
    }

    @Override
    public List<Resource<?>> getGeneratedResources()
    {
        throw new RuntimeException("Not yet implemented.");
    }

    @Override
    public AccessStrategy getAccessStrategy()
    {
        // No AccessStrategy required for Spring.

        return null;
    }

    @Override
    public TemplateStrategy getTemplateStrategy()
    {
        return new SpringTemplateStrategy(this.project);
    }

    //
    // Protected methods (nothing is private, to help sub-classing)
    //

/*    protected Resource<?> updateApplicationContext()
    {
        ResourceFacet resources =  this.project.getFacet(ResourceFacet.class);
        MetadataFacet meta = this.project.getFacet(MetadataFacet.class);

        FileResource<?> applicationContext = resources.getResource("META-INF/spring/applicationContext.xml");
        Node beans = XMLParser.parse(applicationContext.getResourceInputStream());
        beans.attribute(XMLNS_PREFIX + "context", "http://www.springframework.org/schema/context");

        // Include the spring-context schema file, so that the <context> namespace can be used in web.xml.

        String schemaLoc = beans.getAttribute("xsi:schemaLocation");
        schemaLoc += " http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd";
        beans.attribute("xsi:schemaLocation", schemaLoc);

        // Use a <context:component-scan> to create beans for all DAO interface implementations, annotated as @Repository

        if (beans.get("context:component-scan").isEmpty())
        {
            Node componentScan = new Node("context:component-scan", beans);
            componentScan.attribute("base-package", meta.getTopLevelPackage() + ".repo");            
        }

        // Save the updated applicationContext.xml file to 'src/main/resources/META-INF/applicationContext.xml'.

        String file = XMLParser.toXMLString(beans);
        return resources.createResource(file.toCharArray(), "META-INF/spring/applicationContext.xml");
    }*/

    protected Resource<?> setupMVCContext()
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
        MetadataFacet meta = this.project.getFacet(MetadataFacet.class);

        // Create an mvc-context.xml file for the web application.

        Node beans = new Node("beans");

        // Add the appropriate schema references.

        beans.attribute("xmlns", "http://www.springframework.org/schema/beans");
        beans.attribute(XMLNS_PREFIX + "xsi", "http://www.w3.org/2001/XMLSchema-instance");
        beans.attribute(XMLNS_PREFIX + "mvc", "http://www.springframework.org/schema/mvc");
        beans.attribute(XMLNS_PREFIX + "context", "http://www.springframework.org/schema/context");

        String schemaLoc = "http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd";
        schemaLoc += " http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd";
        schemaLoc += " http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd";
        beans.attribute("xsi:schemaLocation", schemaLoc);

        // Scan the given package for any classes with MVC annotations.

        String mvcPackage = meta.getTopLevelPackage() + ".mvc";
        Node contextScan = new Node("context:component-scan", beans);
        contextScan.attribute("base-package", mvcPackage);

        // Indicate the use of annotations for Spring MVC, such as @Controller or @RequestMapping
        
        beans.createChild("mvc:annotation-driven");

        // Use the Spring MVC default servlet handler
        
        beans.createChild("mvc:default-servlet-handler");

        // Add an InternalViewResolver, mapping all .jsp pages

        Node viewResolver = new Node("bean", beans);
        viewResolver.attribute("class", "org.springframework.web.servlet.view.InternalResourceViewResolver");
        viewResolver.attribute("id", "viewResolver");

        Node prefixProperty = new Node("property", viewResolver);
        prefixProperty.attribute("name", "prefix");
        prefixProperty.attribute("value", "/WEB-INF/views/");

        Node suffixProperty = new Node("property", viewResolver);
        suffixProperty.attribute("name", "suffix");
        suffixProperty.attribute("value", ".jsp");

        // Add a ViewResolver for any view generated by an error

/*        Node errorViewResolver = new Node("bean", beans);
        errorViewResolver.attribute("class", "org.springframework.web.servlet.handler.SimpleMappingExceptionResolver");
        errorViewResolver.attribute("id", "errorViewResolver");

        Node exceptionProperty = new Node("property", errorViewResolver);
        exceptionProperty.attribute("name", "exceptionMappings");
        Node props = new Node("props", exceptionProperty);
        Node prop = new Node("prop", props);
        prop.attribute("key", "java.lang.Exception");
        prop.text("error");*/

        // Unnecessary if there is no static content, but harmless

        Node mvcStaticContent = new Node("mvc:resources", beans);
        mvcStaticContent.attribute("mapping", "/static/**");
        mvcStaticContent.attribute("location", "/");

        // Write the mvc-context file to 'src/main/webapp/WEB-INF/{lowercase-project-name}-mvc-context.xml'.

        String mvcContextFile = XMLParser.toXMLString(beans);
        String filename = "WEB-INF/" + meta.getProjectName().toLowerCase().replace(' ', '-') + "-mvc-context.xml";
        web.createWebResource(mvcContextFile.toCharArray(), filename);
        
        return web.getWebResource(filename);
    }

    protected Resource<?> updateWebXML()
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
        MetadataFacet meta = this.project.getFacet(MetadataFacet.class);

        String projectName = meta.getProjectName();
        String filename = "/WEB-INF/" + projectName.toLowerCase().replace(' ', '-') + "-mvc-context.xml";

        // Retrieve the existing web.xml file

        FileResource<?> webXML = web.getWebResource("WEB-INF/web.xml");
        Node webapp = XMLParser.parse(webXML.getResourceInputStream());

        // Define a dispatcher servlet, named after the project.

        if (webapp.get("servlet").isEmpty())
        {
            Node servlet = new Node("servlet", webapp);
            String servName = projectName.replace(' ', (char) 0);
            Node servletName = new Node("servlet-name", servlet);
            servletName.text(servName);
            Node servletClass = new Node("servlet-class", servlet);
            servletClass.text("org.springframework.web.servlet.DispatcherServlet");
            Node initParam = new Node("init-param", servlet);
            Node paramName = new Node("param-name", initParam);
            paramName.text("contextConfigLocation");
            Node paramValue = new Node("param-value", initParam);
            paramValue.text(filename);
            Node loadOnStartup = new Node("load-on-startup", servlet);
            loadOnStartup.text(1);            
        }

        // Map the servlet to the '/' URL

        if (webapp.get("servlet-mapping").isEmpty())
        {
            Node servletMapping = new Node("servlet-mapping", webapp);
            Node servletNameRepeat = new Node("servlet-name", servletMapping);
            servletNameRepeat.text(projectName.replace(' ', (char) 0));
            Node url = new Node("url-pattern", servletMapping);
            url.text('/');            
        }

        // Add a unique mapping for the error page

        // TODO: This may need to be refactored later, to allow multiple error page locations.

/*        if (webapp.get("error-page").isEmpty())
        {
            Node errorPage = new Node("error-page", webapp);
            Node exceptionType = new Node("exception-type", errorPage);
            exceptionType.text("java.lang.Exception");
            Node location = new Node("location", errorPage);
            location.text("/WEB-INF/views/error.jsp");            
        }*/

        // Save the updated web.xml file

        String file = XMLParser.toXMLString(webapp);
        web.createWebResource(file.toCharArray(), "WEB-INF/web.xml");
        
        return web.getWebResource("WEB-INF/web.xml");
    }

    protected void loadTemplates()
    {
        // Compile the DAO interface Java template.
        
        if (this.daoInterfaceTemplate == null) {
            this.daoInterfaceTemplate = compiler.compile(DAO_INTERFACE_TEMPLATE);
        }
        
        // Compile the DAO interface implementation Java template.
        
        if (this.daoImplementationTemplate == null) {
            this.daoImplementationTemplate = compiler.compile(DAO_IMPLEMENTATION_TEMPLATE);
        }
        
        // Compile the Spring MVC controller Java template.
        
        if (this.springControllerTemplate == null) {
            this.springControllerTemplate = compiler.compile(SPRING_CONTROLLER_TEMPLATE);
        }

        if (this.viewTemplate == null)
        {
            this.viewTemplate = compiler.compile(VIEW_TEMPLATE);
            String template = Streams.toString(this.viewTemplate.getSourceTemplateResource().getInputStream());
            this.viewTemplateEntityMetawidgetIndent = parseIndent(template, "@{metawidget}");
        }

        if (this.createTemplate == null)
        {
            this.createTemplate = compiler.compile(CREATE_TEMPLATE);
            String template = Streams.toString(this.createTemplate.getSourceTemplateResource().getInputStream());
            this.createTemplateEntityMetawidgetIndent = parseIndent(template, "@{metawidget}");
        }

        if (this.searchTemplate == null)
        {
            this.searchTemplate = compiler.compile(SEARCH_TEMPLATE);
            String template = Streams.toString(this.searchTemplate.getSourceTemplateResource().getInputStream());
            this.searchTemplateSearchMetawidgetIndent = parseIndent(template, "@{searchMetawidget}");
            this.searchTemplateBeanMetawidgetIndent = parseIndent(template, "@{beanMetawidget}");
        }

        if (this.navigationTemplate == null)
        {
            this.navigationTemplate = compiler.compile(NAVIGATION_TEMPLATE);
            String template = Streams.toString(this.navigationTemplate.getSourceTemplateResource().getInputStream());
            this.navigationTemplateIndent = parseIndent(template, "@{navigation}");
        }

        if (this.errorTemplate == null)
        {
            this.errorTemplate = compiler.compile(ERROR_TEMPLATE);
        }

        if (this.indexTemplate == null)
        {
            this.indexTemplate = compiler.compile(INDEX_TEMPLATE);
        }
    }

    protected HashMap<Object, Object> getTemplateContext(final Resource<?> template)
    {
        HashMap<Object, Object> context = new HashMap<Object, Object>();
        context.put("template", template);
        context.put("templateStrategy", getTemplateStrategy());
        return context;
    }

    /**
     * Generates the navigation menu based on scaffolded entities.
     */

    protected Resource<?> generateNavigation(final boolean overwrite)
            throws IOException
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
        HtmlTag unorderedList = new HtmlTag("ul");

        for (Resource<?> resource : web.getWebResource("scaffold").listResources())
        {
            HtmlAnchor link = new HtmlAnchor();
            link.putAttribute("href", "/scaffold/" + resource.getName() + "/search");
            link.setTextContent(StringUtils.uncamelCase(resource.getName()));

            HtmlTag listItem = new HtmlTag("li");
            listItem.getChildren().add(link);
            unorderedList.getChildren().add(listItem);
        }

        Writer writer = new IndentedWriter(new StringWriter(), this.navigationTemplateIndent);
        unorderedList.write(writer);
        Map<Object, Object> context = CollectionUtils.newHashMap();
        context.put("navigation", writer.toString().trim());

        if (this.navigationTemplate == null)
        {
            loadTemplates();
        }

        return ScaffoldUtil.createOrOverwrite(this.prompt, (FileResource<?>) getTemplateStrategy().getDefaultTemplate(),
                this.navigationTemplate.render(context), overwrite);
    }

    /**
     * Parses the given XML and determines what namespaces it already declares. These are later removed from the list of
     * namespaces that Metawidget introduces.
     */

    protected Map<String, String> parseNamespaces(final String template)
    {
        Map<String, String> namespaces = CollectionUtils.newHashMap();
        Document document = XmlUtils.documentFromString(template);
        Element element = document.getDocumentElement();
        NamedNodeMap attributes = element.getAttributes();

        for (int i = 0; i < attributes.getLength(); i++)
        {
            org.w3c.dom.Node node = attributes.item(i);
            String nodeName = node.getNodeName();
            int indexOf = nodeName.indexOf(XMLNS_PREFIX);

            if (indexOf == -1)
            {
                continue;
            }

            namespaces.put(nodeName.substring(indexOf + XMLNS_PREFIX.length()), node.getNodeValue());
        }

        return namespaces;
    }

    /**
     * Parses the given XML and determines the indent of the given String namespaces that Metawidget introduces.
     */

    protected int parseIndent(final String template, final String indentOf)
    {
        int indent = 0;
        int indexOf = template.indexOf(indentOf);

        while ((indexOf > 0) && (template.charAt(indexOf) != '\n'))
        {
            if (template.charAt(indexOf) == '\t')
            {
                indent++;
            }

            indexOf--;
        }

        return indent;
    }

    /**
     * Writes the entity Metawidget and its namespaces into the given context.
     */

    protected void writeEntityMetawidget(final Map<Object, Object> context, final int entityMetawidgetIndent,
            final Map<String, String> existingNamespaces)
    {
        StringWriter writer = new StringWriter();
        this.entityMetawidget.write(writer, entityMetawidgetIndent);
        context.put("metawidget", writer.toString().trim());

        Map<String, String> namespaces = this.entityMetawidget.getNamespaces();

        if (namespaces.keySet() != null && existingNamespaces != null)
        {
            namespaces.keySet().removeAll(existingNamespaces.keySet());
        }

        context.put("metawidgetNamespaces", namespacesToString(namespaces));
    }

    protected String namespacesToString(Map<String, String> namespaces)
    {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, String> entry : namespaces.entrySet())
        {
            // At the start, break out of the current quote. Field must be in quotes so that we're valid XML

            builder.append("\"\r\n\txmlns:");
            builder.append(entry.getKey());
            builder.append("=\"");
            builder.append(entry.getValue());
        }

        return builder.toString();
    }

}
