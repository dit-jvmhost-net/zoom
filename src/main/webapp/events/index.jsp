<%@taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="do"%>
<sql:query var="events" dataSource="jdbc/zoom">
	select * from event
</sql:query>
<html>
<head>
<title>Zoom Events</title>
</head>
<body>
	<do:forEach var="event" items="${events.rows}">
	    Timestamp: ${event.timestamp}<br />
    	Object: ${event.object}<br />
    	Action: ${event.action}<br />
    	JSON: ${event.json}<br />
		<hr>
	</do:forEach>
</body>
</html>