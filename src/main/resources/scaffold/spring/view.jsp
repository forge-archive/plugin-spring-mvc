<div id="content">
	<form:form commandName="@{ccEntity}">

		@{metawidget}

	</form:form>

	<form:form commandName="@{ccEntity}" action="$${@{ccEntity}.id}/delete" method="POST">
		<input type="submit" value="Delete"/>
	</form:form>
</div>
