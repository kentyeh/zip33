
<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8"/>
        <title>Home page</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/wro/all.css"/>
        <script src="${pageContext.request.contextPath}/wro/all.js"></script>
    </head>
    <body>
        <div style="margin:0 0 10px 10px; width:300px;">3+2碼郵遞區號速查 請輸入地址</div>
        <form action="${pageContext.request.contextPath}/" method="post" onsubmit="return doSubmit(this)">
            <input type="text" name="addr" size="100" placeholder="輸入地址，如'台北市萬華區大理街132之10號','花蓮縣鳳林鎮信義路249號'等" 
                   value="${param.addr}"/>
            <input type="submit" value="查 詢" />
        </form>
        <c:if test="${'POST' eq pageContext.request.method}">
            <c:if test="${not empty message}">
                <div class="msg">${message}</div>
            </c:if>
            <c:if test="${empty zips}">
                <div class="msg">找不到對應的資料，請查明地址是否正確</div>
            </c:if>
            <c:if test="${fn:length(zips) eq 1}">
                <table>
                    <tbody>
                        <tr><td>郵遞區號：</td><td colspan="2" class="msg" id="zipcode">${zips[0].zipcode}</td></tr>
                        <tr><td>地　　區：</td><td>${zips[0].oriInfo}</td><td>${zips[0].tailInfo}</td></tr>
                    </tbody>
                </table>
            </c:if>
            <c:if test="${fn:length(zips) gt 1}">
                <table>
                    <thead>
                        <tr><th colspan="3">有以下幾種可能：</th></tr>
                        <tr><th>郵遞區號</th><th colspan="2">地　　區</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="zip" items="${zips}">
                            <tr><td class="zip">${zip.zipcode}</td><td>${zip.oriInfo}</td><td>${zip.tailInfo}</td></tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:if>
        </c:if>
    </body>
</html>
