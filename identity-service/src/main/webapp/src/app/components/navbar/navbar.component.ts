import { Component, OnInit } from '@angular/core';
import { PrincipleService } from '../../services/principle.service'
import { AuthenticationService } from 'src/app/services/authentication.service';
import { Router  } from '@angular/router';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit {
  
  isAuthenticated : Boolean = undefined
  isAdmin : Boolean = undefined

  constructor(private principle : PrincipleService, 
              private auth : AuthenticationService,
              private router: Router) { }

  ngOnInit() {
    if (this.principle.isIdentityResolved()) {
      this.isAuthenticated = this.principle.isAuthenticated()
      this.isAdmin = this.principle.isInRole('ROLE_ihtsdo-ops-admin') || this.principle.isInRole('ROLE_ims-administrators')
    }
    
    this.principle.loadAccountCompleted.subscribe( () => {
      this.isAuthenticated = this.principle.isAuthenticated()
      this.isAdmin = this.principle.isInRole('ROLE_ihtsdo-ops-admin') || this.principle.isInRole('ROLE_ims-administrators') 
    })    
  }

  logout () {
    this.auth.logout().then(()=> {
      this.router.navigate(['login'])
    })
    
  }
}
