'use strict';

angular.module('imApp')
    .factory('Register', function ($resource) {
        return $resource('api/register', {}, {
        });
    });


