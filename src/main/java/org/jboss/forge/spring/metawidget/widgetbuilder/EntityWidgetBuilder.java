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

package org.jboss.forge.spring.metawidget.widgetbuilder;

import static org.jboss.forge.spring.metawidget.inspector.ForgeInspectionResultConstants.*;
import static org.metawidget.inspector.InspectionResultConstants.*;
import static org.metawidget.inspector.jsp.JspInspectionResultConstants.*;
import static org.jvnet.inflector.Noun.pluralOf;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.metawidget.statically.StaticWidget;
import org.metawidget.statically.StaticXmlMetawidget;
import org.metawidget.statically.StaticXmlStub;
import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.jsp.StaticJspUtils;
import org.metawidget.statically.jsp.html.StaticHtmlMetawidget;
import org.metawidget.statically.jsp.html.widgetbuilder.CoreForEach;
import org.metawidget.statically.jsp.html.widgetbuilder.CoreOut;
import org.metawidget.statically.jsp.html.widgetbuilder.HtmlTable;
import org.metawidget.statically.jsp.html.widgetbuilder.HtmlTableCell;
import org.metawidget.statically.jsp.html.widgetbuilder.HtmlTableRow;
import org.metawidget.statically.jsp.html.widgetbuilder.HtmlWidgetBuilder;
import org.metawidget.statically.jsp.html.widgetprocessor.StandardBindingProcessor;
import org.metawidget.statically.spring.StaticSpringMetawidget;
import org.metawidget.statically.spring.widgetbuilder.FormOptionTag;
import org.metawidget.statically.spring.widgetbuilder.FormOptionsTag;
import org.metawidget.statically.spring.widgetbuilder.FormSelectTag;
import org.metawidget.util.ClassUtils;
import org.metawidget.util.CollectionUtils;
import org.metawidget.util.WidgetBuilderUtils;
import org.metawidget.util.simple.StringUtils;
import org.w3c.dom.NodeList;

/**
 * Builds widgets with Forge-specific behaviours, such as create/deletion support.
 *
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class EntityWidgetBuilder
        extends HtmlWidgetBuilder {
    
    //
    // Public methods
    //
    
    @Override
    public StaticXmlWidget buildWidget(String elementName, Map<String, String> attributes, StaticXmlMetawidget metawidget) {
        
        // Suppress nested INVERSE ONE_TO_ONE, to avoid recursion
        
        if (TRUE.equals(attributes.get(ONE_TO_ONE)) && TRUE.equals(attributes.get(INVERSE_RELATIONSHIP))
                && metawidget.getParent() != null)
        {
            return new StaticXmlStub();
        }
        
        // Render read-only JSP_LOOKUP as a link.
        
        if (WidgetBuilderUtils.isReadOnly(attributes))
        {
            if (attributes.containsKey(JSP_LOOKUP))
            {
                {
                    String entity = ClassUtils.getSimpleName(WidgetBuilderUtils.getActualClassOrType(attributes));
                    entity = StringUtils.decapitalize(entity);
                    
                    HtmlAnchor link = new HtmlAnchor();
                    new StandardBindingProcessor().processWidget(link, elementName, attributes, (StaticHtmlMetawidget) metawidget);
                    link.setTextContent(link.getAttribute("value"));
                    CoreOut cout = new CoreOut();
                    cout.putAttribute("value", "/" + pluralOf(entity) + "/"  + 
                            StaticJspUtils.wrapExpression(StaticJspUtils.unwrapExpression(link.getAttribute("value")) + ".id"));
                    link.putAttribute("value", null);
                    link.putAttribute("href", cout.toString());
                    
                    return link;
                }
            }
        }
        
        // Render collection tables with links
        
        String type = WidgetBuilderUtils.getActualClassOrType(attributes);
        
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
                
                // Create nestedMetawidget with conditional 'rendered' attribute
                
                StaticSpringMetawidget nestedMetawidget = new StaticSpringMetawidget();
                metawidget.initNestedMetawidget(nestedMetawidget, attributes);
                String unwrappedExpression = StaticJspUtils.unwrapExpression(nestedMetawidget.getValue());
                nestedMetawidget.putAttribute("rendered", StaticJspUtils.wrapExpression("!empty" + unwrappedExpression));
                
                // If read-only, we're done
                
                if(WidgetBuilderUtils.isReadOnly(attributes))
                {
                    return nestedMetawidget;
                }
                
                // Otherwise, further wrap it with a button.
                
                int lastIndexOf = unwrappedExpression.lastIndexOf('.');
                String childExpression = unwrappedExpression.substring(lastIndexOf + 1);
                
                HtmlAnchor createLink = new HtmlAnchor();
                String entity = ClassUtils.getSimpleName(WidgetBuilderUtils.getActualClassOrType(attributes));
                entity = pluralOf(entity);
                CoreOut cout = new CoreOut();
                cout.putAttribute("value", "/" + entity + "/create");
                createLink.setTextContent("Create New " + StringUtils.uncamelCase(childExpression));
                createLink.putAttribute("href", cout.toString());
                createLink.putAttribute("rendered", StaticJspUtils.wrapExpression("empty" + unwrappedExpression));
                
                HtmlTableRow row = new HtmlTableRow();
                HtmlTableCell createLinkCell = new HtmlTableCell();
                HtmlTableCell nestedMetawidgetCell = new HtmlTableCell();
                createLinkCell.getChildren().add(createLink);
                row.getChildren().add(createLinkCell);
                nestedMetawidgetCell.getChildren().add(nestedMetawidget);
                row.getChildren().add(nestedMetawidget);
                return row;
            }
            
            Class<?> clazz = ClassUtils.niceForName(type);
            
            if(clazz != null)
            {
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
            bindingProcessor.processWidget(table, elementName, attributes, (StaticHtmlMetawidget) metawidget);
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
            row.getChildren().add(select);
        }
        
        return row;
    }

    /**
     * Overridden to add a 'delete' column.
     */
    
    protected void addColumnComponents(HtmlTable table, CoreForEach forEach, Map<String, String> attributes, NodeList elements, StaticXmlMetawidget metawidget)
    {
        // super.addColumnComponents(table, forEach, attributes, elements, metawidget);
        
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
        String entity = ClassUtils.getSimpleName(WidgetBuilderUtils.getActualClassOrType(attributes));
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
            Map<String, String> columnAttributes)
    {
        // Suppress columns that show Collection values (their toString is never very nice)
        
        if(TRUE.equals(columnAttributes.get(N_TO_MANY)))
        {
            return;
        }
        
        // FORGE-448: Don't display "owner" when showing relationships
        
        String columnName = columnAttributes.get(NAME);
        
        if(columnName.equals(tableAttributes.get(INVERSE_RELATIONSHIP)))
        {
            return;
        }
        
        // Create the column
        
        super.addColumnComponent(row, forEach, tableAttributes, elementName, columnAttributes);
        List<StaticWidget> columns = forEach.getChildren().get(0).getChildren();
        HtmlTableCell column = (HtmlTableCell) columns.get(columns.size()-1);
        
        // If we can determine the component type, wrap it with a link
        
        String componentType = WidgetBuilderUtils.getComponentType(tableAttributes);
        
        if(componentType != null)
        {
            String entity = StringUtils.decapitalize(ClassUtils.getSimpleName(componentType));
            
            // Get the original column text...
            
            StaticXmlWidget originalComponent = (StaticXmlWidget) column.getChildren().remove(0);
            
            // ...and create a link with the same value.
            
            HtmlAnchor link = new HtmlAnchor();
            CoreOut cout = new CoreOut();
            cout.putAttribute( "value", "/" + pluralOf(entity) + "/" + StaticJspUtils.wrapExpression(forEach.getAttribute("var") + ".id"));
            link.putAttribute("href", cout.toString());
            link.putAttribute("value", originalComponent.getAttribute("value"));
            
            column.getChildren().add(link);
            
            // Ignore bi-directional case
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
        
        return select;
    }
}