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
import static org.jvnet.inflector.Noun.pluralOf;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.metawidget.statically.StaticWidget;
import org.metawidget.statically.StaticXmlMetawidget;
import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.html.widgetbuilder.HtmlTable;
import org.metawidget.statically.html.widgetbuilder.HtmlTableCell;
import org.metawidget.statically.html.widgetbuilder.HtmlTableRow;
import org.metawidget.statically.jsp.StaticJspMetawidget;
import org.metawidget.statically.jsp.StaticJspUtils;
import org.metawidget.statically.jsp.widgetprocessor.StandardBindingProcessor;
import org.metawidget.statically.jsp.widgetbuilder.CoreForEach;
import org.metawidget.statically.jsp.widgetbuilder.CoreOut;
import org.metawidget.statically.jsp.widgetbuilder.JspWidgetBuilder;
import org.metawidget.statically.spring.widgetbuilder.FormOptionTag;
import org.metawidget.statically.spring.widgetbuilder.FormOptionsTag;
import org.metawidget.statically.spring.widgetbuilder.FormSelectTag;
import org.metawidget.util.ClassUtils;
import org.metawidget.util.CollectionUtils;
import org.metawidget.util.WidgetBuilderUtils;
import org.metawidget.util.XmlUtils;
import org.metawidget.util.simple.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Builds widgets with Forge-specific behaviours, such as create/deletion support.
 *
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class EntityWidgetBuilder
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
        
        String type = WidgetBuilderUtils.getActualClassOrType(attributes);

        Class<?> clazz = ClassUtils.niceForName(type);
        
        // Render collection tables with links
              
        if(type != null)
        {
            // Render non-optional ONE_TO_ONE with a button
            
            if(TRUE.equals(attributes.get(ONE_TO_ONE)) && !TRUE.equals(attributes.get(REQUIRED)))
            {
                // (we are about to create a nestedMetawidget, so we must prevent recursion)
                
                if(ENTITY.equals(elementName))
                {
                    return null;
                }
                
                if(Collection.class.isAssignableFrom(clazz))
                {
                    return createDataTableComponent(elementName, attributes, metawidget);
                }
            }
        }
        
        // Otherwise, delegate to the next WidgetBuilder in the chain
        
        return null;
    }
    
    //
    // Protected methods
    //
    
    /**
     * Overridden to add row creation/deletion.
     */
    
    @Override
    protected StaticXmlWidget createDataTableComponent(String elementName, Map<String, String> attributes, StaticXmlMetawidget metawidget)
    {
        // Create the normal table
        
        StaticXmlWidget table = super.createDataTableComponent(elementName, attributes, metawidget);
        
        StandardBindingProcessor bindingProcessor = metawidget.getWidgetProcessor(StandardBindingProcessor.class);
        
        if(bindingProcessor != null)
        {
            bindingProcessor.processWidget(table, elementName, attributes, (StaticJspMetawidget) metawidget);
        }
               
        // Add row creation/deletion for OneToMany and ManyToMany
        
        if(!TRUE.equals(attributes.get(N_TO_MANY)) || WidgetBuilderUtils.isReadOnly(attributes))
        {
            return table;
        }
        
        String componentType = WidgetBuilderUtils.getComponentType(attributes);
        
        if(componentType == null)
        {
            return table;
        }
        
        HtmlTableRow row = new HtmlTableRow();
        
        // If not bidirectional, create an 'Add' section (bidirectional does it in place)
        
        if (!attributes.containsKey(INVERSE_RELATIONSHIP))
        {
            // Create select menu
            
            String simpleComponentType = ClassUtils.getSimpleName(componentType);
            String controllerName = StringUtils.decapitalize(simpleComponentType);
            Map<String, String> emptyAttributes = CollectionUtils.newHashMap();
            FormSelectTag select = createFormSelectTag(controllerName, emptyAttributes);
            select.putAttribute("multiple", "multiple");
            HtmlTableCell selectCell = new HtmlTableCell();
            selectCell.getChildren().add(select);
            row.getChildren().add(selectCell);
            table.getChildren().get(1).getChildren().add(row);
        }
        
        return table;
    }

    /**
     * Overridden to add a 'delete' column.
     */
    
    protected void addColumnComponents(HtmlTable table, CoreForEach forEach, Map<String, String> attributes, NodeList elements, StaticXmlMetawidget metawidget)
    {
        super.addColumnComponents(table, forEach, attributes, elements, metawidget);
        
        if(forEach.getChildren().isEmpty())
        {
            return;
        }
        
        if(!attributes.containsKey(N_TO_MANY) || WidgetBuilderUtils.isReadOnly(attributes))
        {
            return;
        }
        
        HtmlAnchor removeLink = new HtmlAnchor();
        removeLink.setTextContent("Remove");
        String entity = StringUtils.decapitalize(ClassUtils.getSimpleName(attributes.get(PARAMETERIZED_TYPE)));
        String removeExpression = entity + ".id";
        removeExpression = StaticJspUtils.wrapExpression(removeExpression);
        CoreOut cout = new CoreOut();
        cout.putAttribute("value", "/" + pluralOf(entity) + "/" + removeExpression + "/remove");
        removeLink.putAttribute("href", cout.toString());
        
        HtmlTableCell cell = new HtmlTableCell();
        cell.getChildren().add(removeLink);
        
        forEach.getChildren().get(0).getChildren().add(cell);
        
        return;
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

        String type = WidgetBuilderUtils.getActualClassOrType(columnAttributes);

        if (type != null)
        {
            Class<?> clazz = ClassUtils.niceForName(type);

            if (clazz != null && Collection.class.isAssignableFrom(clazz))
            {
                return;
            }
        }

        // FORGE-446: Expand columns that show one-to-one values.

        String componentType = WidgetBuilderUtils.getComponentType(tableAttributes);

        if (TRUE.equals(columnAttributes.get(ONE_TO_ONE)))
        {
            String columnType = columnAttributes.get(TYPE);
            String inspectedType = metawidget.inspect(null, columnType);

            if (inspectedType == null)
            {
                Element root = XmlUtils.documentFromString(inspectedType).getDocumentElement();
                NodeList elements = root.getFirstChild().getChildNodes();
                Map<String, String> embeddedAttributes = CollectionUtils.newHashMap();
                embeddedAttributes.put(TOP_LEVEL_PARAMETERIZED_TYPE, componentType);
                embeddedAttributes.put(PARAMETERIZED_TYPE, columnType);

                // TODO: Figure out a reference to an HtmlDataTable for the 'addColumnComponents' method invocation.
                addColumnComponents(null, forEach, embeddedAttributes, elements, metawidget);
            }
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

        if (tableAttributes.get(TOP_LEVEL_PARAMETERIZED_TYPE) != null)
        {
            componentType = tableAttributes.get(TOP_LEVEL_PARAMETERIZED_TYPE);
        }

        if (componentType != null)
        {
            String controllerName = StringUtils.decapitalize(ClassUtils.getSimpleName(componentType));

            // Create a link...

            HtmlAnchor link = new HtmlAnchor();
            String targetExpression = "/scaffold/" + controllerName + "/view/";
            targetExpression += StaticJspUtils.wrapExpression(forEach.getAttribute("var") + ".id");
            link.putAttribute("href", targetExpression);
            link.getChildren().add(column.getChildren().remove(0));
            column.getChildren().add(link);

            // Ignore bidirectional case.            
        }
    }
    
    protected FormSelectTag createFormSelectTag(String expression, Map<String, String> attributes) {

        // Write the new SELECT tag
        
        FormSelectTag select = new FormSelectTag();
        
        // Empty option, as required
        
        if(WidgetBuilderUtils.needsEmptyLookupItem(attributes))
        {
            FormOptionTag emptyOption = new FormOptionTag();
            emptyOption.putAttribute("value", "");
            
            // Add the empty option to the SELECT tag
            
            select.getChildren().add(emptyOption);
        }
        
        // Options tag
        
        FormOptionsTag options = new FormOptionsTag();
        options.putAttribute("items", expression);
        select.getChildren().add(options);
        
        return select;
    }
}