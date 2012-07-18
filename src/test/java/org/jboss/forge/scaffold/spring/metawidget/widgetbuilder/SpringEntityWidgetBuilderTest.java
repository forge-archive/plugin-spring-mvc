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
import java.util.Set;

import javax.persistence.OneToOne;

import org.junit.Test;
import org.metawidget.inspector.annotation.UiComesAfter;
import org.metawidget.inspector.annotation.UiRequired;
import org.metawidget.statically.StaticWidget;
import org.metawidget.statically.StaticXmlWidget;
import org.metawidget.statically.spring.StaticSpringMetawidget;
import org.metawidget.util.CollectionUtils;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class SpringEntityWidgetBuilderTest
    extends TestCase
{

    //
    // Public methods
    //

    EntityWidgetBuilderConfig config = new EntityWidgetBuilderConfig().setConfig(new MockForgeConfiguration());

    @Test
    public void testOneToOne()
            throws Exception
    {
        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("foo");
        metawidget.setPath(FooOneToOne.class.getName());
        SpringEntityWidgetBuilder widgetBuilder = new SpringEntityWidgetBuilder(this.config);
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bar");
        attributes.put(TYPE, Bar.class.getName());
        attributes.put(ONE_TO_ONE, TRUE);
        attributes.put(REQUIRED, TRUE);

        StaticXmlWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);
        Assert.assertEquals("<form:select><form:options itemValue=\"id\" items=\"${bar}\"/></form:select>",widget.toString());
    }

    @Test
    public void testOptionalOneToOne()
            throws Exception
    {
        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("foo");
        metawidget.setPath(FooOneToOne.class.getName());
        SpringEntityWidgetBuilder widgetBuilder = new SpringEntityWidgetBuilder(this.config);
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bar");
        attributes.put(TYPE, Bar.class.getName());
        attributes.put(ONE_TO_ONE, TRUE);

        StaticXmlWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        String result = "<form:select><form:option value=\"\"/><form:options itemValue=\"id\" items=\"${bar}\"/></form:select>";

        Assert.assertEquals(result, widget.toString());
    }

    public void testEmbeddedSet()
            throws Exception
    {
        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("foo");
        SpringEntityWidgetBuilder widgetBuilder = new SpringEntityWidgetBuilder(config);
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bars");
        attributes.put(TYPE, Set.class.getName());
        attributes.put(PARAMETERIZED_TYPE, Bar.class.getName());
        attributes.put(N_TO_MANY, TRUE);
        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        String result = "<form:select itemValue=\"id\" items=\"${springentitywidgetbuildertest$bars}\" multiple=\"multiple\"/>";
        Assert.assertEquals(result, widget.toString());
    }

    //
    // Inner class
    //

    static class Bar
    {
       public String getName()
       {
          return null;
       }

       public void setName(@SuppressWarnings("unused") String name)
       {
          // Do nothing
       }

       @UiComesAfter("name")
       public String getDescription()
       {
          return null;
       }

       public void setDescription(@SuppressWarnings("unused") String description)
       {
          // Do nothing
       }
    }

    static class FooOneToMany
    {
       public String getField1()
       {
          return null;
       }

       // Not @OneToMany: sometimes annotations are forgotten
       public Set<String> getField2()
       {
          return null;
       }

       public String getField3()
       {
          return null;
       }
    }

    static class FooOneToOne
    {
       @OneToOne
       public Bar getBar()
       {
          return null;
       }
    }

    static class RequiredBar
    {
       @UiRequired
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
