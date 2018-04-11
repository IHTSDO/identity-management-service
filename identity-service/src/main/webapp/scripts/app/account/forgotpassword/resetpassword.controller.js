'use strict';

angular.module('imApp')
    .controller('ResetPasswordController', function ($rootScope, $scope, $location, Auth) {

    	$scope.reset = true;
        $scope.success = null;
        $scope.error = null;
        $scope.doNotMatch = null;
        $scope.invalidKey = null;

        $scope.changePassword = function () {
            if ($scope.password !== $scope.confirmPassword) {
                $scope.doNotMatch = 'ERROR';
            } else if (angular.isUndefined($location.search().key) || $location.search().key === null) {
                $scope.invalidKey = 'ERROR';

            } else {
                $scope.doNotMatch = null;
                $scope.invalidKey = null;
                
                Auth.resetPassword({
                    key: $location.search().key,
                    password: $scope.password
                 }).then(function (response) {

                     $scope.error = null;
                     $scope.success = 'OK';

                     /*if ($rootScope.previousStateName === 'resetPassword') {
                        $state.go('back');
                    } else {
                        $rootScope.back();
                    }*/
                }).catch(function () {
                    $scope.success = null;
                    //$scope.error = 'ERROR';
                    $scope.invalidKey = 'ERROR';

                });
            }
        };
    });
