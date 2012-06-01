The 'plugin-spring-mvc' repository contains the source code for a Forge plugin.  This plugin can be used to create a Spring MVC web application.  As well, it contains a Spring Security plugin which can be used to implement Spring Security in the web application.

To properly set up a project for Spring scaffolding, execute the following commands:

persistence setup --provider {PROVIDER} --container {CONTAINER}

spring setup

spring persistence

scaffold setup --scaffoldType spring --targetDir {TARGET_DIR}

Then, execute scaffold from-entity on the entity.
