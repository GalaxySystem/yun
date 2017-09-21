<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<base href="<%=basePath%>">

<title>My JSP 'more.jsp' starting page</title>

<meta http-equiv="pragma" content="no-cache">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="expires" content="0">
<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
<meta http-equiv="description" content="This is my page">
<link href="${pageContext.request.contextPath }/css/bootstrap.min.css"
	rel="stylesheet" />
<link href="${pageContext.request.contextPath }/css/layer.css"
	rel="stylesheet" />
<script src="${pageContext.request.contextPath }/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath }/js/bootstrap.min.js"></script>
<script src="${pageContext.request.contextPath }/js/layer.js"></script>
<script type="text/javascript">
	var currentPath;
	var username;
		$(document).ready(function() {
			currentPath = "\\";
			username = $("#username").val();
			//全选
			$("#checkAll").click(function () {
				$("input[name='check_name']").prop("checked", $(this).prop("checked"));
			});
		});
		function selectCheckbox(){
			$inputs = $("input[name='check_name']");
			$("#checkAll").prop("checked", $inputs.filter(":checked").length == $inputs.length);
		}
		
	function getFiles(path) {
		$.post("file/getShareFiles.action",
			{
				"path" : path,
				"username" : "1234"
			},function(data) {
				if (data.success) {
					currentPath = path;
					$("#list").empty();
					$.each(data.data, function() {
						$("#list").append('<tr>'
											+ '<td><input type="checkbox" onclick="selectCheckbox()" name="check_name" aria-label="..."></td>'
											+ '<td><a href="#" prepath='+ currentPath +' isFile="'+ this.isFile+ '" onclick="return preDirectory(this)">'+ this.fileName+ '</a></td>'
											+ '<td width="32px"><a href="#" class="glyphicon glyphicon-download-alt" title="下载" onclick="return downloadFile(this)"></a></td>'
											+ '<td>'+ this.fileSize+ '</td>'
											+ '<td>'+ this.lastTime+ '</td>'
											+ '</tr>');
					});
				}
		});
	}
	function preDirectory(obj) {
		if ($(obj).attr("isfile") == "false") {
			var prePath = $(obj).attr("prepath");
			var name = $(obj).text();
			var path;
			if(prePath == null){
				path = $(obj).attr("path");
			}else{
				path = prePath + "\\" + name;
			}
			getFiles(path);
			navPath(path, name);
		}
		return false;
	}
	function theClick(obj) {
		getFiles($(obj).attr("path"));
		$(obj).nextAll().remove();
		return false;
	}

	function navPath(path, currentPath) {
		$("#navPath").append(
				'<a href="#" path="' + path
						+ '" onclick="return theClick(this)">&nbsp;'
						+ currentPath + '&nbsp;&#62;</a>');
	}
	function allFile() {
		window.location.reload();
		return false;
	}
	function downloadFileSelect(obj){
		var $download = $("input:checked");
		var downPath = "";
		$.each($download.parent().next().children(),function(i,n){
			var path = $(this).attr("path");
			if(path==null){
				path = $(this).text();
			}
			downPath += "&downPath=" + encodeURI(path);
		});
		if($download.length <= 0){
			alert("必须选择一个");
			$download.removeAttr("checked");
		}else{
			return download(obj, downPath);
// 			url += downPath;
// 			alert(currentPath);
// 			alert(url);
// 			$(obj).attr("href", url);
// 			return true;
		}
	}
	function downloadFile(obj){
		$file = $(obj).parent().prev().children();
		var path = $file.attr("path");
		if(path==null){
			path = $file.text();
		}
		return download(obj, "&downPath="+ escape(path));
	}
	function download(obj, path){
		var url = "file/download.action?";
		url += ("currentPath=" + encodeURI(currentPath));
		url += ("&username=" + username);
		url += path;
		$(obj).attr("href", url);
		return true;
	}
</script>
</head>
<body>
	<div class="content">
		<div class="top">
			<%@include file="top.jsp"%>
		</div>
		<div style="margin-top: 80px;">
			<div class="panel panel-default"
				style="margin-left: 10px; width: 80%; float: left;">
				<!-- Default panel contents -->
				<div class="panel-heading" id="pathnav">
					<div class="row">
						<div class="col-lg-11" style="margin-top: 10px;">
						<a href="#" path="" onclick="return allFile()">全部文件 ></a> <span
							id="navPath"> </span>
					</div>
					<div class="col-lg-1">
						<div class="input-group ">
<!-- 							<input type="text" class="form-control" placeholder="请输入搜索内容"> -->
							<span class="input-group-btn">
								<a class="btn btn-primary" target="_blank" onclick="return downloadFileSelect(this)">下载</a>
							</span>
						</div>
					</div>
					</div>
				</div>

				<table class="table table-hover" id="mytable">
					<thead>
						<tr>
							<th><input id="checkAll" type="checkbox" aria-label="..."></th>
							<th colspan="2" width="70%">文件名</th>
							<th>大小</th>
							<th>修改时间</th>
						</tr>
					</thead>
					<tbody id="list" currentPath="">
						<c:forEach var="file" items="${files }">
							<tr>
								<td><input type="checkbox" name="check_name" onclick="selectCheckbox()" aria-label="..."></td>
								<td><a href="#" isFile="${file.isFile }"
									path="${file.path }" onclick="return preDirectory(this)">${file.fileName }</a></td>
								<td><a href="#"
									class="glyphicon glyphicon-download-alt" onclick="downloadFile(this)" title="下载"></a></td>
								<td>${file.fileSize }</td>
								<td>${file.lastTime }</td>
							</tr>
						</c:forEach>
					</tbody>
					<form>
						<input type="hidden" value="${files[0].shareUser }" id="username"/>
					</form>
				</table>
			</div>
			<div class="panel panel-default"
				style="float: right; margin-right: 20px; width: 17%">
				<div class="panel-body">
					Basic panel example<br> Basic panel example<br> Basic
					panel example<br> Basic panel example<br> Basic panel
					example<br> Basic panel example<br> Basic panel example<br>
					Basic panel example<br> Basic panel example<br> Basic
					panel example<br> Basic panel example<br> Basic panel
					example<br> Basic panel example<br> Basic panel example<br>
					Basic panel example<br> Basic panel example<br> Basic
					panel example<br> Basic panel example<br> Basic panel
					example<br>

				</div>
			</div>
		</div>
	</div>
</body>
</html>
