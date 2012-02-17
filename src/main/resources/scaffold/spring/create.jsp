<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<?xml version="1.0" encoding="UTF-8"?>

<html>

	<head>
		<title>Create a new @{entityName}</title>
	</head>
	
	<form:form commandName="@{ccEntity}">
	
		@{metawidget}

		<input type="submit" value="Create New @{entity.getName()}"/>		
	</form:form>
	
</html>