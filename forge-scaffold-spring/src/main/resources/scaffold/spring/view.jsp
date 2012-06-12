<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}">

		@{metawidget}

	</form:form>

	<div class="buttons">
		<table>
			<tr>
				<td>
					<a class="button" href="<c:url value="@{targetDir}@{entityPlural.toLowerCase()}"/>">View All</a>
				</td>
				<td>
					<a class="button" href="<c:url value="@{targetDir}@{entityPlural.toLowerCase()}/$${@{ccEntity}.id}?edit=true"/>">Edit</a>
				</td>
				<td>
					<a class="button" href="<c:url value="@{targetDir}@{entityPlural.toLowerCase()}/create"/>">Create New</a>
				</td>
			</tr>
		</table>
	</div>
</div>
