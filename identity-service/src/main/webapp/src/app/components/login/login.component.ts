import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute  } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms'

import { AuthenticationService } from '../../services/authentication.service';
import { PrincipleService } from 'src/app/services/principle.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  
  authenticationError : boolean = false
  forbiddenError : boolean = false

  messsageForm : FormGroup
  formSubmit = false

  constructor(private authenticationService : AuthenticationService, 
              private principle : PrincipleService,
              private router : Router,
              private route: ActivatedRoute,
              private formBuild : FormBuilder) { 
    this.messsageForm = this.formBuild.group({
      username : ['', Validators.required],
      password : ['', Validators.required],
      rememberMe :[true]
    })
  }

  ngOnInit() {
    if (this.principle.isIdentityResolved()) {
      if (this.principle.isAuthenticated()) {
        this.router.navigate([''])
      }
    } else {
      this.principle.loadAccountCompleted.subscribe( () => {
        if (this.principle.isAuthenticated()) {
          this.router.navigate([''])
        }      
      })
    }
  }

  doLogin () {
    this.authenticationError = false
    this.forbiddenError = false
    this.formSubmit = true
    
    if (this.messsageForm.invalid) {
      return
    }

    this.authenticationService.login({
      login: this.messsageForm.controls.username.value,
      password: this.messsageForm.controls.password.value,
      rememberMe: this.messsageForm.controls.rememberMe.value
    }).then(()=> {
      var returnUrl = this.route.snapshot.queryParamMap.get('serviceReferer')

      if (returnUrl) {
        window.location.href = returnUrl
      } else {
        window.location.href = 'https://confluence.ihtsdotools.org/dashboard'
      }   
    }, error => {
      if (error['status'] === 400 || error['status'] === 404) {
        this.authenticationError = true
      } else {
        this.forbiddenError = true
      }      
    });
  }
}
