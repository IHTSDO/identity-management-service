import { BrowserModule } from '@angular/platform-browser';
import { NgModule, APP_INITIALIZER } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http'

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { MainComponent } from './components/main/main.component';
import { LoginComponent } from './components/login/login.component';

import { HttpConfigInterceptor } from './configs/httpconfig.interceptor';
import { PrincipleService } from './services/principle.service';
import { LogoutComponent } from './components/logout/logout.component';

export function startupServiceFactory(principle: PrincipleService): Function {
  return () => principle.identity(true);
}

@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    MainComponent,
    LoginComponent,
    LogoutComponent
    
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    AppRoutingModule
  ],
  providers: [ PrincipleService,
              {
                  provide: APP_INITIALIZER,
                  useFactory: startupServiceFactory,
                  deps: [PrincipleService],
                  multi: true
              } ,           
              {   provide: HTTP_INTERCEPTORS, 
                  useClass: HttpConfigInterceptor, 
                  multi: true
              }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
