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
import java.util.Map;

import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.jsp.StaticJspMetawidget;
import org.metawidget.statically.jsp.widgetbuilder.JspWidgetBuilder;
import org.metawidget.statically.layout.SimpleLayout;
import org.metawidget.statically.spring.StaticSpringMetawidget;
import org.metawidget.statically.spring.widgetbuilder.SpringWidgetBuilder;
import org.metawidget.util.ClassUtils;
import org.metawidget.util.WidgetBuilderUtils;
import org.metawidget.util.simple.StringUtils;
import org.metawidget.widgetbuilder.composite.CompositeWidgetBuilder;
import org.metawidget.widgetbuilder.composite.CompositeWidgetBuilderConfig;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class SpringEntityWidgetBuilder
        extends SpringWidgetBuilder
{
    //
    // Public methods
    //

    @Override
    public StaticXmlWidget buildWidget(String elementName, Map<String, String> attributes, StaticSpringMetawidget metawidget) {

        String type = WidgetBuilderUtils.getActualClassOrType(attributes);
        Class<?> clazz = ClassUtils.niceForName(type);

        if (Collection.class.isAssignableFrom(clazz))
        {
            StaticJspMetawidget nestedMetawidget = new StaticJspMetawidget();
            nestedMetawidget.setInspector( metawidget.getInspector() );
            nestedMetawidget.setLayout( new SimpleLayout() );
            nestedMetawidget.setWidgetBuilder( new CompositeWidgetBuilder( new CompositeWidgetBuilderConfig()
                    .setWidgetBuilders( new EntityWidgetBuilder(), new JspWidgetBuilder() )));
            nestedMetawidget.setPath( metawidget.getPath() + StringUtils.SEPARATOR_FORWARD_SLASH_CHAR + attributes.get( NAME ));
            return nestedMetawidget;
        }

        return null;
    }
}
