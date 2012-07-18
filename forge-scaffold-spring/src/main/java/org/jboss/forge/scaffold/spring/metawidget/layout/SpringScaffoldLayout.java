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

import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.html.StaticHtmlMetawidget;
import org.metawidget.statically.html.layout.HtmlTableLayoutConfig;
import org.metawidget.statically.spring.layout.SpringTableLayout;

/**
 * Custom Metawidget layout that suppresses the <form:hidden> tags generated for the entity's id and version fields.
 * These fields are suppressed because to the form outside of the table layout, for formatting purposes.
 * 
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class SpringScaffoldLayout extends SpringTableLayout
{
    //
    // Constructors
    //

    public SpringScaffoldLayout()
    {
        super();
    }

    public SpringScaffoldLayout(HtmlTableLayoutConfig config)
    {
        super(config);
    }

    //
    // Public methods
    //

    /**
     * Overridden to surprise layout of 'id' and 'version' fields. 
     */

    @Override
    public void layoutWidget(StaticXmlWidget widget, String elementName, Map<String, String> attributes, StaticXmlWidget container, StaticHtmlMetawidget metawidget)
    {
        if (widget.getAttribute("path") != null && (widget.getAttribute("path").equals("id") || widget.getAttribute("path").equals("version")))
        {
            return;
        }
        else
        {
            super.layoutWidget(widget, elementName, attributes, container, metawidget);
        }
    }
}
