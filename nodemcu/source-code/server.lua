require("httpd")
require("ssdp")
httpd_set("/", function(request, response) 
    response:file("http_index.html")
end)
httpd_set("/favicon.ico", function(request, response) 
    response:contentType("image/x-icon")
    response:file("http_favicon.ico")
end)
httpd_set("/SyncSettings", function(request, response) 
    if request.method == "POST" and request.contentType == "application/json" then
		request.body = cjson.decode(request.body)
		local sensors = "{ "
		for i,sensor in pairs(request.body.sensors) do
			if i == #request.body.sensors then 
				sensors = sensors.."\r\n{ pin = "..sensor.pin.." }"
			else
				sensors = sensors.."\r\n{ pin = "..sensor.pin.." },"
			end
		end
		sensors = sensors.." }"
		variables_set("smartthings", "{ token = \"" .. request.body.token .. "\", apiUrl = \""..request.body.apiUrl.."\" }")
		variables_set("sensors", sensors)
		sensors = nil		
    end            
    response:contentType("application/json")
	response:status("204")
    response:send("")
end)
httpd_set("/device", function(request, response) 
    if request.method == "PUT" and request.contentType == "application/json" then
		request.body = cjson.decode(request.body)
		gpio.mode(request.body.pin, gpio.OUTPUT)
		gpio.write(request.body.pin, request.body.state)
    end            
    response:contentType("application/json")
	response:status("204")
    response:send("")
end)