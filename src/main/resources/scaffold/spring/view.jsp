<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<?xml version="1.0" encoding="UTF-8"?>

<html>

	<head>
		<title>View @{entityName}</title>
	</head>

	<form:form commandName="@{ccEntity}">

		@{metawidget}

	</form:form>

	<form:form commandName="@{ccEntity}" method="POST">
		<input type="hidden" name="method" value="_delete"/>
		<input type="submit" value="Delete"/>
	</form:form>

</html>
