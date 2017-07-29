var setTimelapseID, setResolution, setInterval, setCaptured,
	setNextPhotoTime, setBattery, setWatchers, setProgress;
var clearUIData;
var requestImage;

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
	
	requestImage = function() { //#outputImage
		$.ajax({
			url: 'http://192.168.1.35:9090/getImage',
			success: function(r) {
				console.log("AJAX SUCCESS, data: " + r);
				
				var json = JSON.parse(r);
				
				$("#outputImage").attr("src", "data:image/png;base64,"
					+ json.image);
			}
		});
	}
	
	clearUIData();
});