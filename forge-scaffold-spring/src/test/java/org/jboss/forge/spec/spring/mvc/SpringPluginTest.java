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

import junit.framework.Assert;

import org.jboss.forge.project.Project;
import org.jboss.forge.test.AbstractShellTest;
import org.junit.Test;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

public class SpringPluginTest extends AbstractShellTest
{
    @Test
    public void testSpringSetup() throws Exception
    {
        getShell().setOutputStream(System.out);
        Project project = initializeJavaProject();
        queueInputLines("Y", "");
        getShell().execute("spring setup");
        Assert.assertTrue(project.hasFacet(SpringFacet.class));
    }

    @Test
    public void testSpringConfig() throws Exception
    {
        Project project = initializeJavaProject();
        queueInputLines("", "");
        getShell().execute("spring setup");
        Assert.assertTrue(project.hasFacet(SpringFacet.class));

        queueInputLines("", "", "");
        getShell().execute("spring mvc-from-template --mvcContext mvc-context.xml");
        Assert.assertTrue(project.getProjectRoot().getChild("src/main/webapp/WEB-INF/web.xml").exists());
        Assert.assertTrue(project.getProjectRoot().getChild("src/main/resources/META-INF/spring/applicationContext.xml").exists());
        Assert.assertTrue(project.getProjectRoot().getChild("src/main/webapp/WEB-INF/mvc-context.xml").exists());
    }
}
