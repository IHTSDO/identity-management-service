'use strict';

angular.module('imApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('resetPassword', {
                parent: 'site',
                url: '/resetPassword?key',
                data: {
                    pageTitle: 'global.menu.account.password.reset'
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/account/password/password.html',
                        controller: 'ResetPasswordController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('password');
                        return $translate.refresh();
                    }]
                }
            });
    });

