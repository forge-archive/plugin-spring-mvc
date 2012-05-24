package org.jboss.forge.scaffold.spring.metawidget.layout;

import static org.metawidget.inspector.InspectionResultConstants.HIDDEN;
import static org.metawidget.inspector.InspectionResultConstants.REQUIRED;
import static org.metawidget.inspector.InspectionResultConstants.TRUE;

import java.util.Map;

import org.metawidget.layout.iface.LayoutException;
import org.metawidget.statically.StaticXmlStub;
import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.html.StaticHtmlMetawidget;
import org.metawidget.statically.html.layout.HtmlTableLayoutConfig;
import org.metawidget.statically.html.widgetbuilder.HtmlTableBody;
import org.metawidget.statically.html.widgetbuilder.HtmlTableCell;
import org.metawidget.statically.html.widgetbuilder.HtmlTableRow;
import org.metawidget.statically.spring.layout.SpringTableLayout;
import org.metawidget.util.WidgetBuilderUtils;

public class ScaffoldTableLayout extends SpringTableLayout
{
    public ScaffoldTableLayout()
    {
        super();
    }

    public ScaffoldTableLayout(HtmlTableLayoutConfig config)
    {
        super(config);
    }

    @Override
    public void layoutWidget(StaticXmlWidget widget, String elementName, Map<String, String> attributes, StaticXmlWidget container, StaticHtmlMetawidget metawidget)
    {
        try
        {
            // Ignore stubs

            if (widget instanceof StaticXmlStub && widget.getChildren().isEmpty())
            {
                return;
            }

            HtmlTableBody body = (HtmlTableBody) container.getChildren().get(0).getChildren().get(0);
            HtmlTableRow row = new HtmlTableRow();

            // Label

            layoutLabel(row, widget, elementName, attributes, metawidget);

            // Add widget to layout

            if (widget instanceof HtmlTableCell)
            {
                row.getChildren().add(widget);
            }
            else
            {
                HtmlTableCell cell = new HtmlTableCell();
                cell.getChildren().add(widget);
                row.getChildren().add(cell);
            }

            // Indicate whether the field is required or not.

            HtmlTableCell requiredCell = new HtmlTableCell();

            if (TRUE.equals(attributes.get(REQUIRED)) && !WidgetBuilderUtils.isReadOnly(attributes) && !TRUE.equals(attributes.get(HIDDEN)))
            {
                requiredCell.setTextContent("*");
            }

            row.getChildren().add( requiredCell );
            body.getChildren().add( row );

        }
        catch (Exception e)
        {
            throw LayoutException.newException(e);
        }
    }

}
