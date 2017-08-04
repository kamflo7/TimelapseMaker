var setTimelapseID, setResolution, setInterval, setCaptured,
	setNextPhotoTime, setBattery, setWatchers, setProgress,
	setDeviceName, setAPIVersion;
var clearUIData;
var requestData;

var getBaseHref;
var port = 9090;

var requestTryCount = 0;
var unixTimeWhenGotPhoto;
var lastTimeToNextCaptureMs;

var debugMsgs = false;

$(document).ready(function() {
	setTimelapseID 	=	function(id="Unknow") 		{	$("#timelapseid").text(id);	}
	setResolution	=	function(resolution="-"){	$("#resolution").text(resolution);	}
	setInterval		=	function(interval="-")	{	$("#interval").text(interval);	}
	setCaptured		=	function(captured="-", maxPhotos)	{
		$("#captured").text(captured=="-" ? "-" : (captured + " of " + maxPhotos));
	}
	setNextPhotoTime=	function(nextPhoto="-")	{	$("#nextPhotoIn").text(nextPhoto);	}
	setBattery		=	function(battery="-")	{	$("#battery").text(battery);	}
	setWatchers		=	function(watchers="-")	{	$("#watchers").text(watchers);	}
	setProgress		=	function(progress) {
		$("#timelapseProgress").css("width", progress+"%");
		$("#timelapseProgress").text(Number(progress).toFixed(0)+"%");
		$("#timelapseProgress").attr('aria-valuenow', progress);
	}
	setDeviceName 	= 	function(name="-") 			{	$("#deviceName").text(name);}
	setAPIVersion 	= 	function(api="-") 			{	$("#cameraApi").text(api);}

	clearUIData = function() {
		setTimelapseID();
		setResolution();
		setInterval();
		setCaptured();
		setNextPhotoTime();
		setBattery();
		setWatchers();
		setProgress(0);
	}

	getBaseHref = function() {
		return "http://" + window.location.hostname + ":"+port+"/";
	}
	
	requestData = function() {
		$.ajax({
			url: getBaseHref()+'getData',
			success: function(r) {
				//console.log("[AJAX: /getData/], result: " + JSON.stringify(r));
				$("#warningAlert").css("display", "none");
				requestTryCount = 0;
				
				var json = JSON.parse(r);
				
				setResolution(json.resolution);
				setInterval((json.intervalMiliseconds / 1000) + "s");
				setCaptured(json.capturedPhotos, json.maxPhotos);
				setProgress((json.capturedPhotos / json.maxPhotos) * 100);
				setBattery(parseFloat(json.battery_level) + "%");
				setTimelapseID(json.timelapseID);
				setDeviceName(json.device_name + " (" + json.android_version + ")");
				setAPIVersion("v"+json.camera_api);
				setWatchers(json.watchers);
				port = json.app_port;
				
				$("#outputImage").attr("src", "data:image/png;base64,"
					+ json.image);
				
				
				unixTimeWhenGotPhoto = new Date().getTime();
				lastTimeToNextCaptureMs = parseInt(json.timeMsToNextCapture);
				//console.log("Obtained JSON.timeMsToNextCapture:" + lastTimeToNextCaptureMs);
				var timeToNextCall = lastTimeToNextCaptureMs > 0 ? (lastTimeToNextCaptureMs + 500) : json.intervalMiliseconds;
				
				
				//if(json.timelapseStatus == "doing")
				setTimeout(function() { requestData(); }, timeToNextCall);
			},
			error: function(xhr, ajaxOptions, thrownError) {
				requestTryCount++;
				setTimeout(function() { requestData(); }, 5000);
				
				$("#warningAlert").css("display", "block");
				$("#warningText").text("Request does not respond! We will try again in few seconds." + (requestTryCount >= 2 ? (" ["+requestTryCount+"]") : ""));
			}
		});
	}
	
	clearUIData();
	setTimeout(function() {
		requestData();
	}, 700);
	
		
	var refreshTimeToNextPhoto = function() {
		var diff = lastTimeToNextCaptureMs - (new Date().getTime() - unixTimeWhenGotPhoto);
		
		if(debugMsgs)
			console.log("lastTimeToNextCaptureMs: " + lastTimeToNextCaptureMs + "; unixTimeWhenGotPhoto: " + unixTimeWhenGotPhoto + "; NOW: " + new Date().getTime() + "; DIFF: " + diff);
		if(diff < 0) diff = 0;
		
		setNextPhotoTime(parseInt(diff/1000) + "s");
		setTimeout(refreshTimeToNextPhoto, 500);
	}
	
	setTimeout(refreshTimeToNextPhoto, 500);
});