'use strict';

angular.module('imApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('docs', {
                parent: 'site',
                url: '/docs',
                data: {
                    roles: [], 
                    pageTitle: 'global.menu.admin.apidocs'
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/admin/docs/docs.html'
                    }
                }
            });
    });
