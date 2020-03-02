(function() {
    $(function() {
        var ws;
        var data = []
        ws = new WebSocket($("body").data("ws-url"));
        window.onload = function() {
            var request = new XMLHttpRequest()

            request.open('GET', 'http://localhost:9000/stocks', true)

            request.send()

            request.onload = function() {
                // Begin accessing JSON data here
                data = JSON.parse(this.response)

                var i
                for (i = 0; i < data.length; i++) {
                    ws.send(JSON.stringify({
                        symbol: data[i].symbol
                    }));
                }
            }
        };
        ws.onmessage = function(event) {
            var message;
            message = JSON.parse(event.data);
            switch (message.type) {
                case "stockhistory":
                    return populateStockHistory(message);
                case "stockupdate":
                    updateStockValue(message);
                    return updateStockChart(message);
                default:
                    return console.log(message);
            }
        };
    });

    getPricesFromArray = function(data) {
        var v, _i, _len, _results;
        _results = [];
        for (_i = 0, _len = data.length; _i < _len; _i++) {
            v = data[_i];
            _results.push(v[1]);
        }
        return _results;
    };

    getChartArray = function(data) {
        var i, v, _i, _len, _results;
        _results = [];
        for (i = _i = 0, _len = data.length; _i < _len; i = ++_i) {
            v = data[i];
            _results.push([i, v]);
        }
        return _results;
    };

    getChartOptions = function(data) {
        return {
            series: {
                shadowSize: 0
            },
            yaxis: {
                min: getAxisMin(data),
                max: getAxisMax(data)
            },
            xaxis: {
                show: false
            }
        };
    };

    getAxisMin = function(data) {
        return Math.min.apply(Math, data) * 0.9;
    };

    getAxisMax = function(data) {
        return Math.max.apply(Math, data) * 1.1;
    };

    populateStockHistory = function(message) {
        var chart = $("<div>")
            .addClass("chart")
            .prop("id", message.symbol)

        var chartHolder = $("<div>")
            .addClass("chart-holder")
            .append(chart)

        chartHolder
            .append($("<p>").addClass("stockValue")
                .attr(message.symbol + "value", message.symbol)
                .text(message.history[message.history.length - 1]))

        var stockContainer = $("<div>")
            .addClass("stockContainer")
            .append(chartHolder)
            .attr("data-content", message.name + " -- " + message.symbol)

        $("#stocks")
            .prepend(stockContainer)

        var a = [getChartArray(message.history)]
        var b = getChartOptions(message.history)

        var plot = chart
            .plot(a, b)
            .data("plot")

        return plot
    };

    updateStockValue = function(message) {
        var attributeLookupKey = message.symbol + "value"

        var plot = $("#" + message.symbol).data("plot");
        var data = getPricesFromArray(plot.getData()[0].data);
        var value = data[data.length - 1]

        $("p[" + attributeLookupKey + "*='" + message.symbol + "']").text(value);
    }

    updateStockChart = function(message) {
        var lookupQuery = "#" + message.symbol

        if ($(lookupQuery).size() > 0) {
            var plot = $(lookupQuery).data("plot");
            var data = getPricesFromArray(plot.getData()[0].data);
            data.shift();
            data.push(message.price);
            plot.setData([getChartArray(data)]);
            var yaxes = plot.getOptions().yaxes[0];
            if ((getAxisMin(data) < yaxes.min) || (getAxisMax(data) > yaxes.max)) {
                yaxes.min = getAxisMin(data);
                yaxes.max = getAxisMax(data);
                plot.setupGrid();
            }
            return plot.draw();
        }
    };

}).call(this);