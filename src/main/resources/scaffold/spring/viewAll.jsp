<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="search" method="GET">
		@{metawidget}
		<br/>

		<div class=" buttons">
			<table>
				<tbody>
					<tr>
						<td>
							<input type="submit" value="Search" class="button"/>
						</td>
						<td>
							<a class="button" href="<c:url value="@{targetDir}@{entityPlural.toLowerCase()}/create"/>">Create New</a>
						</td>
					</tr>					
				</tbody>
			</table>
		</div>
	</form:form>

	<table class="data-table">
		<thead>
			@{headerMetawidget}
		</thead>
		<tbody>
			<c:forEach items="$${@{entityPlural.toLowerCase()}}" var="@{entity.getName()}">
				<tr>
					@{searchMetawidget}
				</tr>
			</c:forEach>
		</tbody>
	</table>

	<br/>

</div>
