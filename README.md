The 'plugin-spring-mvc' repository contains the source code for a Spring ScaffoldProvider.  This ScaffoldProvider implementation, SpringScaffold,
can be used in conjunction with the ScaffoldPlugin to generate a web application that uses Spring MVC to handle simple CRUD cases for generated
entities.

To properly set up a project for Spring scaffolding, execute the following commands:

persistence setup --provider {PROVIDER} --container {CONTAINER}

scaffold setup --scaffoldType spring --targetDir {TARGET_DIR}

Then, execute scaffold from-entity on the entity.

This repository also contains the Spring plugin, with many commands used to enhance an existing project to configure Spring MVC, e.g.
generating application context files, updating web.xml, etc.  The commands of this plugin, once installed, are available under the alias 'spring'.
The plugin code itself is contained within the SpringPlugin class.

Finally, the repository contains a SpringFacet which, when installed, indicates that the project has a designated location for an application 
context file (src/main/resources/META-INF/spring/applicationContext.xml by default) and has all of the necessary Spring dependencies.
