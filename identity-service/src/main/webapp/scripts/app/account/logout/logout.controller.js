'use strict';

angular.module('imApp')
    .controller('LogoutController', function (Auth, $window) {
        Auth.logout();
    });
