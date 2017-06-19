local sensorSend = { }

for i, sensor in pairs(sensors) do
	gpio.mode(sensor.pin, gpio.INPUT, gpio.PULLUP)
	sensor.state = gpio.read(sensor.pin)
	table.insert(sensorSend, i)
end

tmr.create():alarm(100, tmr.ALARM_AUTO,function(t)
	for i, sensor in pairs(sensors) do
		if sensor.state ~= gpio.read(sensor.pin) then
			sensor.state = gpio.read(sensor.pin)
			table.insert(sensorSend, i)
		end		
	end
end)

if smartthings.token then
	tmr.create():alarm(250, tmr.ALARM_AUTO, function(t)
		if sensorSend[1] then
			t:stop()
			local sensor = sensors[sensorSend[1]]
			http.put(smartthings.apiUrl.."\/sensors\/"..sensor.pin.."\/"..gpio.read(sensor.pin), "Authorization: Bearer "..smartthings.token.."\r\n", "", function() 
				table.remove(sensorSend, 1)
				t:start()
			end)
		end
	end)
end