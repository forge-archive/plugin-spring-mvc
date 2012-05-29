package org.jboss.forge.scaffold.spring.metawidget.widgetbuilder;

import static org.metawidget.inspector.InspectionResultConstants.*;

import java.util.Collection;
import java.util.Map;

import org.metawidget.statically.StaticXmlMetawidget;
import org.metawidget.statically.StaticXmlStub;
import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.jsp.widgetbuilder.CoreOut;
import org.metawidget.statically.jsp.widgetbuilder.ReadOnlyWidgetBuilder;
import org.metawidget.util.WidgetBuilderUtils;

public class ReadOnlyEntityWidgetBuilder extends ReadOnlyWidgetBuilder
{
    @Override
    public StaticXmlWidget buildWidget(String elementName, Map<String, String> attributes, StaticXmlMetawidget metawidget)
    {
        StaticXmlWidget widget = super.buildWidget(elementName, attributes, metawidget);

        Class<?> clazz = WidgetBuilderUtils.getActualClassOrType(attributes, null);

        if (widget instanceof StaticXmlStub)
        {
            return null;
        }

        if (Collection.class.isAssignableFrom(clazz))
        {
            return null;
        }

        if (widget == null)
        {
            if (attributes.get(NAME) != null)
            {
                return new CoreOut();
            }
            else
            {
                return null;
            }
        }

        return widget;
    }
}
