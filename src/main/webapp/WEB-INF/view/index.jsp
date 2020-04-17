
<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8"/>
        <title>Home page</title>
        <style>
            input[type="button"],input[type="submit"]{
                -webkit-border-radius: 5px;
                border-radius: 5px;
            }
            input[type='text']{
                width:calc( 100% - 100px);
            }
            .msg{
                font-weight:bold;
                font-size: 14pt;
                color:red;
            }
            .zip{
                color:red;
            }
            .header{
                color:darkcyan;
                font-size:20px;
                margin:0 0 10px 10px;
            }
            .title{
                margin-top:8px;
                font-size:16px;
                font-weight:bold;
            }
            .addr {
                color:blue;
            }
            td,th{
               text-align:left;
            }
        </style>
        <script type="text/javascript">
            function doSubmit(form) {
                if (!form.elements["addr"].value) {
                    return alert("請輸入地址！"),form.elements["addr"].focus(),false;
                } else
                    return true;
            }
        </script>
    </head>
    <body onload="doOnload()">
        <div class="header">3+3碼郵遞區號速查 請輸入地址</div>
        <form action="${pageContext.request.contextPath}/" method="post" onsubmit="return doSubmit(this)">
            <input type="text" name="addr" placeholder="輸入地址，如'台北市萬華區大理街132之10號','花蓮縣鳳林鎮信義路249號'等" 
                   value="${param.addr}" onfocus="select()" autofocus required/>
            <input type="submit" value="查 詢" />
        </form>
        <c:if test="${'POST' eq pageContext.request.method}">
            <table style="border: none;border-collapse: collapse;">
                <thead>
                    <tr><th class="title">查詢地址：</th><th class="title addr" colspan="2">${param.addr}</th></tr>
                    <c:if test="${fn:length(zips) gt 1}"><tr><th colspan="3">有以下幾種可能：</th></tr></c:if>
                </thead><tbody>
                <c:if test="${not empty message}">
                    <tr><td colspan="3" class="msg">${message}</td></tr>
                  <div class="msg">${message}</div>
                </c:if>
                <c:if test="${fn:length(zips) eq 1}">
                    <tr><td>郵遞區號：</td><td colspan="2" class="msg" id="zipcode">${zips[0].zipcode}</td></tr>
                    <tr><td>街道範圍：</td><td>${zips[0].cas.road}</td><td>${zips[0].scope}<c:if test="${not empty zips[0].recognition}">(${fn:toUpperCase(zips[0].recognition)})</c:if></td></tr>
                </c:if>
                <c:if test="${fn:length(zips) gt 1}"><c:forEach var="zip" items="${zips}">
                    <tr><td class="zip">${zip.zipcode}</td><td>${zip.cas.area} ${zip.cas.road}</td><td>${zip.scope}<c:if test="${not empty zip.recognition}">(${fn:toUpperCase(zip.recognition)})</c:if></td></tr>
                </c:forEach></c:if>
                </tbody>
            </table>
        </c:if>
    </body>
</html>
