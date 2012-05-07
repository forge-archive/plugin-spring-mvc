<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}">

		@{metawidget}

	</form:form>

	<table>
		<tr>
			<td>
				<form:form commandName="@{ccEntity}" action="/@{entityPlural.toLowerCase()}" method="GET">
					<input type="submit" value="View All"/>
				</form:form>
			</td>
			<td>
				<form:form commandName="@{ccEntity}" action="$${@{ccEntity}.id}" method="GET">
					<input type="hidden" name="edit" value="true"/>
					<input type="submit" value="Edit"/>
				</form:form>
			</td>
			<td>
				<form:form commandName=@{ccEntity}" action="/create" method="GET">
					<input type="submit" value="Create New"/>
				</form:form>
			</td>
		</tr>
	</table>
</div>
