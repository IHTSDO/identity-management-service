import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthenticationService } from '../../services/authentication.service';
import { PrincipleService } from 'src/app/services/principle.service';

@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

    authenticationError = false;
    forbiddenError = false;

    messageForm: FormGroup;
    formSubmit = false;

    constructor(private authenticationService: AuthenticationService,
                private principle: PrincipleService,
                private router: Router,
                private route: ActivatedRoute,
                private formBuild: FormBuilder) {
        this.messageForm = this.formBuild.group({
            username: ['', Validators.required],
            password: ['', Validators.required],
            rememberMe: [true]
        });
    }

    ngOnInit() {
        if (this.principle.isIdentityResolved()) {
            if (this.principle.isAuthenticated()) {
                this.router.navigate(['']);
            }
        } else {
            this.principle.loadAccountCompleted.subscribe(() => {
                if (this.principle.isAuthenticated()) {
                    this.router.navigate(['']);
                }
            });
        }
    }

    doLogin() {
        this.authenticationError = false;
        this.forbiddenError = false;
        this.formSubmit = true;

        if (this.messageForm.invalid) {
            return;
        }

        this.authenticationService.login({
            login: this.messageForm.controls.username.value,
            password: this.messageForm.controls.password.value,
            rememberMe: this.messageForm.controls.rememberMe.value
        }).then(() => {
            const returnUrl = this.route.snapshot.queryParamMap.get('serviceReferer');

            if (returnUrl) {
                window.location.href = returnUrl;
            } else {
                window.location.href = 'https://confluence.ihtsdotools.org/dashboard';
            }
        }, error => {
            if (error['status'] === 400 || error['status'] === 404) {
                this.authenticationError = true;
            } else {
                this.forbiddenError = true;
            }
        });
    }
}
