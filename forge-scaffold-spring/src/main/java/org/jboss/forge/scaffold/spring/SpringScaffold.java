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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.jboss.forge.env.Configuration;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.Import;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.ResourceFilter;
import org.jboss.forge.scaffold.AccessStrategy;
import org.jboss.forge.scaffold.ScaffoldProvider;
import org.jboss.forge.scaffold.TemplateStrategy;
import org.jboss.forge.scaffold.spring.metawidget.config.ForgeConfigReader;
import org.jboss.forge.scaffold.spring.metawidget.widgetbuilder.HtmlAnchor;
import org.jboss.forge.scaffold.util.ScaffoldUtil;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.util.Streams;
import org.jboss.forge.spec.javaee.PersistenceFacet;
import org.jboss.seam.render.TemplateCompiler;
import org.jboss.seam.render.spi.TemplateResolver;
import org.jboss.seam.render.template.CompiledTemplateResource;
import org.jboss.seam.render.template.resolver.ClassLoaderTemplateResolver;
import org.jboss.shrinkwrap.descriptor.api.spec.jpa.persistence.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.spec.jpa.persistence.PersistenceUnitDef;
import org.metawidget.statically.StaticUtils.IndentedWriter;
import org.metawidget.statically.javacode.StaticJavaMetawidget;
import org.metawidget.statically.jsp.StaticJspMetawidget;
import org.metawidget.statically.jsp.StaticJspUtils;
import org.metawidget.statically.html.widgetbuilder.HtmlTag;
import org.metawidget.statically.spring.StaticSpringMetawidget;
import org.metawidget.util.CollectionUtils;
import org.metawidget.util.simple.StringUtils;

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
@Help("Spring MVC scaffolding")
@RequiresFacet({ DependencyFacet.class,
            WebResourceFacet.class,
            PersistenceFacet.class})
public class SpringScaffold extends BaseFacet implements ScaffoldProvider
{
    
    //
    // Private statics
    //

    private static final String APPLICATION_CONTEXT_TEMPLATE = "scaffold/spring/applicationContext.xl";
    private static final String MVC_CONTEXT_TEMPLATE = "scaffold/spring/mvc-context.xl";
    private static final String WEB_XML_TEMPLATE = "scaffold/spring/web.xl";

    private static final String INDEX_CONTROLLER_TEMPLATE = "scaffold/spring/IndexControllerTemplate.jv";
    private static final String SPRING_CONTROLLER_TEMPLATE = "scaffold/spring/SpringControllerTemplate.jv";
    private static final String DAO_INTERFACE_TEMPLATE = "scaffold/spring/DaoInterfaceTemplate.jv";
    private static final String DAO_IMPLEMENTATION_TEMPLATE = "scaffold/spring/DaoImplementationTemplate.jv";

    private static final String ENTITY_CONVERTER_TEMPLATE = "scaffold/spring/EntityConverterTemplate.jv";
    private static final String CONVERSION_SERVICE_TEMPLATE = "scaffold/spring/ConversionServiceTemplate.jv";

    private static final String VIEW_TEMPLATE = "scaffold/spring/view.jsp";
    private static final String VIEW_ALL_TEMPLATE = "scaffold/spring/search.jsp";
    private static final String UPDATE_TEMPLATE = "scaffold/spring/update.jsp";
    private static final String CREATE_TEMPLATE = "scaffold/spring/create.jsp";
    private static final String NAVIGATION_TEMPLATE = "scaffold/spring/pageTemplate.jsp";
    
    private static final String ERROR_TEMPLATE = "scaffold/spring/error.jsp";
    private static final String INDEX_TEMPLATE = "scaffold/spring/index.jsp";

    //
    // Protected members (nothing is private, to help sub-classing)
    //

    protected int backingBeanTemplateQbeMetawidgetIndent;

    protected CompiledTemplateResource applicationContextTemplate;
    protected CompiledTemplateResource mvcContextTemplate;
    protected CompiledTemplateResource webXMLTemplate;

    protected CompiledTemplateResource indexControllerTemplate;
    protected CompiledTemplateResource springControllerTemplate;
    protected CompiledTemplateResource daoInterfaceTemplate;
    protected CompiledTemplateResource daoImplementationTemplate;

    protected CompiledTemplateResource entityConverterTemplate;
    protected CompiledTemplateResource conversionServiceTemplate;

    protected CompiledTemplateResource searchTemplate;
    protected int searchTemplateMetawidgetIndent;
    protected int searchMetawidgetIndent;
    protected int headerMetawidgetIndent;

    protected CompiledTemplateResource viewTemplate;
    protected int viewTemplateMetawidgetIndent;

    protected CompiledTemplateResource updateTemplate;
    protected int updateTemplateEntityMetawidgetIndent;

    protected CompiledTemplateResource createTemplate;
    protected int createTemplateEntityMetawidgetIndent;

    protected CompiledTemplateResource navigationTemplate;
    protected int navigationTemplateIndent;

    protected CompiledTemplateResource errorTemplate;
    protected CompiledTemplateResource indexTemplate;   

    protected StaticSpringMetawidget entityMetawidget;
    protected StaticJspMetawidget headerMetawidget;
    protected StaticJspMetawidget searchMetawidget;
    protected StaticJavaMetawidget qbeMetawidget;

    protected TemplateResolver<ClassLoader> resolver;
    protected ShellPrompt prompt;
    protected TemplateCompiler compiler;
    protected Event<InstallFacets> install;

    private Configuration config;

    //
    // Constructor
    //
    
    @Inject
    public SpringScaffold(final Configuration config,
                    final ShellPrompt prompt,
                    final TemplateCompiler compiler,
                    final Event<InstallFacets> install) {
        this.config = config;
        this.prompt = prompt;
        this.compiler = compiler;
        this.install = install;
        
        this.resolver = new ClassLoaderTemplateResolver(SpringScaffold.class.getClassLoader());
        
        if(this.compiler != null) {
            this.compiler.getTemplateResolverFactory().addResolver(this.resolver);
        }
    }
    
    //
    // Public methods
    //

    @Override
    public List<Resource<?>> setup(String targetDir, Resource<?> template, boolean overwrite)
    {
        DependencyFacet deps = this.project.getFacet(DependencyFacet.class);
        MetadataFacet meta = this.project.getFacet(MetadataFacet.class);
        PersistenceFacet persistence = this.project.getFacet(PersistenceFacet.class);
        ResourceFacet resources = this.project.getFacet(ResourceFacet.class);
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

        // Use the Forge DependencyFacet to add Spring dependencies to the POM

        String springVersion = "3.1.1.RELEASE";

        deps.setProperty("spring.version", springVersion);

        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-asm:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-beans:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-context:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-context-support:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-core:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-expression:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-tx:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-web:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-webmvc:${spring.version}"));
        deps.addDirectDependency(DependencyBuilder.create("org.springframework:spring-orm:${spring.version}"));
 
        Map<Object, Object> context = CollectionUtils.newHashMap();
        context.put("targetDir", targetDir);

        if (!targetDir.startsWith("/"))
            targetDir = "/" + targetDir;

        if (!targetDir.endsWith("/"))
            targetDir += "/";

        context.put("topLevelPackage", meta.getTopLevelPackage());

        PersistenceDescriptor descriptor = persistence.getConfig();

        // Use the first persistence unit found by default

        PersistenceUnitDef defaultUnit = descriptor.listUnits().get(0);
        context.put("persistenceUnit", defaultUnit.getName());

        // Add the JNDI name of the EntityManagerFactory to persistence.xml

        defaultUnit.property("jboss.entity.manager.factory.jndi.name", "java:jboss/" + defaultUnit.getName() + "/persistence");
        persistence.saveConfig(descriptor);

        List<Resource<?>> result = generateIndex(targetDir, template, overwrite);

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, resources.getResource("META-INF/spring/applicationContext.xml"), 
                this.applicationContextTemplate.render(context), overwrite));

        String filename = "-mvc-context.xml";

        if (!targetDir.equals("/"))
        {
            filename = "WEB-INF/" + targetDir.substring(1, targetDir.length()-1).replace('/', '-').toLowerCase() + filename;
        }
        else
        {
            filename = "WEB-INF/" + meta.getProjectName().replace(' ', '-').toLowerCase() + filename;
        }

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource(filename),
                this.mvcContextTemplate.render(context), overwrite));

        context.put("projectName", meta.getProjectName());

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/web.xml"),
                this.webXMLTemplate.render(context), overwrite));

        result.add(setupTilesLayout(targetDir));

        deps.addDirectDependency(DependencyBuilder.create("org.jboss.spec.javax.servlet:jboss-servlet-api_3.0_spec"));
        deps.addDirectDependency(DependencyBuilder.create("org.apache.tiles:tiles-jsp:2.1.3"));

        return result;
    }

    /**
     * Overridden to setup the Metawidgets.
     * <p>
     * Metawidgets must be configured per project <em>and per Forge invocation</em>. It is not sufficient to simply
     * configure them in <code>setup</code> because the user may restart Forge and not run <code>scaffold setup</code> a
     * second time.
     */    
    
    @Override
    public void setProject(Project project) {
        super.setProject(project);
        
        ForgeConfigReader configReader = new ForgeConfigReader(this.config, this.project);
        
        this.entityMetawidget = new StaticSpringMetawidget();
        this.entityMetawidget.setConfigReader(configReader);
        this.entityMetawidget.setConfig("scaffold/spring/metawidget-entity.xml");

        this.headerMetawidget = new StaticJspMetawidget();
        this.headerMetawidget.setConfigReader(configReader);
        this.headerMetawidget.setConfig("scaffold/spring/metawidget-header.xml");

        this.searchMetawidget = new StaticJspMetawidget();
        this.searchMetawidget.setConfigReader(configReader);
        this.searchMetawidget.setConfig("scaffold/spring/metawidget-search.xml");

        this.qbeMetawidget = new StaticJavaMetawidget();
        this.qbeMetawidget.setConfigReader(configReader);
        this.qbeMetawidget.setConfig("scaffold/spring/metawidget-qbe.xml");
    }

    @Override
    public List<Resource<?>> generateFromEntity(String targetDir, Resource<?> template, JavaClass entity, boolean overwrite)
    {

        // Save the current thread's ContextClassLoader, so that it can be restored later

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        // Track the list of resources generated

        List<Resource<?>> result = new ArrayList<Resource<?>>();

        try
        {

            // Force the current thread to use the ScaffoldProvider's ContextClassLoader

            Thread.currentThread().setContextClassLoader(SpringScaffold.class.getClassLoader());

            try
            {
                JavaSourceFacet java = this.project.getFacet(JavaSourceFacet.class);
                WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
                MetadataFacet meta = this.project.getFacet(MetadataFacet.class);

                loadTemplates();

                // Set context for Java and JSP generation

                Map<Object, Object> context = CollectionUtils.newHashMap();
                context.put("entity", entity);
                String ccEntity = StringUtils.decapitalize(entity.getName());
                context.put("ccEntity", ccEntity);

                // TODO: Support multiple packages for controllers and DAOs.

                context.put("daoPackage", meta.getTopLevelPackage() + ".repo");
                context.put("mvcPackage", meta.getTopLevelPackage() + ".mvc");
                findEntityRelationships(entity, context);

                if (!targetDir.startsWith("/"))
                    targetDir = "/" + targetDir;

                if (!targetDir.endsWith("/"))
                    targetDir += "/";

                context.put("targetDir", targetDir);
                context.put("entityName", StringUtils.uncamelCase(entity.getName()));
                String entityPlural = pluralOf(entity.getName());
                context.put("entityPlural", entityPlural);
                context.put("entityPluralName", pluralOf(StringUtils.uncamelCase(entity.getName())));

                // Prepare entity metawidget

                this.entityMetawidget.putAttribute("value", ccEntity);
                this.entityMetawidget.setPath(entity.getQualifiedName());
                this.entityMetawidget.setReadOnly(false);

                // Create a views.xml file containing all tiles definitions.

                Resource<?> viewsXML = web.getWebResource("WEB-INF/views/views.xml");
                Node definitions = new Node("tiles-definitions");

                if (viewsXML.exists())
                    definitions = XMLParser.parse(viewsXML.getResourceInputStream());

                String tile = targetDir.equals("/") ? "standard" : targetDir.substring(1, targetDir.length()-1);

                // Add index page(s)

                if (!targetDir.equals("/"))
                {
                    addViewDefinition("standard", "/index", "Weclome to Forge", "Welcome to Forge", "Your application is running.",
                            "/WEB-INF/views/index.jsp", definitions);
                }

                addViewDefinition(tile, targetDir + "index", "Welcome to Forge", "Welcome to Forge", "Your application is running.",
                        "/WEB-INF/views" + targetDir + "index.jsp", definitions);

                // Add error page

                addViewDefinition("standard", "/error", "Server Error", "Oops!", "That's going to leave a mark!",
                        "/WEB-INF/views/error.jsp", definitions);

                // Generate create
    
                writeEntityMetawidget(context, this.createTemplateEntityMetawidgetIndent);
    
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/create" + entity.getName() + ".jsp"), this.createTemplate.render(context), overwrite));

                addViewDefinition(tile, "create" + entity.getName(), "Create " + StringUtils.uncamelCase(entity.getName()),
                        StringUtils.uncamelCase(entity.getName()), "Create a new " + StringUtils.uncamelCase(entity.getName()),
                        "/WEB-INF/views" + targetDir + entity.getName() + "/create" + entity.getName() + ".jsp", definitions);

                // Generate update

                writeEntityMetawidget(context, this.updateTemplateEntityMetawidgetIndent);

                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/update" + entity.getName() + ".jsp"), this.updateTemplate.render(context), overwrite));

                addViewDefinition(tile, "update" + entity.getName(), "Edit " + StringUtils.uncamelCase(entity.getName()),
                        StringUtils.uncamelCase(entity.getName()), "Edit an existing " + StringUtils.uncamelCase(entity.getName()),
                        "/WEB-INF/views" + targetDir + entity.getName() + "/update" + entity.getName() + ".jsp", definitions);

                // Generate search

                this.headerMetawidget.setValue(StaticJspUtils.wrapExpression(entity.getName()));
                this.headerMetawidget.setPath(entity.getQualifiedName());
                this.headerMetawidget.setReadOnly(true);
                this.searchMetawidget.setValue(StaticJspUtils.wrapExpression(entity.getName()));
                this.searchMetawidget.setPath(entity.getQualifiedName());
                this.searchMetawidget.setReadOnly(true);

                writeEntityMetawidget(context, this.searchTemplateMetawidgetIndent);
                writeHeaderAndSearchMetawidgets(context, this.headerMetawidgetIndent, this.searchMetawidgetIndent);

                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/" + entityPlural.toLowerCase() + ".jsp"), this.searchTemplate.render(context), overwrite));

                addViewDefinition(tile, entityPlural.toLowerCase(), "Search " + StringUtils.uncamelCase(entity.getName()) + " entities",
                        StringUtils.uncamelCase(entity.getName()), "Search " + StringUtils.uncamelCase(entity.getName()) + " entities",
                        "/WEB-INF/views" + targetDir + entity.getName() + "/" + entityPlural.toLowerCase()+ ".jsp", definitions);

                // Generate view

                this.entityMetawidget.setReadOnly(true);
                writeEntityMetawidget(context, this.viewTemplateMetawidgetIndent);
    
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/view" + entity.getName() + ".jsp"), this.viewTemplate.render(context), overwrite));

                addViewDefinition(tile, "view" + entity.getName(), "View " + StringUtils.uncamelCase(entity.getName()),
                        StringUtils.uncamelCase(entity.getName()), "View existing " + StringUtils.uncamelCase(entity.getName()),
                        "/WEB-INF/views" + targetDir + entity.getName() + "/view" + entity.getName() + ".jsp", definitions);

                String viewsFile = XMLParser.toXMLString(definitions);

                // TODO: Find a cleaner way to add Tiles DTD than this.

                viewsFile = viewsFile.substring(0, 55) + "\n<!DOCTYPE tiles-definitions PUBLIC\n\"-//Apache Software Foundation"
                + "//DTD Tiles Configuration 2.0//EN\"\n\"http://tiles.apache.org/dtds/tiles-config_2_0.dtd\">\n\n" + viewsFile.substring(55);
                result.add(web.createWebResource(viewsFile, "WEB-INF/views/views.xml"));

                // Prepare qbeMetawidget

                this.qbeMetawidget.setPath(entity.getQualifiedName());
                StringWriter writer = new StringWriter();
                this.qbeMetawidget.write(writer, backingBeanTemplateQbeMetawidgetIndent);

                context.put("qbeMetawidget", writer.toString().trim());
                context.put("qbeMetawidgetImports",
                        CollectionUtils.toString(this.qbeMetawidget.getImports(), ";\r\n", true, false));

                JavaInterface daoInterface = JavaParser.parse(JavaInterface.class, this.daoInterfaceTemplate.render(context));
                JavaClass daoImplementation = JavaParser.parse(JavaClass.class, this.daoImplementationTemplate.render(context));
    
                // Save the created interface and class implementation, so they can be referenced by the controller.

                java.saveJavaSource(daoInterface);
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(daoInterface),
                        daoInterface.toString(), overwrite));
    
                java.saveJavaSource(daoImplementation);
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(daoImplementation),
                        daoImplementation.toString(), overwrite));
    
                // Create a Spring MVC controller for the passed entity, using SpringControllerTemplate.jv
    
                JavaClass entityController = JavaParser.parse(JavaClass.class, this.springControllerTemplate.render(context));
                java.saveJavaSource(entityController);
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(entityController),
                        entityController.toString(), overwrite));

                // Create a Spring MVC controller for the root of the servlet, using IndexControllerTemplate.jv

                JavaClass indexController = JavaParser.parse(JavaClass.class, this.indexControllerTemplate.render(context));
                java.saveJavaSource(indexController);
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(indexController),
                        indexController.toString(), overwrite));

                // If we have not just generated an IndexController for the '/' directory, create one.

                context.put("mvcPackage", meta.getTopLevelPackage() + ".mvc");
                context.put("targetDir", "/");
                JavaClass rootIndexController = JavaParser.parse(JavaClass.class, this.indexControllerTemplate.render(context));
                java.saveJavaSource(rootIndexController);
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(rootIndexController),
                        rootIndexController.toString(), overwrite));

                // Generate navigation, for both "/" and for targetDir

                if (!targetDir.equals("/"))
                    result.add(generateNavigation(entity.getPackage(), targetDir, overwrite));

                result.add(generateNavigation(entity.getPackage(), "/", overwrite));
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error generating Spring scaffolding: " + entity.getName(), e);
            }
        }
        finally
        {
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
            this.install.fire(new InstallFacets(WebResourceFacet.class, PersistenceFacet.class));
        
        return true;
    }

    @Override
    public boolean isInstalled()
    {
        return true;
    }

    @Override
    public List<Resource<?>> generateIndex(String targetDir, Resource<?> template, boolean overwrite) {
        List<Resource<?>> result = new ArrayList<Resource<?>>();
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

        loadTemplates();

//        generateTemplates(overwrite);
        HashMap<Object, Object> context = getTemplateContext(template);
        context.put("targetDir", targetDir);

        // Root index page

        if (!targetDir.equals("/"))
            result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views/index.jsp"),
                    this.indexTemplate.render(context), overwrite));

        // Basic pages

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + "index.jsp"),
                this.indexTemplate.render(context), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + "error.jsp"),
                this.errorTemplate.render(context), overwrite));

        // Static resources

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/add.png"),
                getClass().getResourceAsStream("/scaffold/spring/add.png"), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/background.gif"),
                getClass().getResourceAsStream("/scaffold/spring/background.gif"), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/false.png"),
                getClass().getResourceAsStream("/scaffold/spring/false.png"), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/favicon.ico"),
                getClass().getResourceAsStream("/scaffold/spring/favicon.ico"), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/forge-logo.png"),
                getClass().getResourceAsStream("/scaffold/spring/forge-logo.png"), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/forge-style.css"),
                getClass().getResourceAsStream("/scaffold/spring/forge-style.css"), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/jboss-community.png"),
                getClass().getResourceAsStream("/scaffold/spring/jboss-community.png"), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/remove.png"),
                getClass().getResourceAsStream("/scaffold/spring/remove.png"), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/search.png"),
                getClass().getResourceAsStream("/scaffold/spring/search.png"), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/true.png"),
                getClass().getResourceAsStream("/scaffold/spring/true.png"), overwrite));

       return result;
    }

    @Override
    public List<Resource<?>> generateTemplates(String targetDir, final boolean overwrite)
    {
        List<Resource<?>> result = new ArrayList<Resource<?>>();

        try {
/*            WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

            result.add(ScaffoldUtil.createOrOverwrite(this.prompt,
                    web.getWebResource("/resources/scaffold/paginator.xhtml"),
                    getClass().getResourceAsStream("/resources/scaffold/paginator.xhtml"),
                    overwrite));

            result.add(generateNavigation(targetDir, overwrite));*/
        } catch (Exception e) {
            throw new RuntimeException("Error generating default templates.", e);
        }

        return result;
    }

    // TODO: Perhaps this method should retrieve all generated resources in targetDir, but instead retrieves any generated resource.

    @Override
    public List<Resource<?>> getGeneratedResources(String targetDir)
    {
        /**
         * Not implemented as of yet.
         */

        return null;
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
        // No TemplateStrategy required for Spring.

        return null;
    }

    //
    // Protected methods (nothing is private, to help sub-classing)
    //

    protected Resource<?> setupTilesLayout(String targetDir)
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

        Node definitions = new Node("tiles-definitions");

        if (web.getWebResource("WEB-INF/layouts/layouts.xml").exists())
            definitions = XMLParser.parse(web.getWebResource("WEB-INF/layouts/layouts.xml").getResourceInputStream());

        addLayoutDefinition("standard", "/WEB-INF/layouts/pageTemplate.jsp", definitions);

        if (!targetDir.equals("/"))
        {
            String name = targetDir.substring(1, targetDir.length()-1);
            addLayoutDefinition(name, "/WEB-INF/layouts/" + name + "Template.jsp", definitions);
        }

        String tilesDefinitionFile = XMLParser.toXMLString(definitions);

        // TODO: Find a cleaner way to add Tiles DTD than this.

        tilesDefinitionFile = tilesDefinitionFile.substring(0, 55) + "\n<!DOCTYPE tiles-definitions PUBLIC\n\"-//Apache Software Foundation"
        + "//DTD Tiles Configuration 2.0//EN\"\n\"http://tiles.apache.org/dtds/tiles-config_2_0.dtd\">\n\n" + tilesDefinitionFile.substring(55);

        return web.createWebResource(tilesDefinitionFile, "/WEB-INF/layouts/layouts.xml"); 
    }

    protected void loadTemplates()
    {

        if (this.applicationContextTemplate == null)
        {
            this.applicationContextTemplate = compiler.compile(APPLICATION_CONTEXT_TEMPLATE);
        }

        if (this.mvcContextTemplate == null)
        {
            this.mvcContextTemplate = compiler.compile(MVC_CONTEXT_TEMPLATE);
        }

        if (this.webXMLTemplate == null)
        {
            this.webXMLTemplate = compiler.compile(WEB_XML_TEMPLATE);
        }

        // Compile the DAO interface Java template.
        
        if (this.daoInterfaceTemplate == null)
        {
            this.daoInterfaceTemplate = compiler.compile(DAO_INTERFACE_TEMPLATE);           
        }
        
        // Compile the DAO interface implementation Java template.
        
        if (this.daoImplementationTemplate == null)
        {
            this.daoImplementationTemplate = compiler.compile(DAO_IMPLEMENTATION_TEMPLATE);
        }

        // Compile the Spring MVC index controller Java template.

        if (this.indexControllerTemplate == null)
        {
            this.indexControllerTemplate = compiler.compile(INDEX_CONTROLLER_TEMPLATE);
        }

        // Compile the Spring MVC entity controller Java template.
        
        if (this.springControllerTemplate == null)
        {
            this.springControllerTemplate = compiler.compile(SPRING_CONTROLLER_TEMPLATE);
        }

        if (this.conversionServiceTemplate == null)
        {
            this.conversionServiceTemplate = compiler.compile(CONVERSION_SERVICE_TEMPLATE);
        }

        if (this.entityConverterTemplate == null)
        {
            this.entityConverterTemplate = compiler.compile(ENTITY_CONVERTER_TEMPLATE);
        }

        if (this.searchTemplate == null)
        {
            this.searchTemplate = compiler.compile(VIEW_ALL_TEMPLATE);
            String template = Streams.toString(this.searchTemplate.getSourceTemplateResource().getInputStream());
            this.searchTemplateMetawidgetIndent = parseIndent(template, "@{metawidet}");
            this.headerMetawidgetIndent = parseIndent(template, "@{headerMetawidget}");
            this.searchMetawidgetIndent = parseIndent(template, "@{searchMetawidget}");
        }

        if (this.viewTemplate == null)
        {
            this.viewTemplate = compiler.compile(VIEW_TEMPLATE);
            String template = Streams.toString(this.viewTemplate.getSourceTemplateResource().getInputStream());
            this.viewTemplateMetawidgetIndent = parseIndent(template, "@{metawidget}");
        }

        if (this.updateTemplate == null)
        {
            this.updateTemplate = compiler.compile(UPDATE_TEMPLATE);
            String template = Streams.toString(this.updateTemplate.getSourceTemplateResource().getInputStream());
            this.updateTemplateEntityMetawidgetIndent = parseIndent(template, "@{metawidget}");
        }

        if (this.createTemplate == null)
        {
            this.createTemplate = compiler.compile(CREATE_TEMPLATE);
            String template = Streams.toString(this.createTemplate.getSourceTemplateResource().getInputStream());
            this.createTemplateEntityMetawidgetIndent = parseIndent(template, "@{metawidget}");
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

    protected Resource<?> generateNavigation(String domainPackage, String targetDir, final boolean overwrite)
            throws IOException
    {
        JavaSourceFacet java = this.project.getFacet(JavaSourceFacet.class);
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

        HtmlTag unorderedList = new HtmlTag("ul");

        targetDir = (targetDir.endsWith("/")) ? targetDir : targetDir + "/";

        ResourceFilter filter = new ResourceFilter()
        {
            @Override
            public boolean accept(Resource<?> resource) {
                FileResource<?> file = (FileResource<?>) resource;

                if ( !file.isDirectory() || file.getName().equals("META-INF") || file.getName().equals("WEB-INF") 
                        || file.getName().equals("resources") || file.getName().equals("layouts") || file.getName().equals("views"))
                    return false;

                return true;
            }
        };
        
        for (Resource<?> resource : web.getWebResource("WEB-INF/views" + targetDir).listResources(filter))
        {
            HtmlAnchor link = new HtmlAnchor();

            if (java.getJavaResource(domainPackage + StringUtils.SEPARATOR_DOT_CHAR + resource.getName()).exists())
            {
                link.putAttribute("href", "<c:url value=\"" + targetDir + pluralOf(resource.getName()).toLowerCase() + "/\"/>");
            }
            else
            {
                link.putAttribute("href", "<c:url value=\"" + targetDir + resource.getName() + "\"/>");
            }

            link.setTextContent(StringUtils.uncamelCase(resource.getName()));

            HtmlTag listItem = new HtmlTag("li");
            listItem.getChildren().add(link);
            unorderedList.getChildren().add(listItem);
        }

        Writer writer = new IndentedWriter(new StringWriter(), this.navigationTemplateIndent);
        unorderedList.write(writer);

        Map<Object, Object> context = CollectionUtils.newHashMap();
        context.put("navigation", writer.toString().trim());
        context.put("targetDir", targetDir);

        if (this.navigationTemplate == null)
        {
            loadTemplates();
        }

        if (targetDir.equals("/"))
        {
            return ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/layouts/pageTemplate.jsp"),
                    this.navigationTemplate.render(context), overwrite);
        }
        else
        {
            return ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/layouts/" + targetDir.substring(1,
                    targetDir.length()-1) + "Template.jsp"), this.navigationTemplate.render(context), overwrite);
        }
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

    // TODO: Manage import of namespaces in writeMetawidget methods.

    /**
     * Writes the entity Metawidget into the given context.
     */

    protected void writeEntityMetawidget(final Map<Object, Object> context, final int entityMetawidgetIndent)
    {
        StringWriter writer = new StringWriter();
        this.entityMetawidget.write(writer, entityMetawidgetIndent);
        context.put("metawidget", writer.toString().trim());
    }

    /**
     * Writes the bean Metawidget for displaying the existing entities in the database on the view all page.
     */

    protected void writeHeaderAndSearchMetawidgets(final Map<Object, Object> context, final int headerMetawidgetIndent,
            final int searchMetawidgetIndent)
    {
        StringWriter stringWriter = new StringWriter();
        this.headerMetawidget.write(stringWriter, headerMetawidgetIndent);
        context.put("headerMetawidget", stringWriter.toString().trim());

        stringWriter = new StringWriter();
        this.searchMetawidget.write(stringWriter, searchMetawidgetIndent);
        context.put("searchMetawidget", stringWriter.toString().trim());
    }

    /**
     * Add an <mvc:resources location="/" mapping="/static/**"/> to the root servlet definition.
     */
    
    protected void updateMVCContext(String filename) {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
        Node beans = XMLParser.parse(web.getWebResource(filename).getResourceInputStream());

        if (beans.getSingle("mvc:resources") == null) {
            Node resources = new Node("mvc:resources");
            resources.attribute("location", "/");
            resources.attribute("mapping", "/static/**");
        }

        web.createWebResource(XMLParser.toXMLString(beans), filename);
    }

    /**
     * Add an Apache Tiles2 view <definition> for the given parameters, assuming one does not exist already.
     */

    protected void addViewDefinition(String template, String name, String title, String header, String subheader,
            String body, Node definitions)
    {
        if (definitionExists(name, definitions))
        {
            return;
        }

        Node definition = new Node("definition", definitions);
        definition.attribute("extends", template);
        definition.attribute("name", name);

        Node titleAttribute = new Node("put-attribute", definition);
        titleAttribute.attribute("name", "title");
        titleAttribute.attribute("value", title);

        Node headerAttribute = new Node("put-attribute", definition);
        headerAttribute.attribute("name", "header");
        headerAttribute.attribute("value", header);

        Node subheaderAttribute = new Node("put-attribute", definition);
        subheaderAttribute.attribute("name", "subheader");
        subheaderAttribute.attribute("value", subheader);

        Node bodyAttribute = new Node("put-attribute", definition);
        bodyAttribute.attribute("name", "body");
        bodyAttribute.attribute("value", body);
    }

    /**
     * Add an Apache Tiles2 layout <definition>, assuming one does not already exist.
     */

    protected void addLayoutDefinition(String name, String template, Node definitions)
    {
        if (definitionExists(name, definitions))
        {
            return;
        }

        Node definition = new Node("definition", definitions);
        definition.attribute("name", name);
        definition.attribute("template", template);
    }

    /**
     * Check if the passed node, definitions, contains an Apache Tiles2 <definition> with name="tilesName" as an attribute.
     */
    
    protected boolean definitionExists(String name, Node definitions)
    {
        for (Node definition : definitions.get("definition")) {
            if (definition.getAttribute("name").equals(name))
                return true;
        }

        return false;
    }

    protected Map<Object, Object> findEntityRelationships(JavaClass entity, Map<Object, Object> context) throws FileNotFoundException
    {
        List<String> entityNames = new ArrayList<String>();
        List<String> entityClasses = new ArrayList<String>();
        List<String> ccEntityClasses = new ArrayList<String>();
        List<String> nToMany = new ArrayList<String>();

        for ( Field<?> field : entity.getFields()) {
            if (field.hasAnnotation(OneToOne.class) || field.hasAnnotation(OneToMany.class) || field.hasAnnotation(ManyToOne.class)
                    || field.hasAnnotation(ManyToMany.class)) {
                String name = field.getName();
                entityNames.add(name);
                String clazz = new String();

                if (field.hasAnnotation(OneToMany.class) || field.hasAnnotation(ManyToMany.class)) {
                    clazz = field.getStringInitializer();
                    int firstIndexOf = clazz.indexOf("<");
                    int lastIndexOf = clazz.indexOf(">");

                    clazz = clazz.substring(firstIndexOf + 1, lastIndexOf);
                    String domainPackage = findDomainPackage(clazz, entity);

                    nToMany.add(clazz);
                    createConverter(clazz, domainPackage);
                }
                else {
                    clazz = field.getType();
                }

                entityClasses.add(clazz);
                String ccEntity = StringUtils.camelCase(clazz);
                ccEntityClasses.add(ccEntity);
            }
        }

        context.put("entityNames", entityNames);
        context.put("entityClasses", entityClasses);
        context.put("ccEntityClasses", ccEntityClasses);

        if (!nToMany.isEmpty()) {
            context.put("nToMany", nToMany);
            addConverters(context);
            addConversionService();
        }

        return context;
    }

    protected void addConversionService()
    {
        MetadataFacet meta = this.project.getFacet(MetadataFacet.class);
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

        String filename = "WEB-INF/" + meta.getProjectName().replace(' ', '-').toLowerCase() + "-mvc-context.xml";
        Node beans = XMLParser.parse(web.getWebResource(filename).getResourceInputStream());

        beans.getSingle("mvc:annotation-driven").attribute("conversion-service", "conversionService");
        addContextComponentScan(beans, meta.getTopLevelPackage() + ".conversion");

        web.createWebResource(XMLParser.toXMLString(beans), filename);
    }

    protected void addContextComponentScan(Node beans, String basePackage)
    {
        for (Node scan : beans.get("context:component-scan"))
        {
            if (scan.getAttribute("base-package").equals(basePackage))
            {
                return;
            }
        }

        Node scan = new Node("context:component-scan", beans);
        scan.attribute("base-package", basePackage);
    }

    protected void loadConversionTemplates()
    {
        if (this.entityConverterTemplate == null)
        {
            this.compiler.compile(ENTITY_CONVERTER_TEMPLATE);
        }

        if (this.conversionServiceTemplate == null)
        {
            this.compiler.compile(CONVERSION_SERVICE_TEMPLATE);
        }
    }

    protected String findDomainPackage(String clazz, JavaClass entity)
    {

        for (Import imp : entity.getImports())
        {
            String simpleName = imp.getSimpleName();

            if (simpleName.equals(clazz))
            {
                return imp.getQualifiedName();
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected void addConverters(Map<Object, Object> context) throws FileNotFoundException
    {
        MetadataFacet meta = this.project.getFacet(MetadataFacet.class);
        JavaSourceFacet java = this.project.getFacet(JavaSourceFacet.class);

        loadConversionTemplates();
        context.put("topLevelPackage", meta.getTopLevelPackage());

        JavaClass conversionService = JavaParser.parse(JavaClass.class, this.conversionServiceTemplate.render(context));
        String customConversionService = meta.getTopLevelPackage() + ".conversion.ConversionService";

        if (java.getJavaResource(customConversionService).exists())
        {
            conversionService = JavaParser.parse(JavaClass.class, java.getJavaResource(customConversionService).getResourceInputStream());
        }

        List<String> nToMany = (List<String>) context.get("nToMany");
        for (int i = 0; i < nToMany.size(); i++) {
            String clazz = nToMany.get(i);

            if (!hasConverter(conversionService, clazz))
            {
                conversionService.addImport(meta.getTopLevelPackage() + ".repo." + clazz + "Dao");
                conversionService.addImport(meta.getTopLevelPackage() + ".converters." + clazz + "Converter");

                Field<?> dao = conversionService.addField("private " + clazz + "Dao " + StringUtils.camelCase(clazz) + "Dao;");
                dao.addAnnotation("org.springframework.beans.factory.annotation.Autowired");

                Method<?> afterPropertiesSet = conversionService.getMethod("afterPropertiesSet");
                String body = afterPropertiesSet.getBody();
                body += "this.addConverter(new " + clazz + "Converter(" + StringUtils.camelCase(clazz) + "Dao));";
                afterPropertiesSet.setBody(body);
            }
        }

        java.saveJavaSource(conversionService);
    }

    protected boolean hasConverter(JavaClass conversionService, String clazz)
    {
        for (Field<?> dao : conversionService.getFields()) {
            if (dao.getName().equals(StringUtils.camelCase(clazz) + "Dao"));
                return true;
        }

        return false;
    }

    protected void createConverter(String clazz, String domainPackage) throws FileNotFoundException
    {
        JavaSourceFacet java = this.project.getFacet(JavaSourceFacet.class);
        MetadataFacet meta = this.project.getFacet(MetadataFacet.class);

        Map<Object, Object> context = CollectionUtils.newHashMap();

        context.put("entityName", clazz);
        context.put("domainPackage", domainPackage);
        context.put("ccEntity", StringUtils.camelCase(clazz));
        context.put("topLevelPackage", meta.getTopLevelPackage());

        JavaClass entityConverter = JavaParser.parse(JavaClass.class, this.entityConverterTemplate.render(context));
        java.saveJavaSource(entityConverter);
    }
}