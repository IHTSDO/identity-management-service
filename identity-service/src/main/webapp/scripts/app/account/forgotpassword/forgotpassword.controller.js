'use strict';

angular.module('imApp')
    .controller('ForgotPasswordController', function ($scope, $translate, $timeout, Auth) {
        $scope.success = null;
        $scope.error = null;
        $timeout(function (){angular.element('[ng-model="userName"]').focus();});

        $scope.forgotpassword = function () {
            $scope.error = null;

            Auth.forgotPassword($scope.userName).then(function () {
                $scope.success = 'OK';
            }).catch(function (response) {
                $scope.success = null;
                if (response.status === 400 && response.data === 'login already in use') {
                    $scope.errorUserExists = 'ERROR';
                } else if (response.status === 400 && response.data === 'e-mail address already in use') {
                    $scope.errorEmailExists = 'ERROR';
                } else {
                    $scope.error = 'ERROR';
                }
            });
        
        };
    });
