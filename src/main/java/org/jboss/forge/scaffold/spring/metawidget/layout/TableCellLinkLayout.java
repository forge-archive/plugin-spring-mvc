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

package org.jboss.forge.scaffold.spring.metawidget.layout;

import java.util.Map;

import org.jboss.forge.scaffold.spring.metawidget.widgetbuilder.CoreUrl;
import org.jboss.forge.scaffold.spring.metawidget.widgetbuilder.HtmlAnchor;
import org.jvnet.inflector.Noun;
import org.metawidget.statically.StaticXmlStub;
import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.html.StaticHtmlMetawidget;
import org.metawidget.statically.html.layout.HtmlLayout;
import org.metawidget.statically.html.widgetbuilder.HtmlTableCell;
import org.metawidget.statically.html.widgetbuilder.HtmlTableHeader;
import org.metawidget.statically.jsp.StaticJspUtils;

/**
 * Layout widgets wrapped in a HtmlTableCell, <td/>, surrounded by an HtmlTableRow, <tr/>
 * 
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class TableCellLinkLayout
    extends HtmlLayout
{
    @Override
    public void layoutWidget(StaticXmlWidget widget, String elementName, Map<String, String> attributes, StaticXmlWidget container, StaticHtmlMetawidget metawidget)
    {
        if (widget instanceof HtmlTableHeader)
        {
            container.getChildren().add(widget);
            return;
        }

        // Do not layout StaticXmlStubs

        if (widget instanceof StaticXmlStub)
        {
            return;
        }

        HtmlAnchor link = new HtmlAnchor();
        CoreUrl curl = new CoreUrl();

        String entityPlural = Noun.pluralOf(StaticJspUtils.unwrapExpression(metawidget.getAttribute("value").toLowerCase()));
        curl.setValue("/" + entityPlural + "/${" + StaticJspUtils.unwrapExpression(metawidget.getAttribute("value")) + ".id}");
        link.putAttribute("href", curl.toString());
        link.getChildren().add(widget);

        HtmlTableCell cell = new HtmlTableCell();
        cell.getChildren().add(link);
        container.getChildren().add(cell);
    }
}