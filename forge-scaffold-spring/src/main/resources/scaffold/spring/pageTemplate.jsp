<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<html>

	<head>
		<title><tiles:insertAttribute name="title"/></title>

		<link rel="icon" href="<c:url value="/static/favicon.ico"/>"/>
		<link rel="stylesheet" type="text/css" href="<c:url value="/static/resources/forge-style.css"/>"/>
	</head>

	<body>

		<div id="wrapper">

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

				<div id="footer">
					Powered by <a href="http://jboss.org/forge">Forge</a>.  Icons by the <a href="http://everaldo.com/crystal">Crystal Project</a>
				</div>
			</div>
		</div>
	</body>
</html>


