// Metawidget
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package org.jboss.forge.scaffold.spring.metawidget.widgetbuilder;

import static org.metawidget.inspector.InspectionResultConstants.*;
import static org.metawidget.inspector.spring.SpringInspectionResultConstants.*;

import java.util.Map;

import org.metawidget.statically.javacode.JavaStatement;
import org.metawidget.statically.javacode.StaticJavaMetawidget;
import org.metawidget.statically.javacode.StaticJavaStub;
import org.metawidget.statically.javacode.StaticJavaWidget;
import org.metawidget.util.ClassUtils;
import org.metawidget.util.WidgetBuilderUtils;
import org.metawidget.util.simple.StringUtils;
import org.metawidget.widgetbuilder.iface.WidgetBuilder;

public class QueryByExampleWidgetBuilder
        implements WidgetBuilder<StaticJavaWidget, StaticJavaMetawidget>
{
    //
    // Public methods
    //

    @Override
    public StaticJavaWidget buildWidget(String elementName,
            Map<String, String> attributes, StaticJavaMetawidget metawidget)
    {
        // Drill down
        
        if(ENTITY.equals(elementName))
        {
            return null;
        }
        
        // Suppress
        
        if(TRUE.equals(attributes.get(HIDDEN)))
        {
            return new StaticJavaStub();
        }

        // Get actual class or type, defaulting to a String if none is found.

        Class<?> clazz = WidgetBuilderUtils.getActualClassOrType(attributes, String.class);

        String name = attributes.get(NAME);
        
        // String
        
        if (String.class.equals(clazz))
        {
           StaticJavaStub toReturn = new StaticJavaStub();
           toReturn.getChildren().add(
                    new JavaStatement("String " + name + " = search.get" + StringUtils.capitalize(name) + "()"));
           JavaStatement ifNotEmpty = new JavaStatement("if (" + name + " != null && !\"\".equals(" + name + "))");
           ifNotEmpty.getChildren().add(
                    new JavaStatement("predicatesList.add(builder.like(root.<String>get(\"" + name + "\"), '%' + " + name
                             + " + '%'))"));
           toReturn.getChildren().add(ifNotEmpty);
           return toReturn;
        }
        
        // int Primitive
        
        if(int.class.equals(clazz))
        {
            StaticJavaStub toReturn = new StaticJavaStub();
            toReturn.getChildren().add(
                     new JavaStatement("int " + name + " = search.get" + StringUtils.capitalize(name) + "()"));
            JavaStatement ifNotEmpty = new JavaStatement("if (" + name + " != 0)");
            ifNotEmpty.getChildren().add(
                     new JavaStatement("predicatesList.add(builder.equal(root.get(\"" + name + "\"), " + name + "))"));
            toReturn.getChildren().add(ifNotEmpty);
            return toReturn;
        }
        
        // LOOKUP

        String type = attributes.get(TYPE);

        if (attributes.containsKey(LOOKUP))
        {
           StaticJavaStub toReturn = new StaticJavaStub();
           JavaStatement getValue = new JavaStatement(ClassUtils.getSimpleName(type) + " " + name + " = search.get"
                    + StringUtils.capitalize(name) + "()");
           getValue.putImport(type);
           toReturn.getChildren().add(getValue);
           JavaStatement ifNotEmpty = new JavaStatement("if (" + name + " != null)");
           ifNotEmpty.getChildren().add(
                    new JavaStatement("predicatesList.add(builder.equal(root.get(\"" + name + "\"), " + name + "))"));
           toReturn.getChildren().add(ifNotEmpty);
           return toReturn;
        }

        // SPRING_LOOKUP

        if (attributes.containsKey(SPRING_LOOKUP))
        {
           StaticJavaStub toReturn = new StaticJavaStub();
           JavaStatement getValue = new JavaStatement(ClassUtils.getSimpleName(type) + " " + name + " = search.get"
                    + StringUtils.capitalize(name) + "()");
           getValue.putImport(type);
           toReturn.getChildren().add(getValue);
           JavaStatement ifNotEmpty = new JavaStatement("if (" + name + " != null && " + name + ".getId() != null)");
           ifNotEmpty.getChildren().add(
                    new JavaStatement("predicatesList.add(builder.equal(root.get(\"" + name + "\")," + name + "))"));
           toReturn.getChildren().add(ifNotEmpty);
           return toReturn;
        }        

        // Ignore N_TO_MANY relationships for now.

        return new StaticJavaStub();        
    }
}
