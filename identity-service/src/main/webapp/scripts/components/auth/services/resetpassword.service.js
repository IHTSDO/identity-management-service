'use strict';

angular.module('imApp')
    .factory('ResetPassword', function ($http) {        
        return {

            reset: function(req) {
                
            	var data = 'key=' + encodeURIComponent(req.key) +
                    '&password=' + encodeURIComponent(req.password);            
                return $http.post('api/reset_password', data, {
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    }
                }).success(function (response) {
                	
                    return response;
                });
            }
        }
    });


