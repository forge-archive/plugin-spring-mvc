package org.jboss.forge.spec.spring.security.impl;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.*;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.spec.javaee.ServletFacet;
import org.jboss.forge.spec.spring.security.SpringSecurityFacet;

/**
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

@Alias("forge.spec.springsecurity")
@RequiresFacet({ DependencyFacet.class, ServletFacet.class })
public class SpringSecurityFacetImpl extends BaseFacet implements SpringSecurityFacet
{
    public static final Dependency JAVAEE6 = DependencyBuilder.create("org.jboss.spec:jboss-javaee-6.0").setScopeType(ScopeType.IMPORT)
            .setPackagingType(PackagingType.BASIC);

    private final DependencyInstaller installer;

    private static final String SPRING_SECURITY_VERSION = "3.1.4.RELEASE";

    private static final Dependency SPRING_SECURITY = DependencyBuilder.create("org.springframework.security:spring-security-core:${spring.security.version}");

    private static final Dependency SPRING_SECURITY_CONFIG = DependencyBuilder.create("org.springframework.security:spring-security-config:${spring.security.version}");

    private static final Dependency SPRING_SECURITY_WEB = DependencyBuilder.create("org.springframework.security:spring-security-web:${spring.security.version}");

    private String TARGET_DIR;

    @Inject
    public SpringSecurityFacetImpl(final DependencyInstaller installer)
    {
        this.installer = installer;
    }

    @Override
    public boolean isInstalled()
    {
        String version = project.getFacet(ServletFacet.class).getConfig().getVersion();

        if (!version.trim().startsWith("3"))
        {
            return false;
        }

        DependencyFacet deps = project.getFacet(DependencyFacet.class);

        for (Dependency requirement : getRequiredSecurityDependencies())
        {
            if(!deps.hasEffectiveDependency(requirement))
            {
                return false;
            }
        }

        return true;
    }

    private List<Dependency> getRequiredSecurityDependencies() {
    	  return Arrays.asList(SPRING_SECURITY, SPRING_SECURITY_CONFIG, SPRING_SECURITY_WEB);      
	}


    /*
     * Facet Methods
     */
    @Override
    public FileResource<?> getSecurityContextFile(String targetDir)
    {
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);

        if (targetDir.equals("/") || targetDir.isEmpty())
        {
            MetadataFacet meta = project.getFacet(MetadataFacet.class);

            return web.getWebResource("WEB-INF/" + meta.getProjectName().replace(' ', '-').toLowerCase() + "-security-context.xml");
        }
        else
        {
            while (targetDir.startsWith("/"))
            {
                targetDir = targetDir.substring(1);
            }

            while (targetDir.endsWith("/"))
            {
                targetDir = targetDir.substring(0, targetDir.length()-1);
            }

            String filename = "WEB-INF/" + targetDir.replace('/', '-').toLowerCase() + "-security-context.xml";

            return web.getWebResource(filename);
        }
    }

    @Override
    public String getTargetDir() {
        return this.TARGET_DIR;
    }

    @Override
    public void setTargetDir(String targetDir) {
        this.TARGET_DIR = targetDir;
    }

    @Override
	public boolean install() {
		for (Dependency requirement : getRequiredSecurityDependencies())
        {
            if (!this.installer.isInstalled(project, requirement))
            {
                DependencyFacet deps = project.getFacet(DependencyFacet.class);

                if (!deps.hasDirectManagedDependency(JAVAEE6))
                {
                    this.installer.installManaged(project, JAVAEE6);
                }

                if (requirement.getGroupId().equals("org.springframework.security"))
                {
                    deps.setProperty("spring.security.version", SPRING_SECURITY_VERSION);
                }

                if (!requirement.equals(JAVAEE6))
                {
                    deps.addDirectDependency(requirement);
                }
            }
        }

        return true;
	}
}