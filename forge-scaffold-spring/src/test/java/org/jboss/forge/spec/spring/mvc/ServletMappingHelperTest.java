/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and individual contributors
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

package org.jboss.forge.spec.spring.mvc;

import java.io.InputStream;

import junit.framework.Assert;

import org.jboss.forge.shell.util.Streams;
import org.jboss.forge.spec.spring.mvc.impl.SpringFacetImpl.ServletMappingHelper;
import org.junit.Test;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class ServletMappingHelperTest
{
    @Test
    public void addSpringServletMappingTest() throws Exception
    {
        ServletMappingHelper helper = new ServletMappingHelper();
        InputStream webXML = Streams.fromString(getWebXmlShort());
        InputStream webXML2 = helper.addSpringServletMapping(webXML, "*.jsp");
        String webXML2String = Streams.toString(webXML2);
        Assert.assertNotSame(webXML, webXML2);
        Assert.assertTrue(webXML2String.contains("<url-pattern>*.jsp</url-pattern>"));
        Assert.assertTrue(webXML2String.contains("<servlet-name>Spring Servlet</servlet-name>"));

        webXML2.reset();
        webXML2 = helper.addSpringServletMapping(webXML2, "/spring/");
        webXML2String = Streams.toString(webXML2);
        Assert.assertNotSame(webXML, webXML2);
        Assert.assertTrue(webXML2String.contains("<url-pattern>/spring/</url-pattern>"));
        Assert.assertTrue(webXML2String.contains("<servlet-name>Spring Servlet</servlet-name>"));
        Assert.assertTrue(webXML2String.contains("<servlet-mapping>"));

        webXML2.reset();
        InputStream webXML3 = helper.addSpringServletMapping(webXML2, "/spring/");
        String webXML3String = Streams.toString(webXML3);
        Assert.assertEquals(webXML2String, webXML3String);
    }

    private String getWebXmlShort()
    {
       return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                +
                "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"3.0\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\">\n"
                +
                "  <display-name>test2</display-name>\n" +
                "  <session-config>\n" +
                "    <session-timeout>30</session-timeout>\n" +
                "  </session-config>\n" +
                "</web-app>";
    }
}
