function fex_openCsv(doc) {
    var featureTable = doc.getElementById('fTable');
    var numRows = featureTable.rows.length - 1; // subtract header row

    //alert("Type: " + doc.toString())
    var newWin = window.open('about:blank', "_blank");

    var newDoc = newWin.document;
    newDoc.writeln("<html>");
    newDoc.writeln("<head>");
    newDoc.writeln("<title>Result from " + doc.title + "</title>");
    newDoc.writeln("<link rel=\"stylesheet\" type=\"text/css\" href=\"fex-overview.css\"/>");
    newDoc.writeln("</head>");
    newDoc.writeln("<body>");
    newDoc.writeln("<table>");

    for (var i = 0; i < numRows; i++) {
        var selId = "label" + i;
        var selElem = doc.getElementById(selId);
        var patchName = selElem.name;
        var flag = "0";
        for (var j = 1; j < selElem.length; j++) {
            var optElem = selElem[j];
            if (optElem.selected) {
                flag = "1";
                break;
            }
        }

        if (i == 0) {
            // Write header line
            newDoc.write("<tr>");
            newDoc.write("<th>index</th>");
            newDoc.write("<th>patch</th>");
            newDoc.write("<th>flag</th>");
            for (var j = 1; j < selElem.length; j++) {
                var optElem = selElem[j];
                newDoc.write("<th>" + optElem.value + "</th>");
            }
            newDoc.writeln("</tr>");
        }

        // Write data line
        newDoc.writeln("<tr>");
        newDoc.write("<td>" + (i+1) + "</td>");
        newDoc.write("<td>" + patchName + "</td>");
        newDoc.write("<td>" + flag + "</td>");
        for (var j = 1; j < selElem.length; j++) {
            var optElem = selElem[j];
            newDoc.write("<td>" + (optElem.selected ? "1" : "0") + "</td>");
        }
        newDoc.writeln("</tr>");
    }

    newDoc.writeln("</table>");
    newDoc.writeln("</body>");
    newDoc.writeln("</html>");
    newDoc.close();

    newWin.focus();
}
