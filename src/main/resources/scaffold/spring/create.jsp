<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="section">
	<form:form commandName="@{ccEntity}">
	
		@{metawidget}

		<br/>

		<input type="submit" value="Save"/>
		<input type="submit" value="Cancel" onclick="window.location='<c:url value="@{targetDir}@{entityPlural.toLowerCase()}"/>'"/>
	</form:form>
</div>
