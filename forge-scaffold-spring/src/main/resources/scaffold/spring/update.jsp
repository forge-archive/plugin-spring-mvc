<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}" onsubmit="onSubmit();" id="editForm" name="editForm">

		@{metawidget}
		
	<div class="buttons">
		<table>
			<tbody>
				<tr>
					<td>
						<input type="submit" value="Save" class="button" onclick="document.pressed=this.value"/>
					</td>
					<td>
						<input type="submit" value="Delete" class="button" onclick="document.pressed=this.value"/>
					</td>
					<td>
						<a class="button" href="<c:url value="@{targetDir}@{entityPlural.toLowerCase()}"/>">Cancel</a>
					</td>
				</tr>
			</tbody>
		</table>
	</div>		

	</form:form>

</div>

<script type="text/javascript">
	function onSubmit() {
		if (document.pressed == 'Save') {
			document.editForm.action = "$${@{ccEntity}.id}";
		} else if (document.pressed == 'Delete') {
			document.editForm.action = "$${@{ccEntity}.id}/delete";
		}
		return true;
	}
</script>
