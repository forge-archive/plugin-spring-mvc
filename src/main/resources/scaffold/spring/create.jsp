<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<html>

  <div class="header">
    <head>
      <title>Create New @{entity.getName()}</title>
    </head>
  </div>

  <div class="subheader">
    Create a new @{entityName}
  </div>

  <div class="main">
    <form:form commandName="@{ccEntity}" id="create">
      @{metawidget}

      <input type="submit" value="Create"/>
    </form:form>
  </div>

<html>
