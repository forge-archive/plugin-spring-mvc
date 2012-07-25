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

import static org.metawidget.inspector.InspectionResultConstants.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jvnet.inflector.Noun;
import org.metawidget.statically.StaticWidget;
import org.metawidget.statically.StaticXmlMetawidget;
import org.metawidget.statically.StaticXmlStub;
import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.html.widgetbuilder.HtmlTableCell;
import org.metawidget.statically.html.widgetbuilder.HtmlTableRow;
import org.metawidget.statically.jsp.StaticJspUtils;
import org.metawidget.statically.jsp.widgetbuilder.CoreForEach;
import org.metawidget.statically.jsp.widgetbuilder.JspWidgetBuilder;
import org.metawidget.util.ClassUtils;
import org.metawidget.util.WidgetBuilderUtils;
import org.metawidget.util.simple.StringUtils;

/**
 * Builds widgets with Forge-specific behaviours, such as create/deletion support.
 *
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class NToManyWidgetBuilder
        extends JspWidgetBuilder
{
    //
    // Private statics
    //

    private static final String TOP_LEVEL_PARAMETERIZED_TYPE = "top-level-parameterized-type";

    //
    // Public methods
    //

    @Override
    public StaticXmlWidget buildWidget(String elementName, Map<String, String> attributes, StaticXmlMetawidget metawidget)
    {
        // Get actual class or type, defaulting to a String if none is found.

        Class<?> clazz = WidgetBuilderUtils.getActualClassOrType(attributes, String.class);

        // Render collection tables with links
              
        if(clazz != null && Collection.class.isAssignableFrom(clazz))
        {
            return createDataTableComponent(elementName, attributes, metawidget);
        }
        
        // Otherwise, delegate to the next WidgetBuilder in the chain
        
        return super.buildWidget(elementName, attributes, metawidget);
    }
    
    //
    // Protected methods
    //

    /**
     * Overridden to add a CSS class to the produced HTML table.
     */

    @Override
    protected StaticXmlWidget createDataTableComponent(String elementName, Map<String, String> attributes, StaticXmlMetawidget metawidget)
    {
        // Create the normal table

        if (TRUE.equals(attributes.get("search")))
        {
            return new StaticXmlStub();
        }

        StaticXmlWidget table = super.createDataTableComponent(elementName, attributes, metawidget);
        table.putAttribute("class", "data-table");

        return table;
    }
   
    /**
     * Overridden to replace original column text with an <tt>a</tt> link, in those cases where we can determine a data type.
     */
    
    @Override
    protected void addColumnComponent(HtmlTableRow row, CoreForEach forEach, Map<String, String> tableAttributes, String elementName,
            Map<String, String> columnAttributes, StaticXmlMetawidget metawidget)
    {
        // Suppress columns that show Collection values.  Their toString is never very nice, and nested tables are awful.
        //
        // Note: we don't just do N_TO_MANY values, as Collections are sometimes not annotated.

        // Get actual class or type, defaulting to a String if none is found.

        Class<?> clazz = WidgetBuilderUtils.getActualClassOrType(columnAttributes, String.class);

        if (clazz != null && Collection.class.isAssignableFrom(clazz))
        {
            return;
        }

        // FORGE-448: Don't display "owner" when showing relationships.

        String columnName = columnAttributes.get(NAME);

        if (columnName.equals(tableAttributes.get(INVERSE_RELATIONSHIP)))
        {
            return;
        }

        // Create the column.

        super.addColumnComponent(row, forEach, tableAttributes, elementName, columnAttributes, metawidget);
        List<StaticWidget> columns = row.getChildren();
        HtmlTableCell column = (HtmlTableCell) columns.get(columns.size()-1);

        // If we can determine the component type, wrap it with a link.

        String componentType = WidgetBuilderUtils.getComponentType(tableAttributes);

        if (tableAttributes.get(TOP_LEVEL_PARAMETERIZED_TYPE) != null)
        {
            componentType = tableAttributes.get(TOP_LEVEL_PARAMETERIZED_TYPE);
        }

        if (componentType != null)
        {
            String controllerName = StringUtils.decapitalize(ClassUtils.getSimpleName(componentType));
            controllerName = Noun.pluralOf(controllerName).toLowerCase();

            // Create a link...

            HtmlAnchor link = new HtmlAnchor();
            CoreUrl curl = new CoreUrl();
            String targetExpression = "/" + controllerName + "/";
            targetExpression += StaticJspUtils.wrapExpression(forEach.getAttribute("var") + ".id");
            curl.setValue(targetExpression);
            link.putAttribute("href", curl.toString());
            link.getChildren().add(column.getChildren().remove(0));
            column.getChildren().add(link);
        }
    }
}