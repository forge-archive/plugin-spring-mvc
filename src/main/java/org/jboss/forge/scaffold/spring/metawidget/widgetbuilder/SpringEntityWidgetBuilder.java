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

package org.jboss.forge.scaffold.spring.metawidget.widgetbuilder;

import static org.jboss.forge.scaffold.spring.metawidget.inspector.ForgeInspectionResultConstants.*;
import static org.metawidget.inspector.InspectionResultConstants.*;
import static org.metawidget.inspector.spring.SpringInspectionResultConstants.*;

import java.util.Collection;
import java.util.Map;

import org.jboss.forge.env.Configuration;
import org.jboss.forge.parser.java.util.Strings;
import org.jboss.forge.scaffold.spring.SpringScaffold;
import org.jvnet.inflector.Noun;
import org.metawidget.iface.MetawidgetException;
import org.metawidget.statically.BaseStaticXmlWidget;
import org.metawidget.statically.StaticXmlStub;
import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.html.widgetbuilder.HtmlInput;
import org.metawidget.statically.html.widgetbuilder.HtmlTableCell;
import org.metawidget.statically.html.widgetbuilder.HtmlTableRow;
import org.metawidget.statically.jsp.StaticJspMetawidget;
import org.metawidget.statically.jsp.widgetbuilder.CoreOut;
import org.metawidget.statically.jsp.widgetprocessor.StandardBindingProcessor;
import org.metawidget.statically.layout.SimpleLayout;
import org.metawidget.statically.spring.StaticSpringMetawidget;
import org.metawidget.statically.spring.widgetbuilder.SpringWidgetBuilder;
import org.metawidget.util.ClassUtils;
import org.metawidget.util.WidgetBuilderUtils;
import org.metawidget.util.simple.StringUtils;

/**
 * Builds widgets with Forge-specific behaviours (such as links to other Scaffolding pages)
 *
 * @author Ryan Bradley
 */

public class SpringEntityWidgetBuilder
        extends SpringWidgetBuilder
{
    //
    // Private statics
    //

    /**
     * When expanding OneToOne or Embedded types in data table rows, we must point the row link to the original type, not
     * the type being expanded
     */

    private String TOP_LEVEL_PARAMETERIZED_TYPE = "top-level-parameterized-type";

    /**
     * Current Forge configuration.  Useful to retrieve <code>targetDir</code>.
     */

    private final Configuration config;

    //
    // Constructor
    //
    
    public SpringEntityWidgetBuilder(EntityWidgetBuilderConfig config)
    {
        this.config = config.getConfig();
    }

    //
    // Public methods
    //
    
    @Override
    public StaticXmlWidget buildWidget(String elementName, Map<String, String> attributes, StaticSpringMetawidget metawidget)
    {
        // Suppress nested INVERSE ONE_TO_ONE to avoid recursion.

        if (TRUE.equals(attributes.get(ONE_TO_ONE)) && TRUE.equals(attributes.get(INVERSE_RELATIONSHIP)))
        {
            return new StaticXmlStub();
        }

        String type = WidgetBuilderUtils.getActualClassOrType(attributes);

        if (WidgetBuilderUtils.isReadOnly(attributes))
        {
            // Render read-only Spring lookup as a link.

            if (attributes.containsKey(SPRING_LOOKUP))
            {
                // (unless parent is already a link)

                if (widgetIsLink((BaseStaticXmlWidget) metawidget.getParent()))
                {
                    return null;
                }

                String controllerName = Noun.pluralOf(ClassUtils.getSimpleName(type)).toLowerCase();
                CoreUrl curl = new CoreUrl();
                curl.setValue(getTargetDir() + "/" + controllerName);
    
                HtmlInput button = new HtmlInput();
                button.putAttribute("onclick", "window.location='" + curl.toString() + "'\"");
                button.putAttribute("type", "submit");
                button.putAttribute("value", StringUtils.uncamelCase(ClassUtils.getSimpleName(type)));
    
                return button;
            }

            Class<?> clazz = ClassUtils.niceForName(type);

            if (clazz != null)
            {
                // Render read-only booleans as graphics

                if (boolean.class.equals(clazz))
                {
                    CoreOut cout = new CoreOut();
                    StandardBindingProcessor processor = new StandardBindingProcessor();
                    processor.processWidget(cout, elementName, attributes, metawidget);

                    return cout;
                }
            }
        }

        // Render collection tables with links.

        if (type != null)
        {
            // Render non-optional ONE_TO_ONE with a button.

            if (TRUE.equals(attributes.get(ONE_TO_ONE)) && !TRUE.equals(attributes.get(REQUIRED)))
            {
                // (we are about to create a nested metawidget, so we must prevent recursion)

                if (ENTITY.equals(elementName))
                {
                    return null;
                }

                // Create nested StaticJspMetawidget to handle collections

                StaticJspMetawidget nestedMetawidget = new StaticJspMetawidget();
                metawidget.initNestedMetawidget(nestedMetawidget, attributes);

                // If read-only, we're done.

                if (WidgetBuilderUtils.isReadOnly(attributes))
                {
                    return nestedMetawidget;
                }

                // Otherwise, further wrap it with a button.

                String controllerName = Noun.pluralOf(ClassUtils.getSimpleName(type)).toLowerCase();
                CoreUrl curl = new CoreUrl();
                curl.setValue(getTargetDir() + "/" + controllerName + "/create");

                HtmlInput createButton = new HtmlInput();
                createButton.putAttribute("type", "submit");
                createButton.setValue("Create New " + StringUtils.uncamelCase(ClassUtils.getSimpleName(type)));
                createButton.putAttribute("onclick", "window.location='" + curl.toString() + "'\"");

                HtmlTableRow row = new HtmlTableRow();
                HtmlTableCell metawidgetCell = new HtmlTableCell();
                HtmlTableCell buttonCell = new HtmlTableCell();

                row.getChildren().add(buttonCell);
                buttonCell.getChildren().add(createButton);
                row.getChildren().add(metawidgetCell);
                metawidgetCell.getChildren().add(nestedMetawidget);

                return row;
            }

            Class<?> clazz = ClassUtils.niceForName(type);

            if (Collection.class.isAssignableFrom(clazz))
            {
                StaticJspMetawidget nestedMetawidget = new StaticJspMetawidget();
                nestedMetawidget.setInspector( metawidget.getInspector() );
                nestedMetawidget.setLayout( new SimpleLayout() );
                nestedMetawidget.setPath( metawidget.getPath() + StringUtils.SEPARATOR_FORWARD_SLASH_CHAR + attributes.get( NAME ) );

                // If using an external config, lookup StaticJspMetawidget within it

                if ( metawidget.getConfig() != null )
                {
                    nestedMetawidget.setConfig( metawidget.getConfig() );
                    try {
                        nestedMetawidget.getWidgetProcessors();
                    } catch ( MetawidgetException e ) {
                        nestedMetawidget.setConfig( null );
                    }
                }

                return nestedMetawidget;
            }
        }

        return null;
    }

    //
    // Private methods
    //

    private String getTargetDir()
    {
        String target = this.config.getString(SpringScaffold.class.getName() + "_targetDir");

        target = Strings.isNullOrEmpty(target) ? "" : target;

        if (!target.startsWith("/"))
            target = "/" + target;

        if (target.endsWith("/") && !target.startsWith("/"))
            target = target.substring(0, target.length()-1);

        return target;
    }

    private boolean widgetIsLink(BaseStaticXmlWidget widget)
    {
        if (widget instanceof HtmlAnchor)
        {
            return true;
        }

        if (widget instanceof HtmlInput && !widget.getAttribute("onclick").isEmpty())
        {
            return true;
        }

        return false;
    }
}
