function LocatorController($scope, $http) {

/*	Asks Geolocation API for latitude and longitude,
	if not available, defaults to 45 45
*/
	$scope.get_location = function() {
	  if (Modernizr.geolocation) {
	  	$scope.coordinates = "Reading your current latitude and longitude";

	    navigator.geolocation.getCurrentPosition(
	    	function(position) {
	    	  var latitude = position.coords.latitude;
	    	  var longitude = position.coords.longitude;
	    	  $scope.coordinates = latitude + "," + longitude;
	    	  $scope.$digest();
	    	});
	  } else {
	  	$scope.coordinates = "45,45";
	  	console.log("No support for Geolocation API");
	  }
	};

/*	Input validation functions
*/	
	$scope.valid_lat_lon = function() {
		return /^-?\d+(\.\d+)?,\s*-?\d+(\.\d+)?$/.test($scope.coordinates);
	}

	$scope.valid_ip = function() {
		return /^\d{1,3}(\.\d{1,3}){3}$/.test($scope.ip);
	}

	$scope.get_location_name = function(){
		alert($scope.valid_lat_lon());
		alert($scope.valid_ip());
	}

	$scope.get_location_name = function() {
		if (! ($scope.valid_lat_lon() || $scope.valid_ip())) {
			alert("Neither the coordinates nor the IP address seems right.\nGive it another shot.");
		}

    	var postData = {coordinates: $scope.coordinates, ip: $scope.ip};
    	
    	$http.post('query', postData).success( function(data, status, headers, config) {
    		console.log(status + data);
    	}).error(function(data, status, headers, config) {
    		console.log(data + status);
    	});
    }

	$scope.ip = "127.0.0.1";
	$scope.get_location();
}