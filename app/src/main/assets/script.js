var setTimelapseID, setResolution, setInterval, setCaptured,
	setNextPhotoTime, setBattery, setWatchers, setProgress;
var clearUIData;

$(document).ready(function() {
	setTimelapseID 	=	function(id="Unknow") 		{	$("#timelapseid").text(id);	}
	setResolution	=	function(resolution="-"){	$("#resolution").text(resolution);	}
	setInterval		=	function(interval="-")	{	$("#interval").text(interval);	}
	setCaptured		=	function(captured="-")	{	$("#captured").text(captured);	}
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
	
	clearUIData();
});