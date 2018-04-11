'use strict';

angular.module('imApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('forgotpassword', {
                parent: 'account',
                url: '/forgotpassword',
                data: {
                    roles: [],
                    pageTitle: 'forgotpassword.title'
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/account/forgotpassword/forgotpassword.html',
                        controller: 'ForgotPasswordController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('forgotpassword');
                        return $translate.refresh();
                    }]
                }
            });
    });
