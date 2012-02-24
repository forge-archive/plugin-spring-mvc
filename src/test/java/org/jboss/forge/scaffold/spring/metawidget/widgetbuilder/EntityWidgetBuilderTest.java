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
import static org.metawidget.inspector.jsp.JspInspectionResultConstants.*;
import static org.jboss.forge.scaffold.spring.metawidget.inspector.ForgeInspectionResultConstants.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.OneToOne;

import org.junit.Assert;
import org.metawidget.inspector.annotation.UiComesAfter;
import org.metawidget.statically.StaticWidget;
import org.metawidget.statically.spring.StaticSpringMetawidget;
import org.metawidget.util.CollectionUtils;

import junit.framework.TestCase;

public class EntityWidgetBuilderTest
        extends TestCase
{
    //
    // Public methods
    //
    
    public void testManyToOne()
            throws Exception
    {
        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("#{foo}");
        EntityWidgetBuilder widgetBuilder = new EntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bar");
        attributes.put(TYPE, Bar.class.getName());
        attributes.put(READ_ONLY, TRUE);
        attributes.put(JSP_LOOKUP, "#{bar.all}");
        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        String result = "<a href=\"<c:out value=\"/entityWidgetBuilderTest$Bars/${foo.bar.id}\"/>\">${foo.bar}</a>";

        Assert.assertEquals(result, widget.toString());
    }

    public void testReadOnlyOneToOne()
            throws Exception
    {
        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("#{foo}");
        metawidget.setPath(FooOneToOne.class.getName());
        EntityWidgetBuilder widgetBuilder = new EntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bar");
        attributes.put(TYPE, Bar.class.getName());
        attributes.put(INVERSE_RELATIONSHIP, TRUE);
        attributes.put(READ_ONLY, TRUE);
        attributes.put(ONE_TO_ONE, TRUE);

        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        String result = "<table id=\"bar\"><tbody><tr><th><form:label path=\"bar.name\">Name:</form:label>";
        result += "</th><td><c:out value=\"${foo.bar.name}\"/></td><td/></tr><tr><th><form:label path=\"bar.description\">Description:";
        result += "</form:label></th><td><c:out value=\"${foo.bar.description}\"/></td><td/></tr></tbody></table>";

        Assert.assertEquals(result, widget.toString());
    }

    public void testOneToOne()
            throws Exception
    {
        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("#{foo}");
        metawidget.setPath(FooOneToOne.class.getName());
        EntityWidgetBuilder widgetBuilder = new EntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bar");
        attributes.put(TYPE, Bar.class.getName());
        attributes.put(ONE_TO_ONE, TRUE);
        attributes.put(REQUIRED, TRUE);

        // ONE_TO_ONE and REQUIRED should return null

        Assert.assertNull(widgetBuilder.buildWidget(PROPERTY, attributes, metawidget));
    }

    public void testOptionalOneToOne()
            throws Exception
    {
        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("#{foo}");
        metawidget.setPath(FooOneToOne.class.getName());
        EntityWidgetBuilder widgetBuilder = new EntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bar");
        attributes.put(TYPE, Bar.class.getName());
        attributes.put(ONE_TO_ONE, TRUE);

        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        // This WidgetBuilder functionality needs to be further debugged.

        String result = "<tr><td><a href=\"<c:out value=\"/EntityWidgetBuilderTest$Bars/create\"/>\" ";
        result += "rendered=\"${emptyfoo.bar}\">Create New Bar</a></td><table id=\"bar\"><tbody><tr><th><form:label path=\"bar.name\">";
        result += "Name:</form:label></th><td><form:input path=\"bar.name\"/></td><td/></tr><tr><th><form:label path=\"bar.description\">";
        result += "Description:</form:label></th><td><form:input path=\"bar.description\"/></td><td/></tr></tbody></table></tr>";

        Assert.assertEquals(result, widget.toString());
    }

    public void testTopLevelList()
            throws Exception
    {
        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("#{foo}");
        EntityWidgetBuilder widgetBuilder = new EntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bars");
        attributes.put(TYPE, List.class.getName());
        attributes.put(PARAMETERIZED_TYPE, Bar.class.getName());
        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        String result = "<table><thead><tr/></thead><tbody><c:forEach items=\"${bars}\" var=\"item\"/></tbody></table>";

        Assert.assertEquals(result, widget.toString());
    }

    public void testReadOnlyBoolean()
            throws Exception
    {

        // Normal boolean

        StaticSpringMetawidget metawidget = new StaticSpringMetawidget();
        metawidget.setValue("#{foo}");
        EntityWidgetBuilder widgetBuilder = new EntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bar");
        attributes.put(TYPE, boolean.class.getName());

        Assert.assertNull(widgetBuilder.buildWidget(PROPERTY, attributes, metawidget));

        // Read-only boolean

        attributes.put(READ_ONLY, TRUE);
        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        String result = "<c:out value=\"${foo.bar}\"/>";
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
}
