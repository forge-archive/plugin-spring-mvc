<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}">
	
		@{metawidget}

		<br/>

		<input type="submit" value="Create New @{entity.getName()}"/>
	</form:form>
</div>
