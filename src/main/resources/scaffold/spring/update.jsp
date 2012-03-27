<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}" action="$${@{ccEntity}.id}">

		@{metawidget}

		<input type="submit" value="Update"/>

	</form:form>
</div>
