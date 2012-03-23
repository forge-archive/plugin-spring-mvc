<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<html>

	<head>
		<title>View All @{entityPluralName}</title>
	</head>

	<body>

		<form:form commandName="search" method="GET">
			@{metawidget}
			<input type="submit" value="Search"/>
		</form:form>
	
		<table>
			<c:forEach items="$${entities}" var="entity">
				<tr>
					<td>@{entityName} $${entity.id}</td>
					<td><input type="submit" value="View" onclick="window.location='<c:url value="/@{entityPlural.toLowerCase()}/$${entity.id}"/>'"/></td>
					<td><input type="submit" value="Edit" onclick="window.location='<c:url value="/@{entityPlural.toLowerCase()}/$${entity.id}?edit=true"/>'"/></td>
				</tr>
			</c:forEach>
		</table>

		<br/>

		<input type="submit" value="Create New @{entityName}" onclick="window.location='<c:url value="/@{entityPlural.toLowerCase()}/create"/>'"/>

	</body>

</html>
