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
import java.util.Collection;
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
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.Import;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.Dependency;
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
import org.jboss.forge.resources.java.JavaResource;
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
import org.jboss.forge.spec.spring.mvc.SpringFacet;
import org.jboss.seam.render.TemplateCompiler;
import org.jboss.seam.render.spi.TemplateResolver;
import org.jboss.seam.render.template.CompiledTemplateResource;
import org.jboss.seam.render.template.resolver.ClassLoaderTemplateResolver;
import org.jboss.shrinkwrap.descriptor.api.spec.jpa.persistence.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.spec.jpa.persistence.PersistenceUnitDef;
import org.metawidget.statically.StaticMetawidget;
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
            PersistenceFacet.class,
            SpringFacet.class })
public class SpringScaffold extends BaseFacet implements ScaffoldProvider
{

    //
    // Private statics
    //

    private static final Dependency JBOSS_SERVLET_API = DependencyBuilder.create("org.jboss.spec.javax.servlet:jboss-servlet-api_3.0_spec");
    private static final Dependency APACHE_TILES = DependencyBuilder.create("org.apache.tiles:tiles-jsp:2.1.3");

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
    private static final String SEARCH_TEMPLATE = "scaffold/spring/search.jsp";
    private static final String EDIT_TEMPLATE = "scaffold/spring/edit.jsp";
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
    protected int resultMetawidgetIndent;
    protected int headerMetawidgetIndent;

    protected CompiledTemplateResource viewTemplate;
    protected int viewTemplateMetawidgetIndent;

    protected CompiledTemplateResource editTemplate;
    protected int editTemplateEntityMetawidgetIndent;

    protected CompiledTemplateResource createTemplate;
    protected int createTemplateEntityMetawidgetIndent;

    protected CompiledTemplateResource navigationTemplate;
    protected int navigationTemplateIndent;

    protected CompiledTemplateResource errorTemplate;
    protected CompiledTemplateResource indexTemplate;   

    protected StaticSpringMetawidget entityMetawidget;
    protected StaticJspMetawidget headerMetawidget;
    protected StaticJspMetawidget resultMetawidget;
    protected StaticJavaMetawidget qbeMetawidget;
    protected StaticSpringMetawidget searchMetawidget;
    protected StaticSpringMetawidget viewjspMetawidget;

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
                    final Event<InstallFacets> install)
    {
        this.config = config;
        this.prompt = prompt;
        this.compiler = compiler;
        this.install = install;

        this.resolver = new ClassLoaderTemplateResolver(SpringScaffold.class.getClassLoader());
        
        if(this.compiler != null)
        {
            this.compiler.getTemplateResolverFactory().addResolver(this.resolver);
        }
    }
    
    //
    // Public methods
    //

    @Override
    public List<Resource<?>> setup(String targetDir, Resource<?> template, boolean overwrite)
    {
        DependencyFacet deps = project.getFacet(DependencyFacet.class);
        MetadataFacet meta = project.getFacet(MetadataFacet.class);
        PersistenceFacet persistence = project.getFacet(PersistenceFacet.class);
        ResourceFacet resources = project.getFacet(ResourceFacet.class);
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        // Use the Forge DependencyFacet to add JBoss Servlet and Apache Tiles dependencies to the POM

        deps.addDirectDependency(JBOSS_SERVLET_API);
        deps.addDirectDependency(APACHE_TILES);

        Map<Object, Object> context = CollectionUtils.newHashMap();
        context.put("targetDir", targetDir);

        String mvcPackage = (targetDir.isEmpty()) ? ".mvc.root" : ".mvc." + targetDir.toLowerCase().replace('/', '.');
        mvcPackage = meta.getTopLevelPackage() + mvcPackage;
        context.put("mvcPackage", mvcPackage);

        targetDir = processTargetDir(targetDir);
        targetDir = (targetDir.isEmpty()) ? "/" : "/" + targetDir + "/";

        context.put("repoPackage", meta.getTopLevelPackage() + ".repo");

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
        context.put("mvcContextFile", filename);

        if (!web.getWebResource("WEB-INF/web.xml").exists())
        {
            result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/web.xml"),
                    this.webXMLTemplate.render(context), overwrite));
        }
        else
        {
            SpringFacet spring = project.getFacet(SpringFacet.class);

            if (targetDir.equals("/"))
            {
                spring.addRootServlet(meta.getProjectName().replace(' ', '-').toLowerCase() + "-mvc-context.xml");
            }
            else
            {
                targetDir = processTargetDir(targetDir);
                spring.addServlet(targetDir, targetDir.replace('/', '-').toLowerCase() + "-mvc-context.xml");
            }
        }

        result.add(setupTilesLayout(targetDir));

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
    public void setProject(Project project)
    {
        super.setProject(project);
        
        ForgeConfigReader configReader = new ForgeConfigReader(this.config, this.project);
        
        this.entityMetawidget = new StaticSpringMetawidget();
        this.entityMetawidget.setConfigReader(configReader);
        this.entityMetawidget.setConfig("scaffold/spring/metawidget-entity.xml");

        this.headerMetawidget = new StaticJspMetawidget();
        this.headerMetawidget.setConfigReader(configReader);
        this.headerMetawidget.setConfig("scaffold/spring/metawidget-header.xml");

        this.resultMetawidget = new StaticJspMetawidget();
        this.resultMetawidget.setConfigReader(configReader);
        this.resultMetawidget.setConfig("scaffold/spring/metawidget-result.xml");

        this.qbeMetawidget = new StaticJavaMetawidget();
        this.qbeMetawidget.setConfigReader(configReader);
        this.qbeMetawidget.setConfig("scaffold/spring/metawidget-qbe.xml");

        this.searchMetawidget = new StaticSpringMetawidget();
        this.searchMetawidget.setConfigReader(configReader);
        this.searchMetawidget.setConfig("scaffold/spring/metawidget-search.xml");
        
        this.viewjspMetawidget = new StaticSpringMetawidget();
        this.viewjspMetawidget.setConfigReader(configReader);
        this.viewjspMetawidget.setConfig("scaffold/spring/metawidget-jsp-entity.xml");
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
                JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
                WebResourceFacet web = project.getFacet(WebResourceFacet.class);
                MetadataFacet meta = project.getFacet(MetadataFacet.class);

                loadTemplates();

                // Set context for Java and JSP generation

                Map<Object, Object> context = CollectionUtils.newHashMap();
                context.put("entity", entity);
                String ccEntity = StringUtils.decapitalize(entity.getName());
                context.put("ccEntity", ccEntity);

                context.put("topLevelPackage", meta.getTopLevelPackage());

                String mvcPackage = (targetDir.isEmpty()) ? meta.getTopLevelPackage() + ".mvc.root" : meta.getTopLevelPackage() + ".mvc." +
                                        targetDir.replace('/', '.');
                context.put("mvcPackage", mvcPackage);

                findEntityRelationships(entity, context);

                targetDir = processTargetDir(targetDir);
                targetDir = (targetDir.isEmpty()) ? "/" : "/" + targetDir + "/";

                context.put("targetDir", targetDir);
                context.put("entityName", StringUtils.uncamelCase(entity.getName()));
                String entityPlural = pluralOf(entity.getName());
                context.put("entityPlural", entityPlural);
                context.put("entityPluralName", pluralOf(StringUtils.uncamelCase(entity.getName())));

                // Prepare entity metawidget

                this.entityMetawidget.setValue(ccEntity);
                this.entityMetawidget.setPath(entity.getQualifiedName());
                this.entityMetawidget.setReadOnly(false);

                // Create, or update, a views.xml file containing all tiles definitions.

                Node definitions = new Node("tiles-definitions");

                if (web.getWebResource("WEB-INF/views/views.xml").exists())
                {
                    definitions = XMLParser.parse(web.getWebResource("WEB-INF/views/views.xml").getResourceInputStream());
                }

                String tile = targetDir.equals("/") ? "standard" : targetDir.substring(1, targetDir.length()-1);

                // Add index page(s)

                if (!targetDir.equals("/"))
                {
                    addViewDefinition("standard", "/index", "Weclome to Forge", "Welcome to Forge", "",
                            "/WEB-INF/views/index.jsp", definitions);
                }

                addViewDefinition(tile, targetDir + "index", "Welcome to Forge", "Welcome to Forge", "",
                        "/WEB-INF/views" + targetDir + "index.jsp", definitions);

                // Add error page

                addViewDefinition("standard", "/error", "Server Error", "Oops!", "That's going to leave a mark!",
                        "/WEB-INF/views/error.jsp", definitions);

                // Generate create

                writeMetawidget(context, this.entityMetawidget, this.createTemplateEntityMetawidgetIndent, "metawidget");
    
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/create" + entity.getName() + ".jsp"), this.createTemplate.render(context), overwrite));

                addViewDefinition(tile, "create" + entity.getName(), "Create " + StringUtils.uncamelCase(entity.getName()),
                        StringUtils.uncamelCase(entity.getName()), "Create a new " + StringUtils.uncamelCase(entity.getName()),
                        "/WEB-INF/views" + targetDir + entity.getName() + "/create" + entity.getName() + ".jsp", definitions);

                // Generate edit

                writeMetawidget(context, this.entityMetawidget, this.editTemplateEntityMetawidgetIndent, "metawidget");

                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/edit" + entity.getName() + ".jsp"), this.editTemplate.render(context), overwrite));

                addViewDefinition(tile, "edit" + entity.getName(), "Edit " + StringUtils.uncamelCase(entity.getName()),
                        StringUtils.uncamelCase(entity.getName()), "Edit an existing " + StringUtils.uncamelCase(entity.getName()),
                        "/WEB-INF/views" + targetDir + entity.getName() + "/edit" + entity.getName() + ".jsp", definitions);

                // Generate search

                this.searchMetawidget.setValue(ccEntity);
                this.searchMetawidget.setPath(entity.getQualifiedName());
                this.searchMetawidget.setReadOnly(false);

                this.headerMetawidget.setValue(StaticJspUtils.wrapExpression(entity.getName()));
                this.headerMetawidget.setPath(entity.getQualifiedName());
                this.headerMetawidget.setReadOnly(true);

                this.resultMetawidget.setValue(StaticJspUtils.wrapExpression(entity.getName()));
                this.resultMetawidget.setPath(entity.getQualifiedName());
                this.resultMetawidget.setReadOnly(true);

                writeMetawidget(context, this.searchMetawidget, this.searchTemplateMetawidgetIndent, "metawidget");
                writeMetawidget(context, this.headerMetawidget, this.headerMetawidgetIndent, "headerMetawidget");
                writeMetawidget(context, this.resultMetawidget, this.resultMetawidgetIndent, "resultMetawidget");

                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/" + entityPlural.toLowerCase() + ".jsp"), this.searchTemplate.render(context), overwrite));

                addViewDefinition(tile, entityPlural.toLowerCase(), "Search " + StringUtils.uncamelCase(entity.getName()) + " entities",
                        StringUtils.uncamelCase(entity.getName()), "Search " + StringUtils.uncamelCase(entity.getName()) + " entities",
                        "/WEB-INF/views" + targetDir + entity.getName() + "/" + entityPlural.toLowerCase()+ ".jsp", definitions);

                // Generate view

                this.viewjspMetawidget.setValue(ccEntity);
                this.viewjspMetawidget.setPath(entity.getQualifiedName());
                this.viewjspMetawidget.setReadOnly(true);
                writeMetawidget(context, this.viewjspMetawidget, this.viewTemplateMetawidgetIndent, "metawidget");
    
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/view" + entity.getName() + ".jsp"), this.viewTemplate.render(context), overwrite));

                addViewDefinition(tile, "view" + entity.getName(), "View " + StringUtils.uncamelCase(entity.getName()),
                        StringUtils.uncamelCase(entity.getName()), "View existing " + StringUtils.uncamelCase(entity.getName()),
                        "/WEB-INF/views" + targetDir + entity.getName() + "/view" + entity.getName() + ".jsp", definitions);

                String viewsXML = XMLParser.toXMLString(definitions);
                viewsXML = viewsXML.substring(0, 55) + "\n<!DOCTYPE tiles-definitions PUBLIC \"-//Apache Software Foundation//DTD Tiles " +
                		"Configuration 2.0//EN\" \"http://tiles.apache.org/dtds/tiles-config_2_0.dtd\">\n" + viewsXML.substring(55);

                result.add(web.createWebResource(viewsXML, "WEB-INF/views/views.xml"));

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
                extractNonNToN(entity, context);
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

                if (!targetDir.equals("/"))
                {
                    context.put("mvcPackage", meta.getTopLevelPackage() + ".mvc.root");
                    context.put("targetDir", "/");

                    JavaClass rootIndexController = JavaParser.parse(JavaClass.class, this.indexControllerTemplate.render(context));
                    java.saveJavaSource(rootIndexController);
                    result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(rootIndexController),
                            rootIndexController.toString(), overwrite));

                // Generate navigation, for both "/" and for targetDir

                    result.add(generateNavigation("/", overwrite));
                }

                result.add(generateNavigation(targetDir, overwrite));
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

	private void extractNonNToN(JavaClass entity, Map<Object, Object> context) {
		@SuppressWarnings("unchecked")
		List<String> names = (List<String>) context.get("entityNames");
		List<String> notnToMany = new ArrayList<String>();
		for (String string : names) {
			for (Annotation<JavaClass> ntomany : entity.getField(string).getAnnotations()) {
				if(!(ntomany.getName().contains("ManyToOne") || ntomany.getName().contains("ManyToMany") || ntomany.getName().contains("OneToMany")))
					notnToMany.add(string);
				
			}
		}
		context.put("notntomany", notnToMany);
	}

    @Override
    @SuppressWarnings("unchecked")    
    public boolean install()
    {
        if(!(project.hasFacet(WebResourceFacet.class) && project.hasFacet(PersistenceFacet.class)
                && project.hasFacet(SpringFacet.class)))
            this.install.fire(new InstallFacets(WebResourceFacet.class, PersistenceFacet.class, SpringFacet.class));
        
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
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        loadTemplates();

        targetDir = targetDir.startsWith("/") ? targetDir : "/" + targetDir;
        targetDir = targetDir.endsWith("/") ? targetDir : targetDir + "/";

        generateTemplates(targetDir, overwrite);
        HashMap<Object, Object> context = getTemplateContext(targetDir, template);
        context.put("targetDir", targetDir);

        // Root index page

        if (!targetDir.equals("/"))
        {
            result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views/index.jsp"),
                    this.indexTemplate.render(context), overwrite));
        }

        // Basic pages

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + "index.jsp"),
                this.indexTemplate.render(context), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views/error.jsp"),
                this.errorTemplate.render(context), overwrite));

        // Static resources - only add them if they are not already present.

        ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/bootstrap.css"),
                getClass().getResourceAsStream("/scaffold/spring/bootstrap.css"), overwrite);
        ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/false.png"),
                getClass().getResourceAsStream("/scaffold/spring/false.png"), overwrite);
        ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/forge-logo.png"),
                getClass().getResourceAsStream("/scaffold/spring/forge-logo.png"), overwrite);
        ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/forge-style.css"),
                getClass().getResourceAsStream("/scaffold/spring/forge-style.css"), overwrite);
        ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/search.png"),
                getClass().getResourceAsStream("/scaffold/spring/search.png"), overwrite);
        ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/true.png"), 
                getClass().getResourceAsStream("/scaffold/spring/true.png"), overwrite);
        ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("/resources/jboss-community.png"), 
                getClass().getResourceAsStream("/scaffold/spring/jboss-community.png"), overwrite);

       return result;
    }

    @Override
    public List<Resource<?>> generateTemplates(String targetDir, final boolean overwrite)
    {
        List<Resource<?>> result = new ArrayList<Resource<?>>();

        try
        {
            result.add(generateNavigation(targetDir, overwrite));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error generating default templates.", e);
        }

        return result;
    }

    // TODO: Perhaps this method should retrieve all generated resources in targetDir, but instead retrieves any generated resource.

    @Override
    public List<Resource<?>> getGeneratedResources(String targetDir)
    {
        List<Resource<?>> generatedResources = new ArrayList<Resource<?>>();
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        for (Resource<?> resource : web.getWebResource(targetDir).listResources())
        {
            FileResource<?> file = (FileResource<?>) resource;

            if (file.isDirectory())
            {
                generatedResources.addAll(getGeneratedResources(targetDir + "/" + file.getName()));
                continue;
            }

            if (Streams.toString(file.getResourceInputStream()).contains("<!-- Generated by Forge -->"))
            {
                generatedResources.add(resource);
            }
        }

        return generatedResources;
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

    protected Resource<?> setupTilesLayout(String targetDir)
    {
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        Node definitions = new Node("tiles-definitions");

        if (web.getWebResource("WEB-INF/layouts/layouts.xml").exists())
        {
            definitions = XMLParser.parse(web.getWebResource("WEB-INF/layouts/layouts.xml").getResourceInputStream());
        }

        addLayoutDefinition("standard", "/WEB-INF/layouts/pageTemplate.jsp", definitions);

        if (!targetDir.equals("/"))
        {
            String name = targetDir.substring(1, targetDir.length()-1);
            addLayoutDefinition(name, "/WEB-INF/layouts/" + name + "Template.jsp", definitions);
        }

        String layoutsXML = XMLParser.toXMLString(definitions);
        layoutsXML = layoutsXML.substring(0, 55) + "\n<!DOCTYPE tiles-definitions PUBLIC \"-//Apache Software Foundation//DTD Tiles " +
        		"Configuration 2.0//EN\" \"http://tiles.apache.org/dtds/tiles-config_2_0.dtd\">\n" + layoutsXML.substring(55);

        return web.createWebResource(layoutsXML, "/WEB-INF/layouts/layouts.xml"); 
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
            this.searchTemplate = compiler.compile(SEARCH_TEMPLATE);
            String template = Streams.toString(this.searchTemplate.getSourceTemplateResource().getInputStream());
            this.searchTemplateMetawidgetIndent = parseIndent(template, "@{metawidet}");
            this.headerMetawidgetIndent = parseIndent(template, "@{headerMetawidget}");
            this.resultMetawidgetIndent = parseIndent(template, "@{resultMetawidget}");
        }

        if (this.viewTemplate == null)
        {
            this.viewTemplate = compiler.compile(VIEW_TEMPLATE);
            String template = Streams.toString(this.viewTemplate.getSourceTemplateResource().getInputStream());
            this.viewTemplateMetawidgetIndent = parseIndent(template, "@{metawidget}");
        }

        if (this.editTemplate == null)
        {
            this.editTemplate = compiler.compile(EDIT_TEMPLATE);
            String template = Streams.toString(this.editTemplate.getSourceTemplateResource().getInputStream());
            this.editTemplateEntityMetawidgetIndent = parseIndent(template, "@{metawidget}");
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

    protected HashMap<Object, Object> getTemplateContext(String targetDir, final Resource<?> template)
    {
        HashMap<Object, Object> context = new HashMap<Object, Object>();
        context.put("template", template);
        context.put("templateStrategy", getTemplateStrategy());
        context.put("targetDir", targetDir);
        return context;
    }

    /**
     * Generates the navigation menu based on scaffolded entities.
     */

    protected Resource<?> generateNavigation(String targetDir, final boolean overwrite)
            throws IOException
    {
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        HtmlTag unorderedList = new HtmlTag("ul");

        targetDir = (targetDir.endsWith("/")) ? targetDir : targetDir + "/";

        ResourceFilter filter = new ResourceFilter()
        {
            @Override
            public boolean accept(Resource<?> resource)
            {
                FileResource<?> file = (FileResource<?>) resource;

                if (!file.isDirectory() || file.getName().equals("META-INF") || file.getName().equals("WEB-INF") 
                        || file.getName().equals("resources") || file.getName().equals("layouts") || file.getName().equals("views"))
                    return false;

                return true;
            }
        };
        
        for (Resource<?> resource : web.getWebResource("WEB-INF/views" + targetDir).listResources(filter))
        {
            SpringFacet spring = project.getFacet(SpringFacet.class);
            HtmlAnchor link = new HtmlAnchor();

            if (spring.hasServlet(resource.getName()))
            {
                link.putAttribute("href", "<c:url value=\"" + targetDir + resource.getName() + "\"/>");
            }
            else
            {
                link.putAttribute("href", "<c:url value=\"" + targetDir + pluralOf(resource.getName()).toLowerCase() + "/\"/>");
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
        context.put("appName", project.getProjectRoot().getName());

        if (this.navigationTemplate == null)
        {
            loadTemplates();
        }

        if (targetDir.equals("/"))
        {
            return ScaffoldUtil.createOrOverwrite(this.prompt, (FileResource<?>) getTemplateStrategy().getDefaultTemplate(),
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

    /**
     * Writes the Metawidget into the given context.
     */

    protected void writeMetawidget(final Map<Object, Object> context, final StaticMetawidget metawidget, final int metawidgetIndent,
                    final String metawidgetName)
    {
        StringWriter writer = new StringWriter();
        metawidget.write(writer, metawidgetIndent);
        context.put(metawidgetName, writer.toString().trim());
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

        for ( Field<?> field : entity.getFields())
        {
            if (field.hasAnnotation(OneToOne.class) || field.hasAnnotation(OneToMany.class) || field.hasAnnotation(ManyToOne.class)
                    || field.hasAnnotation(ManyToMany.class))
            {
                String name = field.getName();
                entityNames.add(name);
                String clazz = new String();

                if (field.hasAnnotation(OneToMany.class) || field.hasAnnotation(ManyToMany.class))
                {
                    clazz = field.getStringInitializer();
                    int firstIndexOf = clazz.indexOf("<");
                    int lastIndexOf = clazz.indexOf(">");

                    clazz = clazz.substring(firstIndexOf + 1, lastIndexOf);
                    String domainPackage = findDomainPackage(clazz, entity);

                    nToMany.add(clazz);
                    createConverter(clazz, domainPackage);
                }
                else
                {
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

        if (!nToMany.isEmpty())
        {
            context.put("nToMany", nToMany);
            addConverters(context);
            addConversionService();
        }

        return context;
    }

    protected void addConversionService()
    {
        MetadataFacet meta = project.getFacet(MetadataFacet.class);
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

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

        return entity.getPackage() + "." + clazz;
    }

    @SuppressWarnings("unchecked")
    protected void addConverters(Map<Object, Object> context) throws FileNotFoundException
    {
        MetadataFacet meta = project.getFacet(MetadataFacet.class);
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        
        

        context.put("topLevelPackage", meta.getTopLevelPackage());

        JavaClass conversionService;
        try{
			conversionService = (JavaClass) java.getJavaResource(context.get("topLevelPackage").toString().replace(".", "/") + "/conversion/CustomConversionService").getJavaSource();
        }catch (Exception e) {
        	conversionService = JavaParser.parse(JavaClass.class, this.conversionServiceTemplate.render(context));
		}
        
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
        for (Field<?> dao : conversionService.getFields())
        {
            if (dao.getName().equals(StringUtils.camelCase(clazz) + "Dao"))
            {
                return true;
            }
        }

        return false;
    }

    protected void createConverter(String clazz, String domainPackage) throws FileNotFoundException
    {
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        MetadataFacet meta = project.getFacet(MetadataFacet.class);

        Map<Object, Object> context = CollectionUtils.newHashMap();

        context.put("entityName", clazz);
        context.put("domainPackage", domainPackage);
        context.put("ccEntity", StringUtils.camelCase(clazz));
        context.put("topLevelPackage", meta.getTopLevelPackage());

        JavaClass entityConverter = JavaParser.parse(JavaClass.class, this.entityConverterTemplate.render(context));
        java.saveJavaSource(entityConverter);
    }

    private String processTargetDir(String targetDir)
    {
        targetDir = (targetDir.startsWith("/")) ? targetDir.substring(1) : targetDir;
        targetDir = (targetDir.endsWith("/")) ? targetDir.substring(0, targetDir.length()) : targetDir;

        return targetDir;
    }
}