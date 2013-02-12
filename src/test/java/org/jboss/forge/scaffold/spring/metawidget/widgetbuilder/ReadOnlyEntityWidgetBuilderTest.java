/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import static org.jboss.forge.scaffold.spring.metawidget.inspector.ForgeInspectionResultConstants.*;

import java.util.Map;

import org.junit.Test;
import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.spring.StaticSpringMetawidget;
import org.metawidget.util.CollectionUtils;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class ReadOnlyEntityWidgetBuilderTest
        extends TestCase
{
    @Test
    public void testNToMany()
    {
        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("foo");
        ReadOnlyEntityWidgetBuilder widgetBuilder = new ReadOnlyEntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(READ_ONLY, TRUE);
        attributes.put(NAME, "bar");
        attributes.put(N_TO_MANY, TRUE);

        StaticXmlWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);
        Assert.assertEquals("<c:out/>", widget.toString());
    }

    @Test
    public void testInverseRelationship()
    {
        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("foo");
        ReadOnlyEntityWidgetBuilder widgetBuilder = new ReadOnlyEntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(READ_ONLY, TRUE);
        attributes.put(NAME, "bar");
        attributes.put(INVERSE_RELATIONSHIP, TRUE);

        StaticXmlWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);
        Assert.assertEquals("<c:out/>", widget.toString());
    }

    @Test
    public void testBuildComplexWidget()
    {
        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("foo");
        ReadOnlyEntityWidgetBuilder widgetBuilder = new ReadOnlyEntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(READ_ONLY, TRUE);
        attributes.put(NAME, "bar");
        attributes.put(TYPE, Bar.class.getName());

        StaticXmlWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);
        Assert.assertEquals("<c:out/>", widget.toString());
    }

    public static class Bar
    {
        public String getName()
        {
            return null;
        }

        public void setName(@SuppressWarnings("unused") String name)
        {
            // Do nothing
        }
    }
}