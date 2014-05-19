"use strict";

var maxNumberOfLinks = 20;
var templates = {};
var requestContentMaxSize = 150;
var responseContentMaxSize = 150;

// init filters
$(function () {

    var timeFilters = $("#filters"),
        dateTimePickerFrom = $("#start-time-picker", timeFilters),
        dateTimePickerTo = $("#end-time-picker", timeFilters);

    dateTimePickerFrom.datetimepicker({
        changeMonth: true,
        numberOfMonths: 1,
        dateFormat: 'yy-mm-dd',
        separator: 'T',
        timeFormat: 'HH:mm:ssz',
        firstDay: 1,
        onClose: function (selectedDate) {
            dateTimePickerTo.datetimepicker("option", "minDate", selectedDate);
        }
    });
    dateTimePickerTo.datetimepicker({
        changeMonth: true,
        numberOfMonths: 1,
        dateFormat: 'yy-mm-dd',
        separator: 'T',
        timeFormat: 'HH:mm:ssz',
        firstDay: 1,
        onClose: function (selectedDate) {
            dateTimePickerFrom.datetimepicker("option", "maxDate", selectedDate);
        }
    });

    var now = new Date();
    dateTimePickerFrom.datetimepicker('setDate', new Date(now.getTime() - 3600 * 1000));
    dateTimePickerTo.datetimepicker('setDate', now);

    $("#start-time-picker").on('click', function () {
        $("#timeInputMethodFromTo").prop("checked", true);
    });
    $("#end-time-picker").on('click', function () {
        $("#timeInputMethodFromTo").prop("checked", true);
    });
    $("#lastTimeSelector").on('click', function () {
        $("#timeInputMethodLT").prop("checked", true);
    });

    // load Handlebars templates
    $.find('[type="text/x-handlebars-template"]').forEach(function (element) {
        $.get(element.src, function (data) {
            templates[element.id] = Handlebars.compile(data);
        });
    });

});

function requestAuditingEventsReport() {

    var startTime, endTime;
    var timeInputMethod = $("input:radio[name=timeInputMethod]:checked").val();

    if (timeInputMethod === "lastTime") {
        var lastTimeValue = $("#lastTimeSelector").val();
        if (!lastTimeValue) {
            alert("Please select time period.");
            return;
        }
        else if (lastTimeValue === "today") {
            startTime = new Date();
            startTime.setHours(0);
            startTime.setMinutes(0);
            startTime.setSeconds(0);
            startTime.setMilliseconds(0);
            endTime = new Date();
        }
        else {
            var duration = lastTimeValue * 1000;
            endTime = new Date();
            startTime = new Date(endTime.getTime() - duration);
        }
    }
    else if (timeInputMethod === "fromTo") {
        var startTimeString = $("#start-time-picker").val();
        startTime = new Date(startTimeString);
        var endTimeString = $("#end-time-picker").val();
        endTime = new Date(endTimeString);
    }
    else {
        alert("Please select time period.");
        return;
    }

    var data = {
        "startTime": startTime.getTime(),
        "endTime": endTime.getTime(),
        "filter": {
            "user": userUuid
        },
        "mode": "SYNC",
        "timeout": 300
    };

    // create audit events report
    $.ajax({
        type: "POST",
        url: getProxiedUrl(auditManagerAddress + "/audit_events"),
        data: JSON.stringify(data),
        contentType: "application/json"
    })
        .done(function (data, textStatus, jqXHR) {
            if (data.jobStatus === "SUCCESS") {
                var location = jqXHR.getResponseHeader("Location");
                var matches = /\/([\w-]+)$/.exec(location);
                var jobId = matches[1];

                drawDiagrams(jobId, startTime, endTime);
            }
        })
        .fail(function (jqXHR, textStatus) {
            alert(textStatus);
        });
}

function drawDiagrams(jobId, startTime, endTime) {

    // get list of all access tokens
    $.ajax({
        type: "GET",
        url: getProxiedUrl(auditManagerAddress + "/audit_events/reports/" + jobId + "/access_tokens"),
        dataType: "json"
    })
        .done(function (data, textStatus, jqXHR) {

            // set color for each access tokens
            var tokensColorMap = {};
            var rainbow = new Rainbow();
            var maxNumber = (data.length > 1) ? data.length - 1 : 1;
            rainbow.setNumberRange(0, maxNumber);
            data.forEach(function (d, i) {
                tokensColorMap[d] = "#" + rainbow.colorAt(i);
            });

            drawTimeline(jobId, startTime, endTime, tokensColorMap);
            drawInteractionDiagram(jobId, tokensColorMap);
        })
        .fail(function (jqXHR, textStatus) {
            alert(textStatus);
        });
}

function drawTimeline(jobId, startTime, endTime, tokensColorMap) {

    var timelineDiv = $('#timeline');
    var height = timelineDiv.height();
    var width = timelineDiv.width();

    var yAxisOffset = 120;
    var xAxisOffset = 50;
    var marginTop = 50;
    var marginBottom = 20;
    var marginLeft = 20;
    var marginRight = 20;
    var yAxisLabelMaxLength = 18;

    $.ajax({
        type: "GET",
        url: getProxiedUrl(auditManagerAddress + "/audit_events/reports/" + jobId + "/timeline"),
        dataType: "json"
    })
        .done(function (data, textStatus, jqXHR) {
            drawDiagram(data);
        })
        .fail(function (jqXHR, textStatus) {
            alert(textStatus);
        });

    function drawDiagram(data) {

        if ($("#timeline-svg").length > 0) {
            $("#timeline-svg").remove();
        }

        var svg = d3.select('#timeline').append('svg:svg')
            .attr('class', 'chart')
            .attr('width', width)
            .attr('height', height)
            .attr("id", "timeline-svg");

        // diagram border
        var borderPath = svg.append("svg:rect")
            .attr("x", 0)
            .attr("y", 0)
            .attr("width", width)
            .attr("height", height)
            .style("stroke", "#C0C0C0")
            .style("fill", "none")
            .style("stroke-width", "1px");

        // diagram title
        svg.append("svg:text")
            .attr("x", width / 2)
            .attr("y", 25)
            .text("Audit Events Timeline")
            .attr('class', 'chart-title');

        if (data.events.length == 0) {
            svg.append("svg:text")
                .attr("x", width / 2)
				.attr("y", height / 2)
                .text("No data available.")
                .attr('class', 'chart-message');
            return;
        }

        var yLabelsMap = {};
        var yAxisLabels = [];
        data.events.forEach(function (d) {
            d.time = new Date(d.time);
            if (!(d.target in yLabelsMap)) {
                yLabelsMap[d.target] = null;
                yAxisLabels.push({'label': d.target, 'title': d.target});
            }
        });

        // set color for each event
        data.events.forEach(function (d) {
            if (d.token) {
                d.color = tokensColorMap[d.token];
            }
            else {
                d.color = "#000000";
            }
        });

        var scaleExtent = [ 0, 200 ];

        // x axis
        var x = d3.time.scale()
            .domain([startTime, endTime])
            .range([yAxisOffset, width - marginRight]);

        var xAxis = d3.svg.axis()
            .scale(x)
            .tickSize(12, 1, 1);

        svg.append('g')
            .attr('class', 'x axis')
            .attr('transform', 'translate(0, ' + (height - xAxisOffset) + ')')
            .call(xAxis);

        // y axis
        var yAxisDomain = [];
        $.each(yAxisLabels, function (index, record) {
            yAxisDomain.push(record.label);
        })
        yAxisDomain.push(""); // x axis should be empty without any events

        var y = d3.scale.ordinal()
            .domain(yAxisDomain)
            .rangePoints([marginTop, height - xAxisOffset]);

        var yAxis = d3.svg.axis()
            .scale(y)
            .orient("left")
            .tickSize(6, 3, 1)
            .tickFormat(function (d) {
                return getComponentShortName(d);
            });

        var yAxisG = svg.append('g')
            .attr('class', 'y axis')
            .attr('transform', sprintf('translate(%d,%d)', yAxisOffset, 0))
            .call(yAxis);

        // y axis grid lines
        svg.append("g")
            .attr("class", "gridlines")
            .selectAll("line.y")
            .data(yAxisLabels)
            .enter().append("line")
            .attr("x1", yAxisOffset)
            .attr("x2", width - marginRight)
            .attr("y1", function (d) {
                return y(d.label)
            })
            .attr("y2", function (d) {
                return y(d.label)
            });


        // titles for y axis labels
        yAxisG.selectAll('.tick').each(function (d, i) {
            d3.select(this).selectAll("text")
                .attr("class", "y-axis-label");
        });

        // TODO: doesn't work
        $(".y-axis-label")
            .qtip({
                content: {
                    text: function (event, api) {
                        api.set('content.text', 'Title');
                        return 'Loading...'
                    }
                }
            });

        // dots
        var dots = svg.append('g')
            .attr('class', 'dot');

        dots.selectAll('circle')
            .data(data.events)
            .enter().append('svg:circle')
            .attr('r', '.50ex')
            .attr('id', function (d) {
                return d.id
            })
            .attr("class", "timeline-event");

        registerTimelineEventTooltip($('.timeline-event'), jobId);

        d3.select("svg").call(d3.behavior.zoom()
            //  By supplying only .x() any pan/zoom can only alter the x scale.  y
            //  scale remains fixed.
            .x(x)
            .scaleExtent(scaleExtent)
            .scale(1)
            .on("zoom", render)
        );

        render();

        function render() {
            dots.selectAll("circle")
                .attr('cx', function (d) {
                    return x(d.time);
                })
                .attr('cy', function (d) {
                    return y(d.target);
                })
                .attr('fill', function (d) {
                    return d.color;
                })
                .attr("visibility", function (d) {
                    return (x(d.time) < yAxisOffset || x(d.time) > width - marginRight) ? "hidden" : "visible";
                })
            ;

            xAxis.scale(x);
            svg.select(".x.axis").call(xAxis);
            svg.select(".y.axis").call(yAxis);
        }
    }

    function getComponentShortName(longName) {

        var regexp = /CN=([^,]+),/gi;
        var matches = regexp.exec(longName);
        var shortName = (matches) ? matches[1] : longName;
        if (shortName.length > yAxisLabelMaxLength) {
            return shortName.substr(0, yAxisLabelMaxLength);
        }
        else {
            return shortName;
        }
    }
}

function drawInteractionDiagram(jobId, tokensColorMap) {

    var interactionDiagramDiv = $('#interaction-diagram');
    var width = interactionDiagramDiv.width();
    var height = interactionDiagramDiv.height();

    $.ajax({
        type: "GET",
        url: getProxiedUrl(sprintf("%s/audit_events/reports/%s/interaction", auditManagerAddress, jobId)),
        dataType: "json"
    })
        .done(function (data, textStatus, jqXHR) {
            drawDiagram(data, tokensColorMap);
        })
        .fail(function (jqXHR, textStatus) {
            alert(textStatus);
        });


    function drawDiagram(data) {

        if ($("#interaction-svg").length > 0) {
            $("#interaction-svg").remove();
        }

        var svg = d3.select("#interaction-diagram").append("svg:svg")
            .attr("width", width)
            .attr("height", height)
            .attr("id", "interaction-svg");

        // diagram border
        var borderPath = svg.append("svg:rect")
            .attr("x", 0)
            .attr("y", 0)
            .attr("width", width)
            .attr("height", height)
            .style("stroke", "#C0C0C0")
            .style("fill", "none")
            .style("stroke-width", "1px");

        // diagram title
        svg.append("svg:text")
            .attr("x", width / 2)
            .attr("y", 25)
            .text("Interaction diagram")
            .attr('class', 'chart-title');

        if (data.length == 0) {
            svg.append("svg:text")
                .attr("x", width / 2)
				.attr("y", height / 2)
                .text("No data available.")
                .attr('class', 'chart-message');
            return;
        }

        //sort links by source, then target
        data.sort(function (a, b) {
            if (a.source > b.source) {
                return 1;
            }
            else if (a.source < b.source) {
                return -1;
            }
            else {
                if (a.target > b.target) {
                    return 1;
                }
                if (a.target < b.target) {
                    return -1;
                }
                else {
                    return 0;
                }
            }
        });
        //any links with duplicate source and target get an incremented 'linknum'
        for (var i = 0; i < data.length; i++) {
            if (i != 0 &&
                data[i].source == data[i - 1].source &&
                data[i].target == data[i - 1].target) {

                if (data[i - 1].linknum >= maxNumberOfLinks) {
                    data.splice(i, 1);
                    i--;
                }
                else {
                    data[i].linknum = data[i - 1].linknum + 1;
                }
            }
            else {
                data[i].linknum = 1;
            }
        }

        var nodes = {};

        // Compute the distinct nodes from the links.
        data.forEach(function (d) {
            d.source = nodes[d.source] || (nodes[d.source] = {name: d.source});
            d.target = nodes[d.target] || (nodes[d.target] = {name: d.target});

            // set link color
            if (d.token) {
                d.color = tokensColorMap[d.token];
            }
            else {
                d.color = "#5C5C5C";
            }
        });

        var force = d3.layout.force()
            .nodes(d3.values(nodes))
            .links(data)
            .size([width, height])
            .linkDistance(350)
            //.charge(-300)
            .charge(-1000)
            .on("tick", tick)
            .start();

        // Per-type markers, as they don't inherit styles.
        svg.append("svg:defs").selectAll("marker")
            .data(["suit", "licensing", "resolved"])
            .enter().append("svg:marker")
            .attr("id", String)
            .attr("viewBox", "0 -5 10 10")
            .attr("refX", 15)
            .attr("refY", -1.5)
            .attr("markerWidth", 6)
            .attr("markerHeight", 6)
            .attr("orient", "auto")
            .append("svg:path")
            .attr("d", "M0,-5L10,0L0,5");

        var path = svg.append("svg:g").selectAll("path")
            .data(force.links())
            .enter().append("svg:path")
            .attr('id', function (d) {
                return d.id;
            })
            .attr("class", function (d) {
                //return "link " + d.type;
                return "link"
            })
            .attr("stroke", function (d) {
                return d.color;
            })
            .attr("marker-end", function (d) {
                return "url(#" + d.type + ")";
            });

        var circle = svg.append("svg:g").selectAll("circle")
            .data(force.nodes())
            .enter().append("svg:circle")
            .attr("r", 6)
            .attr("id", function (d) {
                return d.name;
            })
            .attr("class", "interaction-node")
            .call(force.drag);
        /*.call(force.drag().origin(function () {
         var t = d3.transform(d3.select(this).attr("transform")).translate;
         return {x: t[0], y: t[1]};
         }).on("drag.force", function () {
         force.stop();
         d3.select(this).attr("transform", "translate(" + d3.event.x + "," + d3.event.y + ")");
         }));*/

        var text = svg.append("svg:g").selectAll("g")
            .data(force.nodes())
            .enter().append("svg:g");

        // A copy of the text with a thick white stroke for legibility.
        text.append("svg:text")
            .attr("x", 8)
            .attr("y", ".31em")
            .attr("class", "shadow")
            .text(function (d) {
                return getNodeShortName(d.name);
            });

        text.append("svg:text")
            .attr("x", 8)
            .attr("y", ".31em")
            .text(function (d) {
                return getNodeShortName(d.name);
            });

        $('.interaction-node')
            .qtip({
                content: {
                    text: function (event, api) {
                        return $(this).attr('id');
                    }
                },
                position: {
                    my: 'top left',
                    at: 'bottom right',
                    adjust: {
                        method: 'flip flip'
                    },
                    viewport: $(window)
                }
            })

        registerInteractionEventTooltip($('*[class~="link"]'), jobId);

        // Use elliptical arc path segments to doubly-encode directionality.
        function tick() {
            path.attr("d", function (d) {
                //var distance = Math.sqrt(Math.pow(d.target.x - d.source.x, 2) + Math.pow(d.target.y - d.source.y, 2));
                var dr = 4000 / Math.pow(d.linknum, 0.85);
                var sweepFlag = d.linknum % 2;

                return sprintf("M %f,%f A %f,%f 0 0,%d %f,%f",
                    d.source.x, d.source.y, dr, dr, sweepFlag, d.target.x, d.target.y);
            });

            circle.attr("transform", function (d) {
                return "translate(" + d.x + "," + d.y + ")";
            });

            text.attr("transform", function (d) {
                return "translate(" + d.x + "," + d.y + ")";
            });
        }

        function getNodeShortName(longName) {
            var regexp = /CN=([^,]+),/gi;
            var matches = regexp.exec(longName);
            return (matches) ? matches[1] : longName;
        }
    }
}

function registerTimelineEventTooltip(target, jobId) {
    target.qtip({
        content: {
            text: function (event, api) {

                $.ajax({
                    url: getProxiedUrl(sprintf("%s/audit_events/reports/%s/events/%s", auditManagerAddress, jobId, $(this).attr("id"))),
                    type: "GET",
                    dataType: "json"
                })
                    .then(function (data) {
                        var source = $("#event-info-template").html();
                        var template = Handlebars.compile(source);

                        var formatDate = d3.time.format("%Y-%m-%dT%H:%M:%S.%L%Z")
                        var time = formatDate(new Date(data.eventTime));
                        var getAttachmentByName = function (name) {
                            for (var i in data.attachments) {
                                if (data.attachments[i].name === name) {
                                    return data.attachments[i];
                                }
                            }
                        }
                        var httpRequestData = getAttachmentByName('http_request_data');
                        var httpResponseData = getAttachmentByName('http_response_data');

                        var accessTokenInfoDivId = api._id + '-atinfo';
                        var onBehalfOfDivId = api._id + '-onbehalfof';
                        var context = {
                            'id': data.id,
                            'time': time,
                            'action': data.action,
                            'outcome': data.outcome,
                            'initiatorId': data.initiator.id,
                            'onBehalfOfDivId': onBehalfOfDivId,
                            'accessTokenInfoDivId': accessTokenInfoDivId,
                            'targetId': data.target.id,
                            'requestMethod': (httpRequestData) ? httpRequestData.content.method : 'N/A',
                            'requestUrl': (httpRequestData) ? httpRequestData.content.url : 'N/A',
                            'requestContentType': (httpRequestData) ? httpRequestData.content.contentType : 'N/A',
                            'requestContent': (httpRequestData) ? shorten(httpRequestData.content.content, requestContentMaxSize) : 'N/A',
                            'responseStatusCode': (httpResponseData) ? httpResponseData.content.statusCode : 'N/A',
                            'responseContentType': (httpResponseData) ? httpResponseData.content.contentType : 'N/A',
                            'responseContent': (httpResponseData) ? shorten(httpResponseData.content.content, responseContentMaxSize) : 'N/A'
                        }
                        var html = templates["event-info-template"](context);

                        api.set('content.title', 'Event ' + data.id);
                        api.set('content.text', html);
                        //api.set('content.button', true);

                        // retrieve access token info from oauth-as
                        var accessTokenInfoDiv = $('div#' + accessTokenInfoDivId);
                        if (data.initiator.oauthAccessToken) {
                            accessTokenInfoDiv.text("Loading...");
                            $.ajax({
                                url: getProxiedUrl(sprintf("%s/access_tokens/%s", oauthASAddress, data.initiator.oauthAccessToken)),
                                type: "GET",
                                dataType: "json"
                            })
                                .done(function (data) {
                                    var clientLocation = '';
                                    if (data.client.countries.length > 0) {
                                        for (var i in data.client.countries) {
                                            var name = data.client.countries[i].name;
                                            if (i > 0) {
                                                clientLocation += ', ';
                                            }
                                            clientLocation += name;
                                        }
                                    }
                                    else {
                                        clientLocation = 'N/A';
                                    }

                                    var context = {
                                        'token': data.access_token,
                                        'expireTime': data.expire_time,
                                        'userUuid': data.owner.uuid,
                                        'clientName': data.client.name,
                                        'clientOrganization': data.client.organization.name,
                                        'clientLocation': clientLocation
                                    };

                                    var html = templates["access-token-info-template"](context);
                                    accessTokenInfoDiv.html(html);

                                    // retrieve user (resource owner) from the federation-api using user uuid obtained from the oauth-as
                                    var onBehalfOfDiv = $('div#' + onBehalfOfDivId);
                                    if (data.owner.uuid) {
                                        onBehalfOfDiv.text("Loading...");
                                        $.ajax({
                                            url: getProxiedUrl(sprintf("%s/users/%s", fedApiAddress, data.owner.uuid)),
                                            type: "GET",
                                            dataType: "json"
                                        })
                                            .done(function (data) {
                                                onBehalfOfDiv.html(
                                                    sprintf("<b>On behalf of</b>: %s (%s)", data.username, data.uuid));
                                            })
                                            .fail(function () {
                                                onBehalfOfDiv.text("Failed to retrieve user data.");
                                            })
                                    }
                                    else {
                                        onBehalfOfDiv.text("N/A");
                                    }

                                })
                                .fail(function () {
                                    accessTokenInfoDiv.html("<b>Token</b>: " + data.initiator.oauthAccessToken + "<br/>" +
                                        "Failed to retrieve token details.");
                                })
                        }
                        else {
                            accessTokenInfoDiv.text("N/A");
                        }

                    }, function (xhr, status, error) {
                        api.set('content.text', status + ': ' + error);
                    });

                return 'Loading...'
            }
        },
        show: {
            event: 'mouseover',
            solo: true
        },
        hide: 'unfocus',
        position: {
            //my: 'top left',
            //at: 'bottom right',
            effect: false,
            viewport: $(window),
            adjust: {
                method: 'shift'
            }
        }
    })
}

function registerInteractionEventTooltip(target, jobId) {
    target.qtip({
        content: {
            text: function (event, api) {

                $.ajax({
                    url: getProxiedUrl(sprintf("%s/audit_events/reports/%s/events/%s", auditManagerAddress, jobId, $(this).attr("id"))),
                    type: "GET",
                    dataType: "json"
                })
                    .then(function (data) {


                        var formatDate = d3.time.format("%Y-%m-%dT%H:%M:%S.%L%Z")
                        var time = formatDate(new Date(data.eventTime));
                        var getAttachmentByName = function (name) {
                            for (var i in data.attachments) {
                                if (data.attachments[i].name === name) {
                                    return data.attachments[i];
                                }
                            }
                        }
                        var httpRequestData = getAttachmentByName('http_request_data');
                        var httpResponseData = getAttachmentByName('http_response_data');

                        var accessTokenInfoDivId = api._id + '-atinfo';
                        var onBehalfOfDivId = api._id + '-onbehalfof';
                        var context = {
                            'id': data.id,
                            'time': time,
                            'action': data.action,
                            'outcome': data.outcome,
                            'initiatorId': data.initiator.id,
                            'onBehalfOfDivId': onBehalfOfDivId,
                            'accessTokenInfoDivId': accessTokenInfoDivId,
                            'targetId': data.target.id,
                            'requestMethod': (httpRequestData) ? httpRequestData.content.method : 'N/A',
                            'requestUrl': (httpRequestData) ? httpRequestData.content.url : 'N/A',
                            'requestContentType': (httpRequestData) ? httpRequestData.content.contentType : 'N/A',
                            'requestContent': (httpRequestData) ? shorten(httpRequestData.content.content, requestContentMaxSize) : 'N/A',
                            'responseStatusCode': (httpResponseData) ? httpResponseData.content.statusCode : 'N/A',
                            'responseContentType': (httpResponseData) ? httpResponseData.content.contentType : 'N/A',
                            'responseContent': (httpResponseData) ? shorten(httpResponseData.content.content, responseContentMaxSize) : 'N/A'
                        }
                        var html = templates["event-info-template"](context);

                        api.set('content.title', 'Event ' + data.id);
                        api.set('content.text', html);
                        //api.set('content.button', true);

                        // retrieve access token info from oauth-as
                        var accessTokenInfoDiv = $('div#' + accessTokenInfoDivId);
                        if (data.initiator.oauthAccessToken) {
                            accessTokenInfoDiv.text("Loading...");
                            $.ajax({
                                url: getProxiedUrl(sprintf("%s/access_tokens/%s", oauthASAddress, data.initiator.oauthAccessToken)),
                                type: "GET",
                                dataType: "json"
                            })
                                .done(function (data) {
                                    var clientLocation = '';
                                    if (data.client.countries.length > 0) {
                                        for (var i in data.client.countries) {
                                            var name = data.client.countries[i].name;
                                            if (i > 0) {
                                                clientLocation += ', ';
                                            }
                                            clientLocation += name;
                                        }
                                    }
                                    else {
                                        clientLocation = 'N/A';
                                    }

                                    var context = {
                                        'token': data.access_token,
                                        'expireTime': data.expire_time,
                                        'userUuid': data.owner.uuid,
                                        'clientName': data.client.name,
                                        'clientOrganization': data.client.organization.name,
                                        'clientLocation': clientLocation
                                    };

                                    var html = templates["access-token-info-template"](context);
                                    accessTokenInfoDiv.html(html);

                                    // retrieve user (resource owner) from the federation-api using user uuid obtained from the oauth-as
                                    var onBehalfOfDiv = $('div#' + onBehalfOfDivId);
                                    if (data.owner.uuid) {
                                        onBehalfOfDiv.text("Loading...");
                                        $.ajax({
                                            url: getProxiedUrl(sprintf("%s/users/%s", fedApiAddress, data.owner.uuid)),
                                            type: "GET",
                                            dataType: "json"
                                        })
                                            .done(function (data) {
                                                onBehalfOfDiv.html(
                                                    sprintf("<b>On behalf of</b>: %s (%s)", data.username, data.uuid));
                                            })
                                            .fail(function () {
                                                onBehalfOfDiv.text("Failed to retrieve user data.");
                                            })
                                    }
                                    else {
                                        onBehalfOfDiv.text("N/A");
                                    }

                                })
                                .fail(function () {
                                    accessTokenInfoDiv.html("<b>Token</b>: " + data.initiator.oauthAccessToken + "<br/>" +
                                        "Failed to retrieve token details.");
                                })
                        }
                        else {
                            accessTokenInfoDiv.text("N/A");
                        }

                    }, function (xhr, status, error) {
                        api.set('content.text', status + ': ' + error);
                    });

                return 'Loading...'
            }
        },
        show: {
            event: 'mouseover',
            solo: true
        },
        hide: 'unfocus',
        position: {
            target: 'mouse',
            effect: false,
            viewport: $(window),
            adjust: {
                method: 'shift',
                mouse: false
            }
        }
    })
}

function getProxiedUrl(url) {
    return '/user/jsonproxy/' + window.btoa(url).replace(/\+/g, '-').replace(/\//g, '_').replace(/\=/g, ',');
}

function shorten(str, n) {
    if (!str) {
        return "";
    }
    else if (str.length > n) {
        return str.substr(0, n) + "...";
    }
    else {
        return str;
    }
}