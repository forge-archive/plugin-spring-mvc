The 'plugin-springmvc' repository contains the source code for a Forge plugin.  This plugin can be used to create a Spring MVC web application.  As well, it contains a Spring Security plugin which can be used to implement Spring Security in the web application.

-- BUG WORKAROUND --

Currently, there is a bug with the Spring MVC plugin due to classloading issues between Forge and the plugin.  While this is being fixed, there is a workaround to build and run the plugin:

1. Run Forge
2. Execute 'forge source-plugin {path_to_plugin}'.  This will succeed, but then throw an exception, do not be alarmed.
3. Exit Forge (it is important that you exit it, even if you open a new terminal tab for the next part, as Forge must be fully restarted later).
4. Navigate to ~/.forge/plugins/org/jboss/forge/plugins/spring/plugin-spring-mvc/{build name}.
5. Add the following module to module.xml: '<module name="org.jboss.forge"/>'.
6. Restart Forge!
