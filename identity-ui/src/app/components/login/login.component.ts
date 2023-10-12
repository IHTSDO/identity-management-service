import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthenticationService } from '../../services/authentication/authentication.service';
import { PrincipleService } from 'src/app/services/principle/principle.service';
import {User} from "../../models/user";
import {ToastrService} from "ngx-toastr";

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

    user: User;

    year: number = new Date().getFullYear();

    constructor(private authenticationService: AuthenticationService,
                private principle: PrincipleService,
                private router: Router,
                private route: ActivatedRoute,
                private formBuild: FormBuilder,
                private toastr: ToastrService) {
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
                this.user = this.principle._identity;
            }
        } else {
            this.principle.loadAccountCompleted.subscribe(() => {
                if (this.principle.isAuthenticated()) {
                    this.router.navigate(['']);
                    this.user = this.principle._identity;
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
            login: this.messageForm.controls['username'].value,
            password: this.messageForm.controls['password'].value,
            rememberMe: this.messageForm.controls['rememberMe'].value
        }).then(() => {
            const returnUrl = this.route.snapshot.queryParamMap.get('serviceReferer');

            if (returnUrl) {
                window.location.href = returnUrl;
            } else {
                window.location.href = 'https://confluence.ihtsdotools.org/dashboard';
            }
        }, error => {
            if (error['status'] === 400 || error['status'] === 404) {
                this.toastr.error('Invalid username or password', 'Error');
            } else {
                this.toastr.error('Unauthorised', 'Error');
            }
        });
    }
}
