package org.jboss.forge.scaffold.spring.metawidget.widgetprocessor;

import static org.metawidget.inspector.InspectionResultConstants.*;
import static org.jboss.forge.scaffold.spring.metawidget.inspector.ForgeInspectionResultConstants.*;

import java.util.Map;

import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.jsp.StaticJspMetawidget;
import org.metawidget.statically.spring.widgetbuilder.FormSelectTag;
import org.metawidget.statically.spring.widgetprocessor.PathProcessor;

public class EntityPathProcessor extends PathProcessor
{
    //
    // Public methods
    //

    @Override
    public StaticXmlWidget processWidget(StaticXmlWidget widget, String elementName, Map<String, String> attributes, StaticJspMetawidget metawidget)
    {
        super.processWidget(widget, elementName, attributes, metawidget);

        // Only override the processed widget if there is a ONE_TO_ONE relationship

        if (TRUE.equals(attributes.get(ONE_TO_ONE)) || attributes.containsKey(REVERSE_PRIMARY_KEY_TYPE))
        {
            if (widget instanceof FormSelectTag)
            {
                widget.putAttribute("path", attributes.get(NAME) + ".id");
            }
        }

        return widget;
    }
}
