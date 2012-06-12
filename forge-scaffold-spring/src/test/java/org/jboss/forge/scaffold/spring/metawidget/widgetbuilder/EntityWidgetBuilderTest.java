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
import org.metawidget.inspector.annotation.MetawidgetAnnotationInspector;
import org.metawidget.inspector.annotation.UiComesAfter;
import org.metawidget.inspector.composite.CompositeInspector;
import org.metawidget.inspector.composite.CompositeInspectorConfig;
import org.metawidget.inspector.iface.Inspector;
import org.metawidget.inspector.impl.BaseObjectInspector;
import org.metawidget.inspector.impl.BaseObjectInspectorConfig;
import org.metawidget.inspector.impl.propertystyle.Property;
import org.metawidget.inspector.impl.propertystyle.statically.StaticPropertyStyle;
import org.metawidget.inspector.propertytype.PropertyTypeInspector;
import org.metawidget.statically.StaticWidget;
import org.metawidget.statically.jsp.StaticJspMetawidget;
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
        StaticJspMetawidget metawidget = new StaticJspMetawidget();
        metawidget.setValue("#{foo}");
        JspEntityWidgetBuilder widgetBuilder = new JspEntityWidgetBuilder();
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
        StaticJspMetawidget metawidget = new StaticJspMetawidget();
        metawidget.setValue("#{foo}");
        metawidget.setPath(FooOneToOne.class.getName());
        JspEntityWidgetBuilder widgetBuilder = new JspEntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bar");
        attributes.put(TYPE, Bar.class.getName());
        attributes.put(INVERSE_RELATIONSHIP, TRUE);
        attributes.put(READ_ONLY, TRUE);
        attributes.put(ONE_TO_ONE, TRUE);

        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        String result = "<table id=\"bar\"><tbody><tr><th><label>Name:</label>";
        result += "</th><td><c:out value=\"${foo.bar.name}\"/></td><td/></tr><tr><th><label>Description:";
        result += "</label></th><td><c:out value=\"${foo.bar.description}\"/></td><td/></tr></tbody></table>";

        Assert.assertEquals(result, widget.toString());
    }

    public void testOneToOne()
            throws Exception
    {
        StaticJspMetawidget metawidget = new StaticJspMetawidget();
        metawidget.setValue("#{foo}");
        metawidget.setPath(FooOneToOne.class.getName());
        JspEntityWidgetBuilder widgetBuilder = new JspEntityWidgetBuilder();
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
        StaticJspMetawidget metawidget = new StaticJspMetawidget();
        metawidget.setValue("#{foo}");
        metawidget.setPath(FooOneToOne.class.getName());
        JspEntityWidgetBuilder widgetBuilder = new JspEntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bar");
        attributes.put(TYPE, Bar.class.getName());
        attributes.put(ONE_TO_ONE, TRUE);

        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        // This WidgetBuilder functionality needs to be further debugged.

        String result = "<tr><td><a href=\"<c:out value=\"/EntityWidgetBuilderTest$Bars/create\"/>\" ";
        result += "rendered=\"${emptyfoo.bar}\">Create New Bar</a></td><td><table id=\"bar\"><tbody><tr><th><label for=\"bar-name\">";
        result += "Name:</label></th><td><input id=\"bar-name\" name=\"barName\" type=\"text\" value=\"${foo.bar.name}\"/></td><td/></tr><tr>";
        result += "<th><label for=\"bar-description\">Description:</label></th><td><input id=\"bar-description\" name=\"barDescription\"";
        result += " type=\"text\" value=\"${foo.bar.description}\"/></td><td/></tr></tbody></table></td></tr>";

        Assert.assertEquals(result, widget.toString());
    }

    public void testTopLevelList()
            throws Exception
    {
        StaticJspMetawidget metawidget = new StaticJspMetawidget();
        metawidget.setValue("#{foo}");
        JspEntityWidgetBuilder widgetBuilder = new JspEntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bars");
        attributes.put(TYPE, List.class.getName());
        attributes.put(PARAMETERIZED_TYPE, Bar.class.getName());
        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        String result = "<table><thead><tr><th>Name</th><th>Description</th></tr></thead><tbody><c:forEach items=\"${bars}\" var=\"item\">";
        result += "<tr><td><a href=\"/scaffold/entityWidgetBuilderTest$Bar/view/${item.id}\"><c:out value=\"${item.name}\"/></a>";
        result += "</td><td><a href=\"/scaffold/entityWidgetBuilderTest$Bar/view/${item.id}\"><c:out value=\"${item.description}\"/></a>";
        result += "</td></tr></c:forEach></tbody></table>";

        Assert.assertEquals(result, widget.toString());
    }

    public void testEmbeddedSet()
            throws Exception
    {
        StaticJspMetawidget metawidget = new StaticJspMetawidget();
        metawidget.setValue("#{foo}");        JspEntityWidgetBuilder widgetBuilder = new JspEntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bars");
        attributes.put(TYPE, Set.class.getName());
        attributes.put(PARAMETERIZED_TYPE, Bar.class.getName());
        attributes.put(MANY_TO_MANY, TRUE);
        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        String result = "<table><thead><tr><th>Name</th><th>Description</th></tr></thead><tbody><c:forEach items=\"${bars}\" var=\"item\">";
        result += "<tr><td><a href=\"/scaffold/entityWidgetBuilderTest$Bar/view/${item.id}\"><c:out value=\"${item.name}\"/></a></td>";
        result += "<td><a href=\"/scaffold/entityWidgetBuilderTest$Bar/view/${item.id}\"><c:out value=\"${item.description}\"/></a></td>";
        result += "<td><a href=\"<c:out value=\"/entityWidgetBuilderTest$Bars/${entityWidgetBuilderTest$Bar.id}/remove\"/>\">Remove</a></td>";
        result += "</tr></c:forEach><tr><td><form:select multiple=\"multiple\"><form:option/><form:options items=\"entityWidgetBuilderTest$Bar\"/>";
        result += "</form:select></td></tr></tbody></table>";

        Assert.assertEquals(result, widget.toString());

        // With suppressed column

        attributes.put(INVERSE_RELATIONSHIP, "name");
        widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        result = "<table><thead><tr><th>Name</th><th>Description</th></tr></thead><tbody><c:forEach items=\"${bars}\" var=\"item\">";
        result += "<tr><td><a href=\"/scaffold/entityWidgetBuilderTest$Bar/view/${item.id}\"><c:out value=\"${item.description}\"/></a></td>";
        result += "<td><a href=\"<c:out value=\"/entityWidgetBuilderTest$Bars/${entityWidgetBuilderTest$Bar.id}/remove\"/>\">Remove</a></td>";
        result += "</tr></c:forEach></tbody></table>";

        Assert.assertEquals(result, widget.toString());
    }

    public void testSuppressOneToMany()
            throws Exception
    {
        StaticJspMetawidget metawidget = new StaticJspMetawidget();
        metawidget.setValue("#{foo}");
        JspEntityWidgetBuilder widgetBuilder = new JspEntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bars");
        attributes.put(TYPE, Set.class.getName());
        attributes.put(PARAMETERIZED_TYPE, FooOneToMany.class.getName());
        attributes.put(MANY_TO_MANY, TRUE);
        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        String result = "<table><thead><tr><th>Field 1</th><th>Field 2</th><th>Field 3</th></tr></thead><tbody>";
        result += "<c:forEach items=\"${bars}\" var=\"item\"><tr><td><a href=\"/scaffold/entityWidgetBuilderTest$FooOneToMany/view/${item.id}\">";
        result += "<c:out value=\"${item.field1}\"/></a></td><td><a href=\"/scaffold/entityWidgetBuilderTest$FooOneToMany/view/${item.id}\">";
        result += "<c:out value=\"${item.field3}\"/></a></td><td>";
        result += "<a href=\"<c:out value=\"/entityWidgetBuilderTest$FooOneToManies/${entityWidgetBuilderTest$FooOneToMany.id}/remove\"/>\">";
        result += "Remove</a></td></tr></c:forEach><tr><td><form:select multiple=\"multiple\"><form:option/><form:options items=\"entityWidgetBuilderTest$FooOneToMany\"/>";
        result += "</form:select></td></tr></tbody></table>";

        Assert.assertEquals(result, widget.toString());
    }

    public void testExpandOneToOne()
            throws Exception
    {
        StaticJspMetawidget metawidget = new StaticJspMetawidget();
        Inspector testInspector = new BaseObjectInspector()
        {
            @Override
            protected Map<String, String> inspectProperty(Property property)
            {
                Map<String, String> attributes = CollectionUtils.newHashMap();

                // OneToOne

                if (property.isAnnotationPresent(OneToOne.class))
                {
                    attributes.put(ONE_TO_ONE, TRUE);
                }

                return attributes;
            }
        };
        Inspector inspector = new CompositeInspector(new CompositeInspectorConfig()
                    .setInspectors(
                            new PropertyTypeInspector(new BaseObjectInspectorConfig()
                                    .setPropertyStyle(new StaticPropertyStyle())),
                            new MetawidgetAnnotationInspector(new BaseObjectInspectorConfig()
                                    .setPropertyStyle(new StaticPropertyStyle())),
                            testInspector));

        metawidget.setInspector(inspector);
        metawidget.setValue("#{foo}");
        JspEntityWidgetBuilder widgetBuilder = new JspEntityWidgetBuilder();
        Map<String, String> attributes = CollectionUtils.newHashMap();
        attributes.put(NAME, "bars");
        attributes.put(TYPE, Set.class.getName());
        attributes.put(PARAMETERIZED_TYPE, FooOneToOne.class.getName());
        attributes.put(MANY_TO_MANY, TRUE);
        StaticWidget widget = widgetBuilder.buildWidget(PROPERTY, attributes, metawidget);

        String result = "<table><thead><tr><th>Bar</th></tr></thead><tbody><c:forEach items=\"${bars}\" var=\"item\"><tr><td>";
        result += "<a href=\"/scaffold/entityWidgetBuilderTest$FooOneToOne/view/${item.id}\"><c:out value=\"${item.bar.name}\"/>";
        result += "<c:out value=\"${item.bar.description}\"/></a></td>";
        result += "<td><a href=\"<c:out value=\"/entityWidgetBuilderTest$FooOneToOnes/${entityWidgetBuilderTest$FooOneToOne.id}/remove\"/>\">";
        result += "Remove</a></td></tr></c:forEach><tr><td><form:select multiple=\"multiple\"><form:option/><form:options items=\"entityWidgetBuilderTest$FooOneToOne\"/>";
        result += "</form:select></td></tr></tbody></table>";

        Assert.assertEquals(result, widget.toString());
    }

    public void testReadOnlyBoolean()
            throws Exception
    {

        // Normal boolean

        StaticJspMetawidget metawidget = new StaticJspMetawidget();
        metawidget.setValue("#{foo}");
        JspEntityWidgetBuilder widgetBuilder = new JspEntityWidgetBuilder();
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
