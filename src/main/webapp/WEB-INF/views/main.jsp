<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<html>
<head>
<title>Last.FM to YouTube</title>
</head>
<script src="https://code.jquery.com/jquery-1.12.4.min.js"
	integrity="sha256-ZosEbRLbNQzLpnKIkEdrPv7lOy9C27hHQ+Xp8a4MxAQ="
	crossorigin="anonymous"></script>
<script type="text/javascript">
	$(document).ready(function() {
		$("#listen").click(function(event) {
			var artist = $("#artist").val();
			if (!artist) {
				$("#errors").html("<h4>error</h4><p>please enter an artist name</p>");
				return;
			}
			$("#artist").val("");
			$("#input").html("");
			$("#errors").html("");
			$("#description").html("");
			$("#contact").html("");
			$("#loading_info").html("<h4>please wait...</h4><p>creating a youtube playlist based on last.fm data for '<i>" + artist + "</i>'...</p>");
			$("#loading_div").css("display", "block");
			$.ajax({
				url : "authorize",
				type : "GET",
				data : { "artist" : artist },
				contentType : "application/json",
				success : function(authorizationUrl) {
					if (authorizationUrl) {
						window.location.replace(authorizationUrl);
					} else {
						window.location.replace("/listen?artist=" + artist);
					}
				}
			});
		});
		$("#artist").keypress(function (e) {
			if (e.which == 13) {
				$('#listen').click();
				return false;
			}
		});
	});
</script>
<body style="text-align: center; margin-top:5em; font-size: 220%; font-family: Courier New;">

	<div id="input">
		<label>artist:</label>
		<input id="artist" style="font-size: 58%">
		<button id="listen" style="font-size: 62%">listen</button>
	</div>
	
	<div id="errors" style="color:red; font-size:90%; font-style:italic">
		<c:if test="${not empty errorMessage}">
			<h4>server error</h4>
			<p>${errorMessage}</p>
		</c:if>		
	</div>
	
<!-- 	<div id="playlist"> -->
<%-- 		<c:if test="${not empty playlistId}"> --%>
<!-- 			<br /> -->
<%-- 			<iframe width="560" height="315" src="https://www.youtube.com/embed/videoseries?list=${playlistId}" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe> --%>
<%-- 		</c:if> --%>
<!-- 	</div> -->

	<div id="loading_div" style="display: none; font-size: 60%">
		<p id="loading_info"><p>
		<img src="loading.gif" style="width: 450px; height: 250px;" id="loading_image">
	</div>	

	<br />
	
	<div id="description" style="bottom:5em; width:100%; height:60px; font-size: 60%;">
		creates a youtube playlist with the 10 most played songs of an artist on <a href="http://www.last.fm" target="blank" style="color: inherit"><i>last.fm</i></a>
	</div>

    <div id="contact" style="bottom:5em; width:100%; height:60px; font-size: 40%;">
		contact: dsbankov@gmail.com
	</div>

</body>
</html>