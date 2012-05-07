<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}">
	
		@{metawidget}

		<br/>

		<div class="buttons">
			<table>
				<tbody>
					<tr>
						<td>
							<input type="submit" value="Save"/>
						</td>
						<td>
							<input type="submit" value="Cancel" onclick="window.location='<c:url value="@{targetDir}@{entityPlural.toLowerCase()}"/>'"/>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</form:form>
</div>
