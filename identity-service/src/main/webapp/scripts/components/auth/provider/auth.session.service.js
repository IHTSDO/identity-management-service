'use strict';

angular.module('imApp')
    .factory('AuthServerProvider', function loginService($http, $location, localStorageService, $cookies, $window, $timeout) {
        return {
            login: function(credentials) {
            	
            	var sReferer = $location.search().serviceReferer;
            	var isSRefererExist = angular.isUndefined(sReferer) || sReferer === null;
                
            	var data = 'j_username=' + encodeURIComponent(credentials.username) +
                    '&j_password=' + encodeURIComponent(credentials.password) +
                    '&_spring_security_remember_me=' + credentials.rememberMe + '&submit=Login';
                
                if (!(isSRefererExist)) {
					
                	data = data + '&serviceReferer=' +  encodeURIComponent(sReferer);
				}

                return $http.post('/j_security_check', data, {
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    }
                }).success(function (response) {
                	//need to change
                    localStorageService.set('token', $window.btoa(credentials.username + ':' + credentials.password));
                    return response;
                });
            },
            logout: function() {
                // logout from the server
                $http.post('/j_spring_security_logout').success(function (response) {
                    var returnUrl = $location.search().serviceReferer;
                    localStorageService.clearAll();
                    $window.localStorage.clear();
                    delete $cookies['CSRF-TOKEN'];
                    // to get a new csrf token reload login
                    if(!angular.isUndefined(returnUrl)){
                        $window.location.href = '?serviceReferer=' + returnUrl;
                    }
                    else{
                        $window.location.reload();
                    }
                });
            },
            getToken: function () {
                var token = localStorageService.get('token');
                return token;
            },
            hasValidToken: function () {
                var token = this.getToken();
                return !!token;
            }
        };
    });
