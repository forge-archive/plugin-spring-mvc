<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<form:form commandName="@{ccEntity}">

	@{metawidget}

</form:form>

<span class="buttons">
	<a class="btn btn-primary" href="<c:url value="@{targetDir}@{entityPlural.toLowerCase()}"/>">View All</a>
	<a class="btn btn-primary" href="<c:url value="@{targetDir}@{entityPlural.toLowerCase()}/$${@{ccEntity}.id}?edit=true"/>">Edit</a>
	<a class="btn btn-primary" href="<c:url value="@{targetDir}@{entityPlural.toLowerCase()}/create"/>">Create New</a>
</span>