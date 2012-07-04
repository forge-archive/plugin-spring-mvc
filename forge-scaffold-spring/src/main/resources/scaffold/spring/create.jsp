<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}">

		@{metawidget}

		<div class="buttons">
			<table>
				<tbody>
					<tr>
						<td>
							<input type="submit" value="Save" class="btn btn-primary"/>
						</td>
						<td>
							<a class="btn btn-primary" href="<c:url value="@{targetDir}@{entityPlural.toLowerCase()}"/>">Cancel</a>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</form:form>
</div>
