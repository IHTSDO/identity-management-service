'use strict';

angular.module('imApp')
    .factory('ForgotPassword', function ($resource) {
        return $resource('api/forgot_password', {}, {
        });
    });


