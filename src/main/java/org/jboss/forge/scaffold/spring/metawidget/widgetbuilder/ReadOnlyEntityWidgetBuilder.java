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

        if (clazz != null && Collection.class.isAssignableFrom(clazz))
        {
            if (TRUE.equals(attributes.get("search-result")))
            {
                return new StaticXmlStub();
            }
            else
            {
                return null;
            }
        }

        if (attributes.containsKey(INVERSE_RELATIONSHIP) && WidgetBuilderUtils.isReadOnly(attributes))
        {
            return new CoreOut();
        }

        if (widget == null && WidgetBuilderUtils.isReadOnly(attributes))
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
