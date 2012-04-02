<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}">

		@{metawidget}

	</form:form>

	<table>
		<tr>
			<td>
				<input type="submit" value="Edit" onclick="window.location='<c:url value="@{targetDir}@{entityPlural.toLowerCase()}/$${@{ccEntity}.id}?edit=true"/>'"/>
			</td>
			<td>
				<form:form commandName="@{ccEntity}" action="$${@{ccEntity}.id}/delete" method="POST">
					<input type="submit" value="Delete"/>
				</form:form>
			</td>
		</tr>
	</table>
</div>
