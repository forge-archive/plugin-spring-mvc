package org.jboss.forge.scaffold.spring.metawidget.widgetbuilder;

import static org.metawidget.inspector.InspectionResultConstants.INVERSE_RELATIONSHIP;
import static org.metawidget.inspector.InspectionResultConstants.NAME;
import static org.metawidget.inspector.InspectionResultConstants.TRUE;

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
import org.metawidget.statically.jsp.widgetbuilder.CoreOut;
import org.metawidget.statically.jsp.widgetbuilder.JspWidgetBuilder;
import org.metawidget.util.ClassUtils;
import org.metawidget.util.WidgetBuilderUtils;
import org.metawidget.util.simple.StringUtils;

public class JspEntityWidgetBuilder extends JspWidgetBuilder {
	
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
            
            
            CoreOut curl2 = new CoreOut();
            String valueExpression = StaticJspUtils.wrapExpression(forEach.getAttribute("var") + "." + columnAttributes.get(NAME));
            curl2.setValue(valueExpression);
            column.getChildren().remove(0);
            column.getChildren().add(curl2);
            
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
