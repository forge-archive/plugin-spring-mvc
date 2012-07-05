<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<html>

	<head>
		<title><tiles:insertAttribute name="title"/></title>

		<link rel="icon" href="<c:url value="/static/favicon.ico"/>"/>
		<link rel="stylesheet" type="text/css" href="<c:url value="/static/resources/bootstrap.css"/>"/>
		<link rel="stylesheet" type="text/css" href="<c:url value="/static/resources/forge-style.css"/>"/>
	</head>

	<body>
	
		<div class="navbar navbar-fixed-top">
			<div class="navbar-inner">
				<div class="container">
					<a href="http://design.jboss.org/jbossforge/ticketmonster/html/index.html" class="brand">@{appName}</a>
					<div class="nav-collapse collapse">
						<ul class="nav">
							<li><a href="https://docs.jboss.org/author/display/FORGE/UI+Scaffolding">How to Customize</a></li>
						</ul>
					</div>
				</div>
			</div>
		</div>

		<div class="container forgecontainer">
			<div id="navigation">
				<a id="homeLink" href="<c:url value="/"/>">
					<img src="<c:url value="/static/resources/forge-logo.png"/>" alt="Forge... get hammered" border="0"/>
				</a>
				@{navigation}
			</div>

			<div id="content">
				<h1><tiles:insertAttribute name="header"/></h1>
				<h2><tiles:insertAttribute name="subheader"/></h2>

				<tiles:insertAttribute name="body"/>
			</div>
		</div>

		<footer>
			<div id="footer-wrapper">
				Powered by <a href="http://jboss.org/forge">Forge</a></p>				
			</div>
		</footer>
	</body>
</html>


