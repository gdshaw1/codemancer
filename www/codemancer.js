// This file is part of Codemancer.
// Copyright 2015-2016 Graham Shaw.
// All rights reserved.

var rev = -1;
var reloadCodeTable = 1;
var codeTableInfo = [];
var xhr = null;

// Construct hash containing name-value pairs from URI query string.
var Query = function() {
	// Extract query string from URI, removing the leading '?'.
	var queryString = window.location.search.substring(1);

	// Split into name-value pairs.
	var pairs = queryString.split("&");

	// Insert pairs into hash.
	for (var i = 0; i < pairs.length; i++) {
		var pair = pairs[i].split("=");
		this[pair[0]] = pair[1];
	}
}
var query = new Query();
var db = query['db'];
var sub = query['sub'];

/** Convert integer to upper case hex.
 * @param value the integer to be converted
 * @param digits the number of digits required (up to 16)
 * @return the value as a hex string (no prefix)
 */
function intToHex(value, digits) {
	var string = "0000000000000000"+value.toString(16);
	return string.slice(-digits).toUpperCase();
}

/** Handle click on an area link.
 * @param event the click event
 */
function handleAreaClick(event) {
	history.pushState({}, '', event.target.getAttribute('href'));
	sub = event.target.id.replace('area-','');
	reloadCodeTable = 1;
	if (xhr) { xhr.abort(); }
	requestChangeset();
	return false;
}

/** Update the content of a tree view element.
 * @param root the DOM element at the root of the tree or subtree to be updated
 * @param updates an ordered list of changes to be applied to the tree or subtree
 * The root element should be a 'ul' element. It should not have any content,
 * other than that given to it by this function.
 * Each update has up to three components:
 * - a unique ID which uniquely identifies the node to be updated (required);
 * - a string used to label the node (optional); and
 * - a list of updates to be applied recursively to descendants of the node
 *   (optional).
 * If the label is missing or null then the node is deleted. Any descendants of
 * the deleted node are deleted implicitly, therefore they need not be listed
 * as updates in their own right.
 * If the list of updates is missing or null then the node becomes an internal
 * node (capable of being expanded or collapsed), otherwise it becomes an
 * external node (capable of being selected).
 * The unique ID is used as the 'id' attribute of the corresponding 'li' element
 * in the DOM, so it must be globally unique within the page (not just within the
 * tree view). The ID is also used to determine the order in which the children
 * of a node are displayed. Updates must be supplied in ascending order of ID.
 * Each label is displayed within a 'button' element.
 */
function updateTreeView(root, updates) {
	var currentLi = root.firstChild;

	for (var i = 0; i != updates.length; i++) {
		// Extract the update to be applied.
		var update = updates[i];
		var id = update[0];
		var label = update[1];
		var content = update[2];

		// Skip over any list items which precede the node to be updated.
		while (currentLi && (currentLi.getAttribute('id') < id)) {
			currentLi = currentLi.nextSibling;
		}

		if (label == null) {
			// Delete the list item with the given ID.
			if (currentLi && (currentLi.getAttribute('id') == id)) {
				// The current list item matches, so delete it.
				var nextLi = currentLi.nextSibling;
				root.removeChild(currentLi);
				currentLi = nextLi;
			}
		} else {
			// Ensure that a list item exists with the given ID
			// (but do not add it to the tree yet if it is a new one).
			var li = currentLi;
			if (!(li && (currentLi.getAttribute('id') == id))) {
				li = document.createElement('li');
				li.setAttribute('id', id);
			}
			var child = li.firstChild;

			if (content) {
				// Internal node:
				// Ensure that the node has a checkbox.
				var cb = child;
				if (!(cb && cb.classList.contains('tree-control'))) {
					var cb = document.createElement('input');
					cb.setAttribute('id', 'checkbox-' + id);
					cb.setAttribute('type', 'checkbox');
					cb.classList.add('tree-control');
					li.insertBefore(cb, child);
					child = cb;
				}
				child = child.nextSibling;

				// Ensure that the node has a label.
				if (!(child && child.classList.contains('tree-label'))) {
					var tl = document.createElement('label');
					tl.classList.add('tree-label');
					tl.setAttribute('for', 'checkbox-' + id);
					tl.textContent = label;
					li.insertBefore(tl, child);
					child = tl;
				}
				child = child.nextSibling;

				// Ensure that the list item has a sublist.
				var ul = child;
				if (!(ul && (ul.classList.contains('tree')))) {
					ul = document.createElement('ul');
					ul.setAttribute('class', 'tree');
				}

				// Update the subtree with the required list of changes.
				updateTreeView(ul, content);

				// Ensure that the sublist is attached to the current
				// list item.
				if (ul != child) {
					li.insertBefore(ul, child);
					child = ul;
				}
				child = child.nextSibling;
			} else {
				// External node:
				// Ensure that the node has a button.
				var cb = child;
				if (!(cb && cb.classList.contains('tree-link'))) {
					href = '?db=' + db + '&sub=' + id.toString(16);
					var cb = document.createElement('a');
					cb.setAttribute('id', 'area-' + id.toString(16));
					cb.setAttribute('href', href);
					cb.classList.add('tree-link');
					cb.onclick = handleAreaClick;
					cb.textContent = label;
					li.insertBefore(cb, child);
					child = cb;
				}
				child = child.nextSibling;
			}

			// Remove any remaining children.
			while (child) {
				var node = child;
				child = child.nextSibling;
				li.removeChild(node);
			}

			// If the list item is a new one then add it to the tree.
			if (li != currentLi) {
				root.insertBefore(li, currentLi);
			}
		}
	}
}

/** Process areas from changeset.
 * Each area is represented by an array with two components: a fixed ID
 * (which determines the ordering), and a name. For areas that have been
 * deleted, only the ID is listed.
 * @param areas an array of areas
 */
function processAreas(areas) {
	var areaTree = document.getElementById("areas");
	if (areaTree == null) {
		document.getElementById("nav").innerHTML = "<ul id='areas' class='tree root'></ul>";
		areaTree = document.getElementById("areas");
	}

	updateTreeView(document.getElementById("areas"), areas);
}

/** Process lines from changeset.
 * Each line is represented by an array with four components: a minimum
 * address, a maximum address, a type, and the disassembled instruction.
 * The lines must not overlap, and must be listed in ascending address order.
 * @param lines an array of lines.
 */
function processLines(lines) {
	var codeTable = document.getElementById("code");
	if (codeTable == null) {
		document.getElementById("content").innerHTML = "<table id='code'></table>";
		codeTable = document.getElementById("code");
	}

	var i = 0;

	if (reloadCodeTable != 0) {
		codeTable.innerHTML = "";
		codeTableInfo = [];
		reloadCodeTable = 0;
	}

	// For each supplied line:
	for (var j = 0; j != lines.length; j++) {
		var line = lines[j];

		// Construct a DOM element for the new row.
		var td1 = document.createElement('td');
		td1.appendChild(document.createTextNode(intToHex(line[0], 4)));
		var td2 = document.createElement('td');
		td2.appendChild(document.createTextNode(line[2]));
		var td3 = document.createElement('td');
		var tr = document.createElement('tr');
		tr.setAttribute("onclick", "handleLineClick(this);");
		tr.appendChild(td1);
		tr.appendChild(td2);
		tr.appendChild(td3);

		// Construct new entry for insertion into codeTableInfo.
		var rowInfo = [line[0], tr];

		// Skip over any existing rows located prior to the new row.
		while ((i < codeTableInfo.length) && (codeTableInfo[i][0] < line[0])) {
			++i;
		}

		// Insert the new row, replacing an existing one if appropriate.
		if (i < codeTableInfo.length) {
			if (codeTableInfo[i][0] == line[0]) {
				codeTable.replaceChild(tr, codeTableInfo[i][1]);
				codeTableInfo[i] = rowInfo;
			} else {
				codeTable.insertBefore(tr, codeTableInfo[i][1]);
				codeTableInfo.splice(i, 0, rowInfo);
			}
		} else {
			codeTable.insertBefore(tr, null);
			codeTableInfo.push(rowInfo);
		}
		++i;

		// Remove any rows which overlap with the new line.
		while ((i < codeTableInfo.length) && (codeTableInfo[i][0] <= line[1])) {
			codeTable.removeChild(codeTableInfo[i][1]);
			codeTableInfo.splice(i, 1);
		}
	}
}

/** Process a changeset from the server.
 * @param changeset the changeset to process
 */
function processChangeset(changeset) {
	if (changeset.areas != null) {
		processAreas(changeset.areas);
	}
	if (changeset.lines != null) {
		processLines(changeset.lines);
	}
	rev = changeset.rev;
}

/** Repeatedly request and process changesets from the server. */
function requestChangeset() {
	var minAreaRev = rev + 1;
	var minCodeRev = rev + 1;
	if (reloadCodeTable != 0) {
		minCodeRev = 0;
		document.getElementById('loading').style.display = 'block';
	}

	xhr = new XMLHttpRequest();
	xhr.open("GET", "/changeset.json?db=" + db + "&arearev=" + minAreaRev + "&coderev=" + minCodeRev + "&sub=" + sub.toString(16), true);
	xhr.onreadystatechange = function() {
		if (xhr.readyState == 4) {
			document.getElementById('loading').style.display = 'none';
			if (xhr.status >= 200 && xhr.status < 300) {
				// If the request was successful then process the response, then immediately request again.
				retryCounter = 0;
				var changeset = eval(xhr.responseText);
				processChangeset(changeset);
				requestChangeset();
			} else {
				// If the request failed, display the response as an error.
				var contentDiv = document.getElementById("content");
				contentDiv.innerHTML = xhr.responseText;
			}
		}
	}
	xhr.send(null);
}
