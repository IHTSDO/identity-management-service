# IdentityUi

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 10.1.3.

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `npm test` to execute the unit tests via Jest.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).

## Nginx Support

To get this project running locally using nginx, the following config settings can be used, changing ports where required:

    server {
        listen      8080;
        server_name localhost.ihtsdotools.org;

        location / {
            proxy_pass http://127.0.0.1:4200;
        }

        location /api {
            proxy_pass https://dev-ims.ihtsdotools.org/api;
            proxy_set_header Accept "application/json";
        }
    }
