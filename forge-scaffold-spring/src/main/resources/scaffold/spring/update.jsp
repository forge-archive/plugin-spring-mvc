<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}" action="$${@{ccEntity}.id}">

		@{metawidget}

		<input type="submit" value="Save" class="btn btn-primary"/>

	</form:form>

	<div class="buttons">
		<table>
			<tbody>
				<tr>
					<td>
						<a class="btn btn-primary" href="<c:url value="@{targetDir}@{entityPlural.toLowerCase()}"/>">Cancel</a>
					</td>
					<td>
						<form:form commandName="@{ccEntity}" action="$${@{ccEntity}.id}/delete" method="POST" class="align">
							<input type="submit" value="Delete" class="btn btn-primary"/>
						</form:form>
					</td>
				</tr>
			</tbody>
		</table>
	</div>
</div>
