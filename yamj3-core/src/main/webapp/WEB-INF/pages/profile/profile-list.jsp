<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<!DOCTYPE html>
<html>
    <head>
        <title>YAMJ v3</title>
        <!--Import the header details-->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="HEAD" />
        </c:import>
    </head>
    <body>
        <!--Import the navigation header-->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="NAV" />
        </c:import>

        <div id="logo">
            <h2><spring:message code="title.artwork.profiles"/></h2>
        </div>

        <table id="headertable" class="hero-unit" style="width: 90%; margin: auto;">
        <thead>
            <tr>
	            <th style="vertical-align:top"><spring:message code="label.name"/></th>
                <th style="vertical-align:top" class="center"><spring:message code="page.artwork.profiles.label.applyto"/></th>
	            <th style="vertical-align:top" class="center"><spring:message code="page.artwork.profiles.label.artworktype"/></th>
                <th style="vertical-align:top" class="center"><spring:message code="page.artwork.profiles.label.width"/></th>
	            <th style="vertical-align:top" class="center"><spring:message code="page.artwork.profiles.label.height"/></th>
	            <th style="vertical-align:top" class="center"><spring:message code="page.artwork.profiles.label.scaling"/></th>
	            <th style="vertical-align:top" class="center"><spring:message code="page.artwork.profiles.label.reflection"/></th>
	            <th style="vertical-align:top" class="center"><spring:message code="page.artwork.profiles.label.roundedcorners"/></th>
                <th style="vertical-align:top" class="center"><spring:message code="page.artwork.profiles.label.quality"/></th>
	            <th style="vertical-align:top" class="center"><spring:message code="page.artwork.profiles.label.preprocess"/></th>
	            <th/>
            </tr>
        </thead>
        <tbody>
            <c:forEach items="${profilelist}" var="profile" varStatus="row">
                <tr>
                    <td>${profile.profileName}</td>
                    <td class="center">${profile.metaDataType}</td>
                    <td class="center">${profile.artworkType}</td>
                    <td class="center">${profile.width}</td>
                    <td class="center">${profile.height}</td>
                    <td class="center">
                        <c:choose>
                            <c:when test="${profile.scalingType == 'NORMALIZE'}"><spring:message code="page.artwork.profiles.scale.normalize"/></c:when>
                            <c:when test="${profile.scalingType == 'STRETCH'}"><spring:message code="page.artwork.profiles.scale.stretch"/></c:when>
                            <c:otherwise><spring:message code="page.artwork.profiles.scale.default"/></c:otherwise>
                        </c:choose>
                    </td>
                    <td class="center">
                        <c:if test="${profile.reflection == true}">
                           <img src="${pageContext.request.contextPath}/images/checked.png" alt="enabled" style="width:16px;height:16px"/>
                        </c:if>
                    </td>
                    <td class="center">
                        <c:if test="${profile.roundedCorners == true}">
                           <img src="${pageContext.request.contextPath}/images/checked.png" alt="enabled" style="width:16px;height:16px"/>
                        </c:if>
                    </td>
                    <td class="center">${profile.quality}</td>
                    <td class="center">
                        <c:if test="${profile.preProcess == true}">
                           <img src="${pageContext.request.contextPath}/images/checked.png" alt="enabled" style="width:16px;height:16px"/>
                        </c:if>
                    </td>
                    <td class="center" style="width:1%">
                        <span class="nobr">
                        <a href="${pageContext.request.contextPath}/profile/edit/${profile.id}.html" class="btn info"><spring:message code="button.edit"/></a>
                        <a href="${pageContext.request.contextPath}/profile/generate/${profile.id}.html" class="btn info"><spring:message code="page.artwork.profiles.button.regenerate"/></a>
                       </span>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
        <tfoot>
            <tr>
                <td colspan="10" class="right">
                    <c:if test="${errorMessage != null}">
                    <span id="messageError" style="align:right">${errorMessage}</span>
                    </c:if>
                    <c:if test="${successMessage != null}">
                    <span id="messageSuccess" style="align:right">${successMessage}</span>
                    </c:if>
                </td>
            </tr>
        </tfoot>
        </table>
        
        <!-- Import the footer -->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>

    </body>
</html>
