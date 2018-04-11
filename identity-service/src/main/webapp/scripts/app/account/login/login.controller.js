'use strict';

angular.module('imApp')
    .controller('LoginController', function ($rootScope, $scope, $state, $timeout, $window, $location,$cookies, Auth) {
        $scope.user = {};
        $scope.errors = {};

        $scope.rememberMe = true;
        $timeout(function (){angular.element('[ng-model="username"]').focus();});
        $scope.login = function () {
        	var csrfR = 'CSRF-TOKEN';
        	var csrfH= 'X-CSRF-TOKEN';

        	if (!angular.isUndefined($cookies.csrfR)) {
				
            	$cookies.remove(csrfR);

			}
        	
        	if (!angular.isUndefined($cookies.csrfH)) {
				
            	$cookies.remove(csrfH);

			}

            Auth.login({
                username: $scope.username,
                password: $scope.password,
                rememberMe: $scope.rememberMe
            }).then(function () {
                $scope.authenticationError = false;
                var returnUrl = $location.search().serviceReferer;
                if ($rootScope.previousStateName === 'register') {
                    $state.go('home');
                } else if(!angular.isUndefined(returnUrl)){
                    
                	$window.location.href = returnUrl;
                	
                } else {
                    $window.location.href = 'https://confluence.ihtsdotools.org/dashboard';
                }
            }).catch(function (err) {

            	if (err.status === 403) {
					
            		$scope.forbiddenError = true;
            		
				} else {
					
	                $scope.authenticationError = true;

				}
            });
        };
    });
