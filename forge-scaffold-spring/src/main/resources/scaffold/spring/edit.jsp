<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<form:form commandName="@{ccEntity}" name="edit" onsubmit="onSubmit()">

	@{metawidget}

	<span class="buttons">
		<input type="submit" value="Save" class="btn btn-primary" onclick="document.pressed=this.value"/>
		<a class="btn btn-primary" href="<c:url value="@{targetDir}@{entityPlural.toLowerCase()}"/>">Cancel</a>
		<input type="submit" value="Delete" class="btn btn-primary" onclick="document.pressed=this.value"/>
	</span>
</form:form>

<script type="text/javascript">
	function onSubmit()
	{
		if (document.pressed == 'Save')
		{
			document.edit.action = "$${@{ccEntity}.id}";
		}
		else if (document.pressed == 'Delete')
		{
			document.edit.action = "$${@{ccEntity}.id}/delete";
		}

		return true;
	}
</script>