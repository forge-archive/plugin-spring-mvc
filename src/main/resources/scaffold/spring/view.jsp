<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}">

		@{metawidget}

	</form:form>

	<form:form commandName="@{ccEntity}" action="$${@{ccEntity}.id}/delete" method="POST">
		<input type="submit" value="Delete"/>
	</form:form>
</div>
