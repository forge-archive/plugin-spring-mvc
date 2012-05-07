<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}" action="$${@{ccEntity}.id}">

		@{metawidget}

		<input type="submit" value="Save"/>
		<form:form commandName="@{ccEntity}" action="/@{entityPlural.toLowerCase()}" method="GET">
			<input type="submit" value="Cancel">
		</form:form>
		<form:form commandName="@{ccEntity}" action="$${@{ccEntity}.id}/delete" method="POST">
			<input type="submit" value="Delete"/>
		</form:form>
	</form:form>
</div>
