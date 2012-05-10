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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.jboss.forge.env.Configuration;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.MetadataFacet;
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
import org.metawidget.statically.StaticUtils.IndentedWriter;
import org.metawidget.statically.javacode.StaticJavaMetawidget;
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
public class SpringScaffold extends BaseFacet implements ScaffoldProvider {
    
    //
    // Private statics
    //

    private static String XMLNS_PREFIX = "xmlns:";

    private static final String INDEX_CONTROLLER_TEMPLATE = "scaffold/spring/IndexControllerTemplate.jv";
    private static final String SPRING_CONTROLLER_TEMPLATE = "scaffold/spring/SpringControllerTemplate.jv";
    private static final String DAO_INTERFACE_TEMPLATE = "scaffold/spring/DaoInterfaceTemplate.jv";
    private static final String DAO_IMPLEMENTATION_TEMPLATE = "scaffold/spring/DaoImplementationTemplate.jv";
    private static final String VIEW_TEMPLATE = "scaffold/spring/view.jsp";
    private static final String VIEW_ALL_TEMPLATE = "scaffold/spring/viewAll.jsp";
    private static final String UPDATE_TEMPLATE = "scaffold/spring/update.jsp";
    private static final String CREATE_TEMPLATE = "scaffold/spring/create.jsp";
    private static final String NAVIGATION_TEMPLATE = "scaffold/spring/pageTemplate.jsp";
    
    private static final String ERROR_TEMPLATE = "scaffold/spring/error.jsp";
    private static final String INDEX_TEMPLATE = "scaffold/spring/index.jsp";

    //
    // Protected members (nothing is private, to help sub-classing)
    //

    protected int backingBeanTemplateQbeMetawidgetIndent;

    protected CompiledTemplateResource indexControllerTemplate;
    protected CompiledTemplateResource springControllerTemplate;
    protected CompiledTemplateResource daoInterfaceTemplate;
    protected CompiledTemplateResource daoImplementationTemplate;

    protected CompiledTemplateResource viewAllTemplate;
    protected CompiledTemplateResource viewTemplate;
    protected Map<String, String> viewTemplateNamespaces;
    protected int viewTemplateEntityMetawidgetIndent;

    protected CompiledTemplateResource updateTemplate;
    protected Map<String, String> updateTemplateNamespaces;
    protected int updateTemplateEntityMetawidgetIndent;

    protected CompiledTemplateResource createTemplate;
    protected Map<String, String> createTemplateNamespaces;
    protected int createTemplateEntityMetawidgetIndent;

    protected CompiledTemplateResource navigationTemplate;
    protected int navigationTemplateIndent;

    protected CompiledTemplateResource errorTemplate;
    protected CompiledTemplateResource indexTemplate;   
    private TemplateResolver<ClassLoader> resolver;
    
    private ShellPrompt prompt;
    private TemplateCompiler compiler;
    private Event<InstallFacets> install;
    private StaticSpringMetawidget entityMetawidget;
    private StaticJavaMetawidget qbeMetawidget;

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
            this.compiler.getTemplateResolverFactory().addResolver(this. resolver);
        }
    }
    
    //
    // Public methods
    //

    @Override
    public List<Resource<?>> setup(String targetDir, Resource<?> template, boolean overwrite)
    {
        DependencyFacet deps = this.project.getFacet(DependencyFacet.class);

        if (!targetDir.startsWith("/"))
            targetDir = "/" + targetDir;

        if (!targetDir.endsWith("/"))
            targetDir += "/";

        List<Resource<?>> result = generateIndex(targetDir, template, overwrite);

        result.add(setupMVCContext(targetDir));
        result.add(updateWebXML(targetDir));
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
    public void setProject(Project project)
    {
        super.setProject(project);
        
        ForgeConfigReader configReader = new ForgeConfigReader(this.config, this.project);
        
        this.entityMetawidget = new StaticSpringMetawidget();
        this.entityMetawidget.setConfigReader(configReader);
        this.entityMetawidget.setConfig("scaffold/spring/metawidget-entity.xml");
        
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

        try {

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
                findEntityRelationships(entity, context);

                if (!targetDir.startsWith("/"))
                    targetDir = "/" + targetDir;

                if (targetDir.equals("/"))
                {
                    context.put("daoPackage", meta.getTopLevelPackage() + ".repo.root");
                    context.put("mvcPackage",  meta.getTopLevelPackage() + ".mvc.root");
                }

                else
                {
                    context.put("daoPackage", meta.getTopLevelPackage() + ".repo" + targetDir.replace('/', '.'));
                    context.put("mvcPackage", meta.getTopLevelPackage() + ".mvc" + targetDir.replace('/', '.'));
                    targetDir += "/";
                }

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
                Node tilesDefinitions = new Node("tiles-definitions");

                if (viewsXML.exists())
                {
                    tilesDefinitions = XMLParser.parse(viewsXML.getResourceInputStream());
                }

                String tilesName = "standard";

                if (!targetDir.equals("/"))
                    tilesName = targetDir.substring(1, targetDir.length()-1);

                // Generate create
    
                writeEntityMetawidget(context, this.createTemplateEntityMetawidgetIndent, this.createTemplateNamespaces);
    
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/create" + entity.getName() + ".jsp"), this.createTemplate.render(context), overwrite));

                if (!targetDir.equals("/") && !tilesDefinitionExists(targetDir + "index", tilesDefinitions))
                {
                    Node indexDefinition = new Node("definition", tilesDefinitions);
                    indexDefinition.attribute("name", targetDir + "index");
                    indexDefinition.attribute("extends", tilesName);
                    Node indexTitleAttribute = new Node("put-attribute", indexDefinition);
                    indexTitleAttribute.attribute("name", "title");
                    indexTitleAttribute.attribute("value", "Welcome to Forge");
                    Node indexBodyAttribute = new Node("put-attribute", indexDefinition);
                    indexBodyAttribute.attribute("name", "body");
                    indexBodyAttribute.attribute("value", "/WEB-INF/views" + targetDir + "index.jsp");
                }

                if (!tilesDefinitionExists("/index", tilesDefinitions))
                {
                    Node rootIndexDefinition = new Node("definition", tilesDefinitions);
                    rootIndexDefinition.attribute("name", "/index");
                    rootIndexDefinition.attribute("extends", "standard");
                    Node rootIndexTitleAttribute = new Node("put-attribute", rootIndexDefinition);
                    rootIndexTitleAttribute.attribute("name", "title");
                    rootIndexTitleAttribute.attribute("value", "Welcome to Forge");
                    Node rootIndexBodyAttribute = new Node("put-attribute", rootIndexDefinition);
                    rootIndexBodyAttribute.attribute("name", "body");
                    rootIndexBodyAttribute.attribute("value", "/WEB-INF/views/index.jsp");
                }

                if (!tilesDefinitionExists("create" + entity.getName(), tilesDefinitions))
                {
                    Node createDefinition = new Node("definition", tilesDefinitions);
                    createDefinition.attribute("name", "create" + entity.getName());
                    createDefinition.attribute("extends", tilesName);
                    Node createTitleAttribute = new Node("put-attribute", createDefinition);
                    createTitleAttribute.attribute("name", "title");
                    createTitleAttribute.attribute("value", "Create New " + StringUtils.uncamelCase(entity.getName()));
                    Node createBodyAttribute = new Node("put-attribute", createDefinition);
                    createBodyAttribute.attribute("name", "body");
                    createBodyAttribute.attribute("value", "/WEB-INF/views" + targetDir + entity.getName() + "/create"
                            + entity.getName() + ".jsp");
                }

                // Generate update

                writeEntityMetawidget(context, this.updateTemplateEntityMetawidgetIndent, this.updateTemplateNamespaces);

                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/update" + entity.getName() + ".jsp"), this.updateTemplate.render(context), overwrite));

                if (!tilesDefinitionExists("update" + entity.getName(), tilesDefinitions))
                {
                    Node updateDefinition = new Node("definition", tilesDefinitions);
                    updateDefinition.attribute("name", "update" + entity.getName());
                    updateDefinition.attribute("extends", tilesName);
                    Node updateTitleAttribute = new Node("put-attribute", updateDefinition);
                    updateTitleAttribute.attribute("name", "title");
                    updateTitleAttribute.attribute("value", "Update " + StringUtils.uncamelCase(entity.getName()));
                    Node updateBodyAttribute = new Node("put-attribute", updateDefinition);
                    updateBodyAttribute.attribute("name", "body");
                    updateBodyAttribute.attribute("value", "/WEB-INF/views" + targetDir + entity.getName() + "/update"
                            + entity.getName() + ".jsp");
                }

                // Generate search/view (all)

                writeEntityMetawidget(context, this.viewTemplateEntityMetawidgetIndent, this.viewTemplateNamespaces);

                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/" + entityPlural.toLowerCase() + ".jsp"), this.viewAllTemplate.render(context), overwrite));

                if (!tilesDefinitionExists(entityPlural.toLowerCase(), tilesDefinitions))
                {
                    Node viewAllDefinition = new Node("definition", tilesDefinitions);
                    viewAllDefinition.attribute("name", entityPlural.toLowerCase());
                    viewAllDefinition.attribute("extends", tilesName);
                    Node viewAllTitleAttribute = new Node("put-attribute", viewAllDefinition);
                    viewAllTitleAttribute.attribute("name", "title");
                    viewAllTitleAttribute.attribute( "value", "View All " + pluralOf(StringUtils.uncamelCase(entity.getName())));
                    Node viewAllBodyAttribute = new Node("put-attribute", viewAllDefinition);
                    viewAllBodyAttribute.attribute("name", "body");
                    viewAllBodyAttribute.attribute("value", "/WEB-INF/views" + targetDir + entity.getName() + "/"
                            + entityPlural.toLowerCase() + ".jsp");
                }

                // Generate view (single)

                this.entityMetawidget.setReadOnly(true);
                writeEntityMetawidget(context, this.viewTemplateEntityMetawidgetIndent, this.viewTemplateNamespaces);
    
                result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/views" + targetDir + entity.getName()
                        + "/view" + entity.getName() + ".jsp"), this.viewTemplate.render(context), overwrite));

                if (!tilesDefinitionExists("view" + entity.getName(), tilesDefinitions))
                {
                    Node viewDefinition = new Node("definition", tilesDefinitions);
                    viewDefinition.attribute("name", "view" + entity.getName());
                    viewDefinition.attribute("extends", tilesName);
                    Node viewTitleAttribute = new Node("put-attribute", viewDefinition);
                    viewTitleAttribute.attribute("name", "title");
                    viewTitleAttribute.attribute("value", "View " + StringUtils.uncamelCase(entity.getName()));
                    Node viewBodyAttribute = new Node("put-attribute", viewDefinition);
                    viewBodyAttribute.attribute("name", "body");
                    viewBodyAttribute.attribute("value", "/WEB-INF/views" + targetDir + entity.getName() + "/view"
                            + entity.getName() + ".jsp");
                }

                // Generate index

                if (!tilesDefinitionExists(targetDir + "index", tilesDefinitions))
                {
                    Node indexDefinition = new Node("definition", tilesDefinitions);
                    indexDefinition.attribute("name", targetDir + "index");
                    indexDefinition.attribute("extends", "standard");
                    Node indexTitleAttribute = new Node("put-attribute", indexDefinition);
                    indexTitleAttribute.attribute("name", "title");
                    indexTitleAttribute.attribute("value", "Welcome to Forge");
                    Node indexBodyAttribute = new Node("put-attribute", indexDefinition);
                    indexBodyAttribute.attribute("name", "body");
                    indexBodyAttribute.attribute("value", "/WEB-INF/views" + targetDir + "index.jsp");
                }

                String viewsFile = XMLParser.toXMLString(tilesDefinitions);

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

                context.put("mvcPackage", meta.getTopLevelPackage() + ".mvc.root");
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
        finally {

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
    public List<Resource<?>> generateIndex(String targetDir, Resource<?> template, boolean overwrite)
    {
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

        try
        {
/*            WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

            result.add(ScaffoldUtil.createOrOverwrite(this.prompt,
                    web.getWebResource("/resources/scaffold/paginator.xhtml"),
                    getClass().getResourceAsStream("/resources/scaffold/paginator.xhtml"),
                    overwrite));

            result.add(generateNavigation(targetDir, overwrite));*/
        } catch (Exception e)
        {
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

    protected Resource<?> setupMVCContext(String targetDir)
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
        MetadataFacet meta = this.project.getFacet(MetadataFacet.class);

        String filename = new String();
        String mvcPackage = new String();

        if (targetDir.equals("/"))
        {
            filename = "WEB-INF/" + meta.getProjectName().replace(' ', '-').toLowerCase() + "-mvc-context.xml";
            mvcPackage = meta.getTopLevelPackage() + ".mvc.root";
        }
        else
        {
            filename = "WEB-INF/" + targetDir.substring(1, targetDir.length()-1).replace(' ', '-') + "-mvc-context.xml";
            mvcPackage = meta.getTopLevelPackage() + ".mvc" + targetDir.replace('/', '.').toLowerCase();
        }

        // Create or update an mvc-context.xml file for the web application.

        Resource<?> mvcContext = web.getWebResource(filename);
        Node beans = new Node("beans");

        if (mvcContext.exists())
        {
            beans = XMLParser.parse(mvcContext.getResourceInputStream());
        }

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

        if (beans.get("context:component-scan").isEmpty())
        {
            Node componentScan = new Node("context:component-scan", beans);
            componentScan.attribute("base-package", mvcPackage);
        }
        else
        {
            boolean exists = false;

            for (Node node : beans.get("context:component-scan"))
            {
                if (node.getAttribute("base-package").equals(mvcPackage))
                {
                    exists = true;
                }
            }

            if (exists == false)
            {
                Node componentScan = new Node("context:component-scan", beans);
                componentScan.attribute("base-package", mvcPackage);
            }
        }

        // Map view names to Tiles Definitions with support for partial re-rendering

        boolean viewResolverExists = false, tilesConfigurerExists = false;

        for (Node bean : beans.get("bean"))
        {
            if (bean.getAttribute("id").equals("viewResolver"))
            {
                viewResolverExists = true;
            }

            if (bean.getAttribute("id").equals("tilesConfigurer"))
            {
                tilesConfigurerExists = true;
            }
        }

        if (viewResolverExists == false)
        {
            Node viewResolver = new Node("bean", beans);
            viewResolver.attribute("class", "org.springframework.web.servlet.view.tiles2.TilesViewResolver");
            viewResolver.attribute("id", "viewResolver");

            Node viewClass = new Node("property", viewResolver);
            viewClass.attribute("name", "viewClass");
            viewClass.attribute("value", "org.springframework.web.servlet.view.tiles2.TilesView");
        }

        // Initialize the Apache Tiles CompositeView system

        if (tilesConfigurerExists == false)
        {
            Node tilesConfigurer = new Node("bean", beans);
            tilesConfigurer.attribute("class", "org.springframework.web.servlet.view.tiles2.TilesConfigurer");
            tilesConfigurer.attribute("id", "tilesConfigurer");
    
            Node definitions = new Node("property", tilesConfigurer);
            definitions.attribute("name", "definitions");
            Node list = new Node("list", definitions);
            list.createChild("value").text("/WEB-INF/**/layouts.xml");
            list.createChild("value").text("/WEB-INF/**/views.xml");
        }

        // Indicate the use of annotations for Spring MVC, such as @Controller or @RequestMapping

        if (beans.getSingle("mvc:annotation-driven") == null)
        {
            beans.createChild("mvc:annotation-driven");
        }

        // If the servlet is mapped to '/', use the Spring MVC default servlet handler

        if (beans.getSingle("mvc:default-servlet-handler") == null && targetDir.equals("/"))
        {
            beans.createChild("mvc:default-servlet-handler");
        }

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

        if (beans.getSingle("mvc:resources") == null && targetDir.equals("/"))
        {
            Node mvcStaticContent = new Node("mvc:resources", beans);
            mvcStaticContent.attribute("mapping", "/static/**");
            mvcStaticContent.attribute("location", targetDir);
        }

        // Write the mvc-context file to 'src/main/webapp/WEB-INF/{lowercase-project-name}-mvc-context.xml'.

        web.createWebResource(XMLParser.toXMLString(beans), filename);
        
        return web.getWebResource(filename);
    }

    protected Resource<?> setupTilesLayout(String targetDir)
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

        Node tilesDefinitions = new Node("tiles-definitions");

        if (web.getWebResource("WEB-INF/layouts/layouts.xml").exists())
            tilesDefinitions = XMLParser.parse(web.getWebResource("WEB-INF/layouts/layouts.xml").getResourceInputStream());

        if (!tilesDefinitionExists("standard", tilesDefinitions))
        {
            Node standardDefinition = new Node("definition", tilesDefinitions);
            standardDefinition.attribute("name", "standard");
            standardDefinition.attribute("template", "/WEB-INF/layouts/pageTemplate.jsp");
        }

        if (!targetDir.equals("/"))
        {
            if(!tilesDefinitionExists(targetDir.substring(1, targetDir.length()-1), tilesDefinitions))
            {
                Node targetDirDefinition = new Node("definition", tilesDefinitions);
                targetDirDefinition.attribute("name", targetDir.substring(1, targetDir.length()-1));
                targetDirDefinition.attribute("template", "/WEB-INF/layouts/" + targetDir.substring(1, targetDir.length()-1) + "Template.jsp");
            }
        }

        String tilesDefinitionFile = XMLParser.toXMLString(tilesDefinitions);

        // TODO: Find a cleaner way to add Tiles DTD than this.

        tilesDefinitionFile = tilesDefinitionFile.substring(0, 55) + "\n<!DOCTYPE tiles-definitions PUBLIC\n\"-//Apache Software Foundation"
        + "//DTD Tiles Configuration 2.0//EN\"\n\"http://tiles.apache.org/dtds/tiles-config_2_0.dtd\">\n\n" + tilesDefinitionFile.substring(55);

        return web.createWebResource(tilesDefinitionFile, "/WEB-INF/layouts/layouts.xml"); 
    }

    protected Resource<?> updateWebXML(String targetDir)
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
        MetadataFacet meta = this.project.getFacet(MetadataFacet.class);

        String filename = new String();
        String servletName = new String();

        if (targetDir.equals("/"))
        {
            filename = "WEB-INF/" + meta.getProjectName().replace(' ', '-').toLowerCase() + "-mvc-context.xml";
            servletName = meta.getProjectName().replace(' ', '-').toLowerCase();
        }
        else
        {
            filename = "WEB-INF/" + targetDir.substring(1, targetDir.length()-1).replace(' ', '-') + "-mvc-context.xml";
            servletName = targetDir.substring(1, targetDir.length()-1).replace(' ', '-');
        }

        // Retrieve the existing web.xml file

        FileResource<?> webXML = web.getWebResource("WEB-INF/web.xml");
        Node webapp = XMLParser.parse(webXML.getResourceInputStream());

        // Define a dispatcher servlet, named after the project.
       
        if (!servletExists(servletName))
        {
            Node servlet = new Node("servlet", webapp);

            Node servName = new Node("servlet-name", servlet);
            servName.text(servletName);

            Node servletClass = new Node("servlet-class", servlet);
            servletClass.text("org.springframework.web.servlet.DispatcherServlet");

            Node initParam = new Node("init-param", servlet);
            Node paramName = new Node("param-name", initParam);
            paramName.text("contextConfigLocation");
            Node paramValue = new Node("param-value", initParam);
            paramValue.text(filename);
            Node loadOnStartup = new Node("load-on-startup", servlet);
            loadOnStartup.text(1);

            Node servletMapping = new Node("servlet-mapping", webapp);
            Node servletNameRepeat = new Node("servlet-name", servletMapping);
            servletNameRepeat.text(servletName);
            Node url = new Node("url-pattern", servletMapping);
            if (targetDir.equals("/"))
                url.text(targetDir);
            else
                url.text(targetDir + "*");
        }

        // Add a unique mapping for the error page

        // TODO: This may need to be modified later, to allow multiple error page locations.

/*        if (webapp.get("error-page").isEmpty())
        {
            Node errorPage = new Node("error-page", webapp);
            Node exceptionType = new Node("exception-type", errorPage);
            exceptionType.text("java.lang.Exception");
            Node location = new Node("location", errorPage);
            location.text("/WEB-INF/views/error.jsp");            
        }*/

        // Save the updated web.xml file

        web.createWebResource(XMLParser.toXMLString(webapp), "WEB-INF/web.xml");

        updateRootServlet();

        return web.getWebResource("WEB-INF/web.xml");
    }

    protected void loadTemplates()
    {
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

        if (this.viewAllTemplate == null)
        {
            this.viewAllTemplate = compiler.compile(VIEW_ALL_TEMPLATE);
        }

        if (this.viewTemplate == null)
        {
            this.viewTemplate = compiler.compile(VIEW_TEMPLATE);
            String template = Streams.toString(this.viewTemplate.getSourceTemplateResource().getInputStream());
            this.viewTemplateEntityMetawidgetIndent = parseIndent(template, "@{metawidget}");
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

        if (!targetDir.endsWith("/"))
            targetDir += "/";

        ResourceFilter filter = new ResourceFilter()
        {
            @Override
            public boolean accept(Resource<?> resource)
            {
                FileResource<?> file = (FileResource<?>) resource;

                if ( !file.isDirectory() || file.getName().equals("META-INF")
                        || file.getName().equals("WEB-INF")
                        || file.getName().equals("resources")
                        || file.getName().equals("layouts")
                        || file.getName().equals("views"))
                {
                    return false;
                }

                return true;
            }
        };
        
        for (Resource<?> resource : web.getWebResource("WEB-INF/views" + targetDir).listResources(filter))
        {
            HtmlAnchor link = new HtmlAnchor();
            if (java.getJavaResource(domainPackage + StringUtils.SEPARATOR_DOT_CHAR + resource.getName()).exists())
                link.putAttribute("href", "<c:url value=\"" + targetDir + pluralOf(resource.getName()).toLowerCase() + "/\"/>");
            else
                link.putAttribute("href", "<c:url value=\"" + targetDir + resource.getName() + "\"/>");
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
            return ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("WEB-INF/layouts/" + targetDir.substring(1,
                    targetDir.length()-1) + "Template.jsp"), this.navigationTemplate.render(context), overwrite);
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
    }

    /**
     * Checks whether a servlet with the name 'servletName' is defined in WEB-INF/web.xml.
     */

    protected boolean servletExists(String servletName)
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
        Node webapp = XMLParser.parse(web.getWebResource("WEB-INF/web.xml").getResourceInputStream());

        for (Node servlet : webapp.get("servlet"))
        {
            if (servlet.getSingle("servlet-name").getText().equals(servletName))
                return true;
        }

        return false;
    }

    /**
     * Check if there is a servlet mapped to '/'.  If not, create one and an appropriate context file.
     */

    protected void updateRootServlet()
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
        MetadataFacet meta = this.project.getFacet(MetadataFacet.class);
        Node webapp = XMLParser.parse(web.getWebResource("WEB-INF/web.xml").getResourceInputStream());
        boolean hasRootServlet = false;
        String filename = "WEB-INF/" + meta.getProjectName().replace(' ', '-').toLowerCase() + "-mvc-context.xml";

        for (Node servletMapping : webapp.get("servlet-mapping"))
        {
            if (servletMapping.getSingle("url-pattern").getText().equals("/"))
                hasRootServlet = true;
        }

        if (hasRootServlet == true)
        {
            updateMVCContext(filename);
        }
        else
        {
            setupMVCContext("/");

            Node servlet = new Node("servlet", webapp);

            Node servName = new Node("servlet-name", servlet);
            servName.text("root");

            Node servletClass = new Node("servlet-class", servlet);
            servletClass.text("org.springframework.web.servlet.DispatcherServlet");

            Node initParam = new Node("init-param", servlet);
            Node paramName = new Node("param-name", initParam);
            paramName.text("contextConfigLocation");
            Node paramValue = new Node("param-value", initParam);
            paramValue.text(filename);
            Node loadOnStartup = new Node("load-on-startup", servlet);
            loadOnStartup.text(1);

            Node servletMapping = new Node("servlet-mapping", webapp);
            Node servletNameRepeat = new Node("servlet-name", servletMapping);
            servletNameRepeat.text("root");
            Node url = new Node("url-pattern", servletMapping);
            url.text("/");

            web.createWebResource(XMLParser.toXMLString(webapp), "WEB-INF/web.xml");
        }
    }

    /**
     * Add an <mvc:resources location="/" mapping="/static/**"/> to the root servlet definition.
     */
    
    protected void updateMVCContext(String filename)
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
        Node beans = XMLParser.parse(web.getWebResource(filename).getResourceInputStream());

        if (beans.getSingle("mvc:resources") == null)
        {
            Node resources = new Node("mvc:resources");
            resources.attribute("location", "/");
            resources.attribute("mapping", "/static/**");
        }

        web.createWebResource(XMLParser.toXMLString(beans), filename);
    }

    /**
     * Check if the passed node, tilesDefinition, contains an Apache Tiles2 <definition> with name="tilesName" as an attribute.
     */
    
    protected boolean tilesDefinitionExists(String tilesName, Node tilesDefinitions)
    {
        for (Node definition : tilesDefinitions.get("definition"))
        {
            if (definition.getAttribute("name").equals(tilesName))
                return true;
        }

        return false;
    }

    protected Map<Object, Object> findEntityRelationships(JavaClass entity, Map<Object, Object> context)
    {
        List<String> entityNames = new ArrayList<String>();
        List<String> entityClasses = new ArrayList<String>();
        List<String> ccEntityClasses = new ArrayList<String>();

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

        return context;
    }
}
