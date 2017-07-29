var setTimelapseID, setResolution, setInterval, setCaptured,
	setNextPhotoTime, setBattery, setWatchers, setProgress;
var clearUIData;
//var requestImage;
var requestData;

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
		$("#timelapseProgress").text(progress+"%");
		$("#timelapseProgress").attr('aria-valuenow', progress);
	}

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
	
	/*requestImage = function() { //#outputImage
		$.ajax({
			url: 'http://192.168.1.35:9090/getImage',
			success: function(r) {
				console.log("AJAX SUCCESS, data: " + r);
				
				var json = JSON.parse(r);
				
				$("#outputImage").attr("src", "data:image/png;base64,"
					+ json.image);
			}
		});
	}*/
	
	requestData = function() {
		$.ajax({
			url: 'http://192.168.1.35:9090/getData',
			success: function(r) {
				console.log("[AJAX: /getData/], result: " + JSON.stringify(r));
				
				var json = JSON.parse(r);
				
				
				setResolution(json.resolution);
				setInterval(json.intervalMiliseconds / 1000);
				setCaptured(json.capturedPhotos, json.maxPhotos);
				setProgress((json.capturedPhotos / json.maxPhotos) * 100);
				
				$("#outputImage").attr("src", "data:image/png;base64,"
					+ json.image);
				
				//setTimelapseID
				//setBattery
				//setWatchers
				//call setTimeout with received 'timeMsToNextCapture'
				//generally do another setTimeout to refresh time to next capture, not here
			}
		});
	}
	
	clearUIData();
});