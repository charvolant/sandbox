<!DOCTYPE html>
<html lang="en-AU">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="app.version" content="${g.meta(name:'app.version')}"/>
    <meta name="app.build" content="${g.meta(name:'app.build')}"/>
    <meta name="description" content="${grailsApplication.config.skin.orgNameLong}"/>
    <meta name="author" content="${grailsApplication.config.skin.orgNameLong}">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="${g.createLink(uri: '/')}"/>
    <title><g:layoutTitle /></title>
    <r:require modules="bootstrap"/>
    <r:layoutResources/>
    <g:layoutHead />
    <hf:head/>
</head>
<body class="${pageProperty(name:'body.class')}" id="${pageProperty(name:'body.id')}" ng-app="ala.sandbox">

<!-- Header -->
<hf:banner logoutUrl="${g.createLink(controller:"logout", action:"logout", absolute: true)}" />
<!-- End header -->
<g:set var="fluidLayout" value="${pageProperty(name:'meta.fluidLayout')?:grailsApplication.config.skin?.fluidLayout.toBoolean()}"/>

<g:layoutBody />

<!-- Footer -->
<hf:footer/>
<!-- End footer -->

<!-- JS resources-->
<g:render template="/configjs" />
<r:layoutResources disposition="defer"/>
</body>
</html>